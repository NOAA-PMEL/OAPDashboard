/**
 * 
 */
package gov.noaa.pmel.dashboard.oads;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import gov.noaa.ncei.oads.xml.v_a0_2_2.BaseVariableType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.Co2Autonomous;
import gov.noaa.ncei.oads.xml.v_a0_2_2.Co2Discrete;
import gov.noaa.ncei.oads.xml.v_a0_2_2.DicVariableType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.OadsMetadataDocumentType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonContactInfoType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonNameType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PhVariableType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PlatformType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.SpatialExtentsType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.TaVariableType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.TemporalExtentsType;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FileInfo;
import gov.noaa.pmel.dashboard.shared.MetadataPreviewInfo;
import gov.noaa.pmel.dashboard.shared.NotFoundException;
import gov.noaa.pmel.dashboard.util.Anglican;
import gov.noaa.pmel.dashboard.util.xml.XReader;
import gov.noaa.pmel.dashboard.util.xml.XmlUtils;
import gov.noaa.pmel.oads.util.StringUtils;
import gov.noaa.pmel.oads.xml.a0_2_2.OadsXmlReader;
import gov.noaa.pmel.oads.xml.a0_2_2.OadsXmlWriter;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;

/**
 * @author kamb
 *
 */
public class OADSMetadata {

	private static Logger logger = LogManager.getLogger(OADSMetadata.class);
	
	private static class Extents {
		double maxValue;
		double minValue;
		Extents(double max, double min) {
			maxValue = max;
			minValue = min;
		}
	}
	private static class GeoTemporalExtents {
		Extents latExtents;
		Extents lonExtents;
		Extents timeExtents;
		GeoTemporalExtents(Extents lats, Extents lons, Extents times) {
			latExtents = lats;
			lonExtents = lons;
			timeExtents = times;
		}
	}
	static GeoTemporalExtents extractGeospatialTemporalExtents(StdUserDataArray stdArray) {
		double maxLat = Double.NEGATIVE_INFINITY;
		double minLat = Double.POSITIVE_INFINITY;
		double maxLon = Double.NEGATIVE_INFINITY;
		double minLon = Double.POSITIVE_INFINITY;
		double maxTime = Double.NEGATIVE_INFINITY;
		double minTime = Double.POSITIVE_INFINITY;
		Double[] lats = stdArray.getSampleLatitudes();
		Double[] lons = stdArray.getSampleLongitudes();
		Double[] times = stdArray.getSampleTimes();
		for (int i=0; i<stdArray.getNumSamples(); i++) {
			double lat = DashboardServerUtils.doubleValue(lats[i], Double.NaN); 
			double lon = DashboardServerUtils.doubleValue(lons[i], Double.NaN);
			double time = DashboardServerUtils.doubleValue(times[i], Double.NaN);
			if ( ! Double.isNaN(lat) && lat > maxLat ) { maxLat = lat; }
			if ( ! Double.isNaN(lat) && lat < minLat ) { minLat = lat; }
			if ( ! Double.isNaN(lon) && lon > maxLon ) { maxLon = lon; }
			if ( ! Double.isNaN(lon) && lon < minLon ) { minLon = lon; }
			if ( ! Double.isNaN(time) && time > maxTime ) { maxTime = time; }
			if ( ! Double.isNaN(time) && time < minTime ) { minTime = time; }
		}
		GeoTemporalExtents extents = new GeoTemporalExtents(new Extents(maxLat, minLat), 
		                                                    new Extents(maxLon, minLon), 
		                                                    new Extents(maxTime, minTime));
		return extents;
	}
	
	private static String[] requiredVars = new String[] {
			"inorganic_carbon",
			"total_alkalinity",
			"ph_total",
			"pCO2/fCO2 (autonomous)",
			"pCO2/fCO2 (discrete)"
	};
	private static String[] requiredVarNames = new String[] {
			"Dissolved Inorganic Carbon",
			"Total Alkalinity",
			"pH",
			"pCO2A",
			"pCO2D"
	};
	
