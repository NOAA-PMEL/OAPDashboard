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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import gov.noaa.ncei.oads.xml.v_a0_2_2.BaseVariableType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.OadsMetadataDocumentType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonContactInfoType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonNameType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.SpatialExtentsType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.TemporalExtentsType;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FileInfo;
import gov.noaa.pmel.dashboard.shared.MetadataPreviewInfo;
import gov.noaa.pmel.dashboard.shared.NotFoundException;
import gov.noaa.pmel.dashboard.util.xml.XReader;
import gov.noaa.pmel.dashboard.util.xml.XmlUtils;
import gov.noaa.pmel.oads.util.StringUtils;
import gov.noaa.pmel.oads.xml.a0_2_2.OadsXmlReader;

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
			double lat = lats[i].doubleValue();
			double lon = lons[i].doubleValue();
			double time = times[i].doubleValue();
			if ( lat > maxLat ) { maxLat = lat; }
			if ( lat < minLat ) { minLat = lat; }
			if ( lon > maxLon ) { maxLon = lon; }
			if ( lon < minLon ) { minLon = lon; }
			if ( time > maxTime ) { maxTime = time; }
			if ( time < minTime ) { minTime = time; }
		}
		GeoTemporalExtents extents = new GeoTemporalExtents(new Extents(maxLat, minLat), 
		                                                    new Extents(maxLon, minLon), 
		                                                    new Extents(maxTime, minTime));
		return extents;
	}
	
	private static String[] requiredVars = new String[] {
			"inorganic_carbon",
			"alkalinity",
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
	
	public static DashboardOADSMetadata extractOADSMetadata(StdUserDataArray stdArray) {
		DashboardOADSMetadata oads = new DashboardOADSMetadata(stdArray.getDatasetName());
		GeoTemporalExtents gtExtents = extractGeospatialTemporalExtents(stdArray);
		oads.westernBound(gtExtents.lonExtents.minValue);
		oads.easternBound(gtExtents.lonExtents.maxValue);
		oads.northernBound(gtExtents.latExtents.maxValue);
		oads.southernBound(gtExtents.latExtents.minValue);
		oads.startDate(new Date((long)(1000*gtExtents.timeExtents.minValue)));
		oads.endDate(new Date((long)(1000*gtExtents.timeExtents.maxValue)));
		Set<String> addVars = new HashSet<>();
		for (String varname : requiredVars ) {
			try {
				String addName = addVariable(varname, oads, stdArray);
				addVars.add(addName);
			} catch (NoSuchFieldException e) {
				System.err.println(e);
				logger.info("No data column found for variable " + varname);
			}
		}
		for (int i = 0; i < stdArray.getNumDataCols(); i++ ) {
			String username = stdArray.getUserColumnTypeName(i);
			String uunits = stdArray.getUserColumnUnits(i);
			DashDataType<?> colType = stdArray.getDataTypes().get(i);
			String stdName = colType.getStandardName();
			String fullName = DashboardUtils.isEmptyNull(stdName) ? username : stdName;
			if ( ! exclude(username, colType) &&
				 ! addVars.contains(username)) {
				oads.addVariable(new Variable().abbreviation(username).unit(uunits).fullName(fullName));
			}
		}
		return oads;
	}

	private static boolean exclude(String username, DashDataType<?> colType) {
		return false;
	}

	private static String addVariable(String varname, DashboardOADSMetadata oads, StdUserDataArray stdArray) 
			throws NoSuchFieldException {
		Variable v = new Variable();
		
		String fullname;
		String abbrev;
		String units;
		int colIdx;
		DashDataType<?> dataCol = stdArray.findDataColumn(varname);
		if ( dataCol != null) {
			colIdx = stdArray.findDataColumnIndex(varname);
			fullname = DashboardUtils.isEmptyNull(dataCol.getStandardName()) ?
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

	public static DashboardOADSMetadata readOadsXml(File oadsXmlFile) throws Exception {
//		org.w3c.dom.Document xmlDoc = null;
	    JAXBContext jaxbContext = JAXBContext.newInstance(DashboardOADSMetadata.class);
		Unmarshaller jaxbUnmrshlr = jaxbContext.createUnmarshaller();
		DashboardOADSMetadata dbOADSmd = (DashboardOADSMetadata)jaxbUnmrshlr.unmarshal(oadsXmlFile);
	    return dbOADSmd;
	}
	
	public static String createOadsMetadataXml(DashboardOADSMetadata mdata) throws Exception {
//	    JAXBContext jaxbContext = JAXBContext.newInstance(mdata.getClass());
//	    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
//	    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	 
	    StringWriter result = new StringWriter();
//	    jaxbMarshaller.marshal(mdata, result);
	    writeOadsMetadataXml(mdata, result, true);
	    String xml = result.toString();
	    return xml;
	}
	public static void writeOadsMetadataXml(DashboardOADSMetadata mdata, Writer out, boolean formatted) throws Exception {
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
		OADSMetadata.writeOadsMetadataXml(oads, fout, true);
	}

	public static MetadataPreviewInfo getMetadataPreviewInfo(String pageUsername, String datasetId)
		throws NotFoundException, IllegalArgumentException {
		try {
			MetadataPreviewInfo preview = new MetadataPreviewInfo();
			String xml = null;
			File mdFile = OADSMetadata.getMetadataFile(datasetId);
			if ( !mdFile.exists()) {
				mdFile = OADSMetadata.getExtractedMetadataFile(datasetId);
			}
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

	public static File getExtractedMetadataFile(String datasetId) throws IOException {
		// Check and standardize the dataset
		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
		String extractedFile =  DashboardUtils.autoExtractedMdFilename(stdId);
		DashboardConfigStore configStore = DashboardConfigStore.get(false);
		File parentDir = configStore.getMetadataFileHandler().getMetadataFile(stdId, extractedFile).getParentFile();
		File metadataFile = new File(parentDir, extractedFile);
		return metadataFile;
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

	public static DashboardOADSMetadata getCurrentDatasetMetadata(String datasetId, MetadataFileHandler mdHandler) throws Exception {
	    DashboardMetadata mdata = mdHandler.getMetadataInfo(datasetId, DashboardUtils.metadataFilename(datasetId)); 
	    return getCurrentDatasetMetadata(mdata, mdHandler);
    }
	
	public static DashboardOADSMetadata getCurrentDatasetMetadata(DashboardMetadata mdata, 
	                                                              MetadataFileHandler mdHandler) throws Exception {
		File oadsXmlFile = mdHandler.getMetadataFile(mdata.getDatasetId());
		DashboardOADSMetadata dbOADSmd = readOadsXml(oadsXmlFile);
		dbOADSmd.setUploadTimestamp(mdata.getUploadTimestamp());
		dbOADSmd.setOwner(mdata.getOwner());
		dbOADSmd.setVersion(mdata.getVersion());
		
		return dbOADSmd;
	}

    /**
     * @param datasetId
     * @throws IOException 
     */
    public static File createEmptyOADSMetadataFile(String datasetId) throws IOException {
        File mdFile = DashboardConfigStore.get().getMetadataFileHandler().createEmptyOADSMetadataFile(datasetId);
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
            validationMsg = "Validated.";
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
    
	public static void _main(String[] args) {
		try {
			String filename = "/Users/kamb/tomcat/7/content/OAPUploadDashboard/MetadataDocs/CHAB/CHABA062014/CHABA062014_OADS.xml";
			File mdFile = new File(filename);
			DashboardOADSMetadata dbOAmd = OADSMetadata.readOadsXml(mdFile);
			System.out.println(dbOAmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void __main(String[] args) {
		try {
//			String dataFilesDirName = "/Users/kamb/tomcat/7/content/OAPUploadDashboard/CruiseFiles";
			String configDir = "/Users/kamb/tomcat/7";
			System.setProperty("OA_DOCUMENT_ROOT", configDir);
			System.setProperty("UPLOAD_DASHBOARD_SERVER_NAME", "OAPUploadDashboard");
			String datasetId = "CHABA062014";
			DashboardConfigStore config = DashboardConfigStore.get(false);
			DataFileHandler dfh = config.getDataFileHandler();
			DashboardDatasetData ddd = dfh.getDatasetDataFromFiles(datasetId, 0, -1);
			StdUserDataArray std = new StdUserDataArray(ddd, config.getKnownUserDataTypes());
			DashboardOADSMetadata oads = OADSMetadata.extractOADSMetadata(std);
			String xml = createOadsMetadataXml(oads);
			System.out.println(xml);
			File outfile = new File(datasetId+"_OADS.xml");
			OADSMetadata.writeToFile(oads, outfile);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
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