    public static OadsMetadataDocumentType readNewOadsXml(String recordId) throws Exception {
        File metadataFile = DashboardConfigStore.get(false).getMetadataFileHandler().getMetadataFile(recordId);
        if ( ! metadataFile.exists() || metadataFile.length() == 0 ) {
            return new OadsMetadataDocumentType();
        }
        OadsMetadataDocumentType mdDoc;
//        try {
            mdDoc = OadsXmlReader.read(metadataFile);
            return mdDoc;
//        } catch (Exception ex) {
//            logger.warn("Exception parsing metadata file " + metadataFile.getPath() + " : " + ex, ex);
//        }
    }
    
    public static void writeNewOadsXml(File metadataFile, OadsMetadataDocumentType mdDoc) throws Exception {
        OadsXmlWriter.writeXml(mdDoc, metadataFile);
    }
    
	public static OadsMetadataDocumentType extractOADSMetadata(StdUserDataArray stdArray,
	                                                        File metadataFile)
	    throws Exception
	{
        OadsMetadataDocumentType mdDoc = OadsXmlReader.read(metadataFile);
//        OadsMetadataDocumentTypeBuilder mdBuilder = mdDoc.toBuilder();
//		DashboardOADSMetadata oads = new DashboardOADSMetadata(stdArray.getDatasetName());
		GeoTemporalExtents gtExtents = extractGeospatialTemporalExtents(stdArray);
        mdDoc.setSpatialExtents(SpatialExtentsType.builder()
                                .westernBounds(new BigDecimal(gtExtents.lonExtents.minValue).setScale(3, RoundingMode.HALF_UP))
                                .easternBounds(new BigDecimal(gtExtents.lonExtents.maxValue).setScale(3, RoundingMode.HALF_UP))
                                .northernBounds(new BigDecimal(gtExtents.latExtents.maxValue).setScale(3, RoundingMode.HALF_UP))
                                .southernBounds(new BigDecimal(gtExtents.latExtents.minValue).setScale(3, RoundingMode.HALF_UP))
                                .build());
		mdDoc.setTemporalExtents(TemporalExtentsType.builder()
		                         .startDate(new Date((long)(1000*gtExtents.timeExtents.minValue)))
		                         .endDate(new Date((long)(1000*gtExtents.timeExtents.maxValue)))
		                         .build());
		                                    
        if ( ApplicationConfiguration.getProperty("oap.metadata.extract_variables", false) && mdDoc.getVariables().isEmpty()) {
            
            // XXX TODO: This is just a mess.
            // XXX What to do with unknowns and other/ignored, 
            // XXX plus putting data variables first and others last
            
            Map<String, BaseVariableType> existgVarsByColumnName = new HashMap<String, BaseVariableType>();
            Map<String, BaseVariableType> existgVarsByStandardName = new HashMap<String, BaseVariableType>();
            for (BaseVariableType var : mdDoc.getVariables()) {
                existgVarsByColumnName.put(var.getDatasetVarName(), var);
                existgVarsByStandardName.put(var.getName(), var);
            }
                    
//            Set<String> existgVars = checkExistgVars(mdDoc, stdArray);
    		Set<String> addVars = new HashSet<>();
    		for (String varname : requiredVars ) {
                 if ( ! stdArray.hasDataColumn(varname)) {
                    logger.info("Dataset does not have data column defined for " + varname);
                    if ( existgVarsByStandardName.containsKey(varname)) {
                        logger.warn("Metadata contains entry for " + varname + " but dataset does not have data column defined.");
                        // mdDoc.getVariables().remove(existgVarsByStandardName.get(varname));
                    }
                    continue;
                }
    			try {
    				String addName = addVariable(varname, mdDoc, stdArray);
    				addVars.add(addName);
    			} catch (NoSuchFieldException e) {
                    logger.warn(e);
    			}
    		}
    		for (int i = 0; i < stdArray.getNumDataCols(); i++ ) {
    			DashDataType<?> colType = stdArray.getDataTypes().get(i);
//                if ( ! ( colType instanceof DoubleDashDataType )) {
//                    continue;
//                }
    			String userColName = stdArray.getUserColumnTypeName(i);
    			String uunits = stdArray.getUserColumnUnits(i) != null ?
                                stdArray.getUserColumnUnits(i) :
                                "";
    			String stdName = colType.getStandardName();
    			String fullName = colType.getDisplayName();
                if ( colType.getVarName().equals(DashboardUtils.UNKNOWN_VARNAME) ||
                     colType.getVarName().equals(DashboardUtils.OTHER_VARNAME)) {
                    stdName = colType.getVarName();
                    fullName = colType.getDescription();
                }
    			if ( ! ( exclude(userColName, colType) ||
//                         existgVars.contains(stdName) || 
    				     addVars.contains(userColName))) {
//                    addVariable(stdName, mdDoc, stdArray);
                    addVariable(stdName, userColName, fullName, uunits, mdDoc, stdArray);
    //				oads.addVariable(new Variable().abbreviation(username).unit(uunits).fullName(fullName));
    			}
    		}
        }
		return mdDoc;
	}

/**
     * @param mdDoc
     * @return
     */
    private static Set<String> checkExistgVars(OadsMetadataDocumentType mdDoc, StdUserDataArray stdArray) {
        Set<String> existg = new TreeSet<>();
        Set<BaseVariableType> removed = new TreeSet<>();
        for ( BaseVariableType v : mdDoc.getVariables()) {
            if ( stdArray.findDataColumn(v.getDatasetVarName()) == null ) {
                removed.add(v);
            } else {
                existg.add(v.getName());
            }
        }
        for ( BaseVariableType v : removed ) {
            mdDoc.getVariables().remove(v);
        }
        return existg;
    }

//	public static DashboardOADSMetadata OLD_extractOADSMetadata(StdUserDataArray stdArray) {
//		DashboardOADSMetadata oads = new DashboardOADSMetadata(stdArray.getDatasetName());
//		GeoTemporalExtents gtExtents = extractGeospatialTemporalExtents(stdArray);
//		oads.westernBound(gtExtents.lonExtents.minValue);
//		oads.easternBound(gtExtents.lonExtents.maxValue);
//		oads.northernBound(gtExtents.latExtents.maxValue);
//		oads.southernBound(gtExtents.latExtents.minValue);
//		oads.startDate(new Date((long)(1000*gtExtents.timeExtents.minValue)));
//		oads.endDate(new Date((long)(1000*gtExtents.timeExtents.maxValue)));
//		Set<String> addVars = new HashSet<>();
//		for (String varname : requiredVars ) {
//			try {
//				String addName = addVariable(varname, oads, stdArray);
//				addVars.add(addName);
//			} catch (NoSuchFieldException e) {
//				System.err.println(e);
//				logger.info("No data column found for variable " + varname);
//			}
//		}
//		for (int i = 0; i < stdArray.getNumDataCols(); i++ ) {
//			String username = stdArray.getUserColumnTypeName(i);
//			String uunits = stdArray.getUserColumnUnits(i);
//			DashDataType<?> colType = stdArray.getDataTypes().get(i);
//			String stdName = colType.getStandardName();
//			String fullName = DashboardUtils.isEmptyOrNull(stdName) ? username : stdName;
//			if ( ! exclude(username, colType) &&
//				 ! addVars.contains(username)) {
//				oads.addVariable(new Variable().abbreviation(username).unit(uunits).fullName(fullName));
//			}
//		}
//		return oads;
//	}

	private static boolean exclude(String username, DashDataType<?> colType) {
        return "sample num".equals(username) || "WOCE autocheck".equals(username);
	}

	private static String addVariable(String varname, OadsMetadataDocumentType oads, StdUserDataArray stdArray) 
			throws NoSuchFieldException {
		String fullname;
		String abbrev;
		String units;
		int colIdx = stdArray.findDataColumnIndex(varname);
		if ( colIdx >= 0 ) {
            DashDataType<?> dataCol = stdArray.getDataTypes().get(colIdx);
			fullname = DashboardUtils.isEmptyOrNull(dataCol.getStandardName()) ?
												  dataCol.getDescription() : 
												  dataCol.getStandardName();
    		abbrev = stdArray.getUserColumnTypeName(colIdx);
    		units = stdArray.getUserColumnUnits(colIdx);
    		addVariable(varname, abbrev, fullname, units, oads, stdArray);
		} else {
            throw new NoSuchFieldException(stdArray.getDatasetName() + ": Did not find variable for " + varname);
		}
        return abbrev;
	}
        
/**
     * @param userColName
     * @param stdName
     * @param uunits
     * @param mdDoc
     * @param stdArray
     */
    private static void addVariable(String stdName, String userColName, String fullName, String uunits, 
                                    OadsMetadataDocumentType mdDoc, StdUserDataArray stdArray) {
        BaseVariableType v = getVariableFor(stdName);
        
        v.setName(stdName);
        v.setFullName(fullName);
        v.setDatasetVarName(userColName);
        v.setUnits(uunits);
        mdDoc.getVariables().add(v);
	}

    private static BaseVariableType getVariableFor(String varname) {
        switch (varname.toLowerCase()) {
            case "inorganic_carbon":
            case "dissolved_inorganic_carbon":
            case "dissolved inorganic carbon":
                return new DicVariableType();
            case "alkalinity":
            case "total_alkalinity":
            case "total alkalinity":
                return new TaVariableType();
            case "ph_total":
            case "ph":
                return new PhVariableType();
            case "pco2/fco2 (autonomous)":
            case "pco2(fco2)_autonomous":
            case "pco2a":
                return new Co2Autonomous();
            case "pco2/fco2 (discrete)":
            case "pco2(fco2)_discrete":
            case "pco2d":
                return new Co2Discrete();
            default:
                return new BaseVariableType();
        }
    }
	private static String OLD_addVariable(String varname, DashboardOADSMetadata oads, StdUserDataArray stdArray) 
			throws NoSuchFieldException {
		Variable v = new Variable();
		
		String fullname;
		String abbrev;
		String units;
		int colIdx;
		DashDataType<?> dataCol = stdArray.findDataColumn(varname);
		if ( dataCol != null) {
			colIdx = stdArray.findDataColumnIndex(varname);
			fullname = DashboardUtils.isEmptyOrNull(dataCol.getStandardName()) ?
												  dataCol.getDescription() : 
												  dataCol.getStandardName();
		} else {
			colIdx = stdArray.findUserTypeColumn(varname);
			fullname = "other";
		}
		abbrev = stdArray.getUserColumnTypeName(colIdx);
		units = stdArray.getUserColumnUnits(colIdx);
		v.fullName(fullname).abbreviation(abbrev).unit(units);
		oads.addVariable(v);
		return abbrev;
	}

	public static DashboardOADSMetadata readOldOadsXml(File oadsXmlFile) throws Exception {
//		org.w3c.dom.Document xmlDoc = null;
	    JAXBContext jaxbContext = JAXBContext.newInstance(DashboardOADSMetadata.class);
		Unmarshaller jaxbUnmrshlr = jaxbContext.createUnmarshaller();
		DashboardOADSMetadata dbOADSmd = (DashboardOADSMetadata)jaxbUnmrshlr.unmarshal(oadsXmlFile);
	    return dbOADSmd;
	}
	
	public static String createOldOadsMetadataXml(DashboardOADSMetadata mdata) throws Exception {
//	    JAXBContext jaxbContext = JAXBContext.newInstance(mdata.getClass());
//	    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
//	    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	 
	    StringWriter result = new StringWriter();
//	    jaxbMarshaller.marshal(mdata, result);
	    writeOldOadsMetadataXml(mdata, result, true);
	    String xml = result.toString();
	    return xml;
	}
	public static void writeOldOadsMetadataXml(DashboardOADSMetadata mdata, Writer out, boolean formatted) throws Exception {
	    JAXBContext jaxbContext = JAXBContext.newInstance(mdata.getClass());
	    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatted);
	    jaxbMarshaller.marshal(mdata, out);
	}

//	public static Document createOadsMetadataDoc(DashboardOADSMetadata mdata) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	/**
	 * @param args
	 */
	private static void writeToFile(DashboardOADSMetadata oads, File outfile) throws Exception {
		FileWriter fout = new FileWriter(outfile);
		OADSMetadata.writeOldOadsMetadataXml(oads, fout, true);
	}

	public static MetadataPreviewInfo getMetadataPreviewInfo(String pageUsername, String datasetId)
		throws NotFoundException, IllegalArgumentException {
		try {
			MetadataPreviewInfo preview = new MetadataPreviewInfo();
			String xml = null;
			File mdFile = OADSMetadata.getMetadataFile(datasetId);
//			if ( !mdFile.exists()) {
//				mdFile = OADSMetadata.getExtractedMetadataFile(datasetId);
//			}
			if ( !mdFile.exists()) {
				throw new FileNotFoundException("No metadata file found for " + datasetId);
			}
			Date fileModTime = new Date(mdFile.lastModified());
			Path fPath = mdFile.toPath();
			BasicFileAttributes attr = Files.getFileAttributeView(fPath, BasicFileAttributeView.class).readAttributes();
			Date fileCreateTime = new Date(attr.creationTime().toMillis());
			FileInfo mdFileInfo = new FileInfo(mdFile.getName(), fileModTime, fileCreateTime, mdFile.length());
			preview.setMetadataFileInfo(mdFileInfo);
			xml = OADSMetadata.getMetadataXml(datasetId, mdFile);
			String html = XmlUtils.asHtml(xml);
			preview.setMetadataPreview(html);
			return preview;
		} catch (FileNotFoundException ex) {
			throw new NotFoundException(ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	public static File getMetadataFile(String datasetId) throws IOException {
		// Check and standardize the dataset
		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
		DashboardConfigStore configStore = DashboardConfigStore.get(false);
		File parentDir = configStore.getMetadataFileHandler().getMetadataFile(stdId, stdId).getParentFile();
		File metadataFile = new File(parentDir, stdId + "_OADS.xml");
		return metadataFile;
	}
	public static String getMetadataXml(String datasetId, File mdFile) throws IOException {
		if ( !mdFile.exists()) {
			throw new IllegalArgumentException("Specified metadata file not found:" + mdFile);
		}
		String xml = null;
		XReader reader = new XReader(mdFile.getPath());
		xml = reader.getXmlString();
		return xml;
	}
	public static String getMetadataXml(String datasetId) throws IOException {
		File mdFile = getMetadataFile(datasetId);
		if ( !mdFile.exists()) {
			throw new IllegalArgumentException("No metadata file found for dataset " + datasetId);
		}
		return getMetadataXml(datasetId, mdFile);
	}

	public static DashboardOADSMetadata _getCurrentDatasetMetadata(String datasetId, MetadataFileHandler mdHandler) throws Exception {
	    DashboardMetadata mdata = mdHandler.getMetadataInfo(datasetId, MetadataFileHandler.metadataFilename(datasetId)); 
	    return _getCurrentDatasetMetadata(mdata, mdHandler);
    }
	
	public static DashboardOADSMetadata _getCurrentDatasetMetadata(DashboardMetadata mdata, 
	                                                              MetadataFileHandler mdHandler) throws Exception {
		File oadsXmlFile = mdHandler.getMetadataFile(mdata.getDatasetId());
		DashboardOADSMetadata dbOADSmd = readOldOadsXml(oadsXmlFile);
		dbOADSmd.setUploadTimestamp(mdata.getUploadTimestamp());
		dbOADSmd.setOwner(mdata.getOwner());
		dbOADSmd.setVersion(mdata.getVersion());
		
		return dbOADSmd;
	}

    /**
     * @param datasetId
     * @throws IOException 
     */
    public static File createInitialOADSMetadataFile(String datasetId, String userid) throws IOException {
        File mdFile = DashboardConfigStore.get().getMetadataFileHandler().createInitialOADSMetadataFile(datasetId, userid);
        return mdFile;
    }

    /**
     * @param metaFile
     * @return
     * @throws Exception 
     * @throws IOException 
     * @throws SAXException 
     * @throws JAXBException 
     */
    public static String validateMetadata(File metaFile) throws JAXBException, SAXException, 
                                                                IOException, Exception {
        OadsMetadataDocumentType mdDoc = OadsXmlReader.read(metaFile);
        return validateMetadata(mdDoc);
    }
                
    public static String validateMetadata(OadsMetadataDocumentType metadata) {
        String validationMsg;
        try {
            checkSubmitter(metadata);
            checkInvestigators(metadata);
            checkCitationInfo(metadata);
            checkSpatialExtents(metadata);
            checkTemporalExtents(metadata);
            checkVariables(metadata);
            validationMsg = "Validated";
//        } catch (JAXBException ex) {
//            validationMsg = "There was an error processing the metadata file.";
//            ex.printStackTrace();
//        } catch (SAXException ex) {
//            // TODO Auto-generated catch block
//            ex.printStackTrace();
//        } catch (IOException ex) {
//            // TODO Auto-generated catch block
//            ex.printStackTrace();
        } catch (IllegalArgumentException iax) {
            validationMsg = "Metadata has invalid data";
            logger.info(metadata + " : " + validationMsg + ": " + iax);
        } catch (IllegalStateException isx) {
            validationMsg = "Metadata is incomplete";
            logger.info(metadata + " : " + validationMsg + ": " + isx);
        } catch (Exception ex) {
            validationMsg = "Processing Error";
            ex.printStackTrace();
        }
        return validationMsg;
    }

    /**
     * @param mdDoc
     */
    private static void checkSubmitter(OadsMetadataDocumentType mdDoc) {
        PersonType submitter = mdDoc.getDataSubmitter();
        if ( submitter == null ) { throw new IllegalStateException("No data submitter specified."); }
        PersonNameType name = submitter.getName();
        if ( name == null ) { throw new IllegalStateException("Data submitter name is empty."); }
        if ( StringUtils.emptyOrNull(name.getFirst()) || StringUtils.emptyOrNull(name.getLast())) {
            throw new IllegalStateException("Data submitter name is incomplete.");
        }
        if ( StringUtils.emptyOrNull(submitter.getOrganization())) {
            throw new IllegalStateException("No organization specified for data submitter.");
        }
        PersonContactInfoType contactInfo = submitter.getContactInfo();
        if ( contactInfo == null ) { throw new IllegalStateException("Data submitter contact information is empty."); }
        if ( StringUtils.emptyOrNull(contactInfo.getEmail()) && StringUtils.emptyOrNull(contactInfo.getPhone())) {
            throw new IllegalStateException("No contact information for data submitter.");
        }
    }

    /**
     * @param mdDoc
     */
    private static void checkInvestigators(OadsMetadataDocumentType mdDoc) {
        List<PersonType> investigators = mdDoc.getInvestigators();
        if ( investigators == null || investigators.size() == 0 ) { throw new IllegalStateException("Principle investigators list is empty."); }
        PersonType pi = investigators.get(0);
        if ( pi == null ) { throw new IllegalStateException("No principle investigators specified."); }
        PersonNameType name = pi.getName();
        if ( name == null ) { throw new IllegalStateException("Principle investigator name is empty."); }
        if ( StringUtils.emptyOrNull(name.getFirst()) || StringUtils.emptyOrNull(name.getLast())) {
            throw new IllegalStateException("Principle investigator name is incomplete.");
        }
        if ( StringUtils.emptyOrNull(pi.getOrganization())) {
            throw new IllegalStateException("No organization specified for principle investigator.");
        }
    }

    /**
     * @param mdDoc
     */
    private static void checkCitationInfo(OadsMetadataDocumentType mdDoc) {
        if ( StringUtils.emptyOrNull(mdDoc.getTitle())) {
            throw new IllegalStateException("No title given.");
        }
        if ( StringUtils.emptyOrNull(mdDoc.getAbstract())) {
            throw new IllegalStateException("No abstract given.");
        }
    }

    /**
     * @param mdDoc
     */
    private static void checkTemporalExtents(OadsMetadataDocumentType mdDoc) {
        TemporalExtentsType tExtents = mdDoc.getTemporalExtents();
        if ( tExtents == null ) { throw new IllegalStateException("Temporal Extents are empty."); }
        Date start = tExtents.getStartDate();
        Date end = tExtents.getEndDate();
        if ( start == null || end == null ) {
            throw new IllegalStateException("Temporal extents are incomplete.");
        }
        StringBuilder errormsgs = new StringBuilder();
        if ( start.after(end)) {
            errormsgs.append("Start date after end date. ");
        }
        Date now = new Date();
        if ( start.after(now) || end.after(now)) {
            errormsgs.append("Illegal future date.");
        }
        if ( errormsgs.length() > 0 ) {
            throw new IllegalArgumentException(errormsgs.toString());
        }
    }

    private static void checkSpatialExtents(OadsMetadataDocumentType mdDoc) {
        SpatialExtentsType sExtents = mdDoc.getSpatialExtents();
        if ( sExtents == null ) { throw new IllegalStateException("Spatial Extents are empty."); }
        BigDecimal northing = sExtents.getNorthernBounds();
        BigDecimal southing = sExtents.getSouthernBounds();
        BigDecimal easting = sExtents.getEasternBounds();
        BigDecimal westing = sExtents.getWesternBounds();
        if ( northing == null || southing == null || easting == null || westing == null ) {
            throw new IllegalStateException("Spatial extents are incomplete.");
        }
        StringBuilder errormsgs = new StringBuilder();
        double northerly = northing.doubleValue();
        double southerly = southing.doubleValue();
        if ( northerly < southerly ) {
            errormsgs.append("Northern bounds less than southern bounds.");
        }
        if ( northerly > 90 || southerly < -90 ) {
            errormsgs.append("Illegal latitude value.");
        }
        double westerly = westing.doubleValue();
        double easterly = easting.doubleValue();
//        while ( easterly > 180 ) {
//            easterly -= 360;
//        }
//        while ( westerly > 180 ) {
//            westerly -= 360;
//        }
        if ( westerly > 0 && easterly < 0 ) { // crosses dateline
            if ( westerly > 180 || easterly < -180 ) {
                errormsgs.append("Illegal longitude value.");
            }
        }
        if ( westerly < -180 || easterly > 180 ) {
            errormsgs.append("Illegal longitude value.");
        }
        if ( errormsgs.length() > 0 ) {
            throw new IllegalArgumentException(errormsgs.toString());
        }
    }
    
    /**
     * @param mdDoc
     */
    private static void checkVariables(OadsMetadataDocumentType mdDoc) {
        List<BaseVariableType> variables = mdDoc.getVariables();
        if ( variables == null || variables.size() == 0 ) {
            throw new IllegalStateException("Empty variables element.");
        }
        StringBuilder errormsgs = new StringBuilder();
        for ( BaseVariableType var : variables ) {
            if ( StringUtils.emptyOrNull(var.getName()) || 
                 StringUtils.emptyOrNull(var.getDatasetVarName()) ||
                 StringUtils.emptyOrNull(var.getFullName())) {
                errormsgs.append("Incomplete information for " + String.valueOf(var) + ". ");
            }
        }
        if ( errormsgs.length() > 0 ) {
            throw new IllegalStateException(errormsgs.toString());
        }
    }
    
	/**
	 * Create a DsgMetadata object from the data in this object.
	 * Any PI or platform names will be anglicized.  
	 * The version status and QC flag are not assigned.
	 * 
	 * @return
	 *		created DsgMetadata object 
	 */
	public static DsgMetadata createDsgMetadata(OadsMetadataDocumentType mdDoc, String datasetId) throws IllegalArgumentException {
        // Copied/edited from DashboardOADSMetadata

//		// We cannot create a DsgMetadata object if there are conflicts
//		if ( isConflicted() ) {
//			throw new IllegalArgumentException("The Metadata contains conflicts");
//		}

		DashboardConfigStore confStore;
		try {
			confStore = DashboardConfigStore.get(false);
		} catch ( Exception ex ) {
			throw new RuntimeException("Unexpected failure to get the configuration information");
		}
		DsgMetadata scMData = new DsgMetadata(confStore.getKnownMetadataTypes());
		
		scMData.setDatasetId(datasetId);
		// XXX TODO: OME_FILENAME check
//		scMData.setDatasetName(getExperimentName());

		// Anglicize the platform name for NetCDF/LAS
		List<PlatformType> platforms = mdDoc.getPlatforms();
		if ( platforms != null && ! platforms.isEmpty() ) {
            PlatformType platform = platforms.get(0);
			String platformName = platform.getName(); // XXX what if there are more than one...
			scMData.setPlatformName(Anglican.anglicize(platformName));
			// Set the platform type - could be missing
			try {
				scMData.setPlatformType(platform.getType());
			} catch ( Exception ex ) {
				scMData.setPlatformType(null);
			}
		}

		try {
			scMData.setWestmostLongitude(mdDoc.getSpatialExtents().getWesternBounds().doubleValue());
		} catch (NumberFormatException | NullPointerException ex) {
			scMData.setWestmostLongitude(null);				
		}

		try {
			scMData.setEastmostLongitude(mdDoc.getSpatialExtents().getEasternBounds().doubleValue());
		} catch (NumberFormatException | NullPointerException ex) {
			scMData.setEastmostLongitude(null);
		}

		try {
			scMData.setSouthmostLatitude(mdDoc.getSpatialExtents().getSouthernBounds().doubleValue());
		} catch (NumberFormatException | NullPointerException ex) {
			scMData.setSouthmostLatitude(null);
		}

		try {
			scMData.setNorthmostLatitude(mdDoc.getSpatialExtents().getNorthernBounds().doubleValue());
		} catch (NumberFormatException | NullPointerException ex) {
			scMData.setNorthmostLatitude(null);
		}
		
//		SimpleDateFormat dateParser = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
//		dateParser.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			scMData.setBeginTime(Double.valueOf(mdDoc.getTemporalExtents().getStartDate().getTime() / 1000.0));
		} catch (Exception ex) {
			scMData.setBeginTime(null);
		}
		try {
			scMData.setEndTime(Double.valueOf(mdDoc.getTemporalExtents().getStartDate().getTime() / 1000.0));
		} catch (Exception ex) {
			scMData.setEndTime(null);
		}

		StringBuffer piNames = new StringBuffer();
        List<PersonType> investigators = mdDoc.getInvestigators();
		if ( investigators != null ) {
			for ( PersonType investigator : investigators ) {
				if ( piNames.length() > 0 )
					piNames.append(DsgMetadata.NAMES_SEPARATOR);
				// Anglicize investigator names for NetCDF/LAS
				piNames.append(Anglican.anglicize(getStandardFullName(investigator)));
			}
			scMData.setInvestigatorNames(piNames.toString());
		}

		HashSet<String> usedOrganizations = new HashSet<String>();
		StringBuffer orgGroup = new StringBuffer();
        List<PersonType> pis = mdDoc.getInvestigators();
		if ( pis != null ) {
            for ( PersonType pi : pis ) {
                String orgName = pi.getOrganization();
				if ( StringUtils.emptyOrNull(orgName)) 
					continue;
				if ( usedOrganizations.add(orgName) ) {
					if ( orgGroup.length() > 0 )
						orgGroup.append(DsgMetadata.NAMES_SEPARATOR);
					// Anglicize organizations names for NetCDF/LAS
					orgGroup.append(Anglican.anglicize(orgName));
				}
			}
		}
		String allOrgs = orgGroup.toString().trim();
		if ( allOrgs.isEmpty() )
			allOrgs = DashboardOADSMetadata.DEFAULT_ORGANIZATION; // XXX
		scMData.setOrganizationName(allOrgs);

		return scMData;
	}
	/**
     * @param person
     * @return the full name of the person as a single string
     */
    private static String getStandardFullName(PersonType person) {
        if ( person == null ) throw new IllegalArgumentException("Attempting to get full name of null person.");
        PersonNameType name = person.getName();
        StringBuilder sb = new StringBuilder();
        sb.append(name.getFirst()).append(" ");
        if ( name.getMiddle() != null ) { sb.append(name.getMiddle()).append(" "); }
        sb.append(name.getLast()).append(" ");
        if ( name.getSuffix() != null ) { sb.append(name.getSuffix()); }
        return sb.toString().trim();
    }

    public static void _main(String[] args) {
		try {
			String filename = "/Users/kamb/tomcat/7/content/OAPUploadDashboard/MetadataDocs/CHAB/CHABA062014/CHABA062014_OADS.xml";
			File mdFile = new File(filename);
			DashboardOADSMetadata dbOAmd = OADSMetadata.readOldOadsXml(mdFile);
			System.out.println(dbOAmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		try {
			File file = new File("/local/tomcat/oap_content/OAPUploadDashboard/MetadataDocs//PRIS/PRISM022008/PRISM022008_OADS.xml");
//			DashboardOADSMetadata omd = OADSMetadata.readOadsXml(file);
//			System.out.println(omd);
            validateMetadata(file);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	}

}
