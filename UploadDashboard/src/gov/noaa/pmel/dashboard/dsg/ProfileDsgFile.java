/**
 * 
 */
package gov.noaa.pmel.dashboard.dsg;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map.Entry;
import java.util.Set;

import gov.noaa.pmel.dashboard.datatype.CastSet;
import gov.noaa.pmel.dashboard.datatype.CharDashDataType;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.DoubleDashDataType;
import gov.noaa.pmel.dashboard.datatype.IntDashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.datatype.StringDashDataType;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayString;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFileWriter.Version;

/**
 * @author kamb
 *
 */
public class ProfileDsgFile extends DsgNcFile {

	private static final long serialVersionUID = -7178421505101183090L;

	private static Logger logger = LogManager.getLogger(ProfileDsgFile.class);
	
	private static enum ElemCategory {
		METADATA,
		PROFILE,
		DATA
	}
	private class Dims {
		List<Dimension> _dString;
		List<Dimension> _dChar;
		List<Dimension> _dNum;
		
		Dims(List<Dimension> dNum, List<Dimension> dChar, List<Dimension> dString) {
			_dNum = dNum;
			_dChar = dChar;
			_dString = dString;
		}
		
		List<Dimension> strings() { return _dString; }
		List<Dimension> chars() { return _dChar; }
		List<Dimension> nums() { return _dNum; }
	}
	
//	private static Set<String> dsgVariableTypes = null;
	// Dimensions
	// n_profiles
	// n_observations
	// 
	// Attributes
	// dataset_id
	//
	// Variables
	// profile(n_profiles)
	// time(n_profiles)
	// lat(n_profiles)
	// lon(n_profiles)
	// row_count(n_profiles)
	// 
	// sample_id(n_obs)
	// sample_depth(n_obs)
	// sample_time(n_obs)
	// sample_latitude(n_obs)
	// sample_longitude(n_obs)
	
//// QC'd Variables
//	CTD_salinity
//	CTD_Sal_QC
//	Bottle_salt
//	Bottle_Salt_QC
//	CTD_oxygen (µmol/kg)
//	CTD_oxygen_QC
//	Bottle_oxygen (µmol/kg)
//	Bottle_oxygen_QC
//	Silicate (µmol/kg)
//	Silicate_QC
//	Nitrate (µmol/kg)
//	Nitrate_QC
//	Nitrite (µmol/kg)
//	Nitrite_QC
//	Phosphate (µmol/kg)
//	Phosphate_QC
//	Ammonium (µmol/kg)
//	Ammonium_QC
//	// Nutrients_QC
//	// Nutrient_analysis_temperature
//	DIC (µmol/kg)
//	DIC_QC
//	Alkalinity (µmol/kg)
//	Alkalinity_QC
//	pH_TOT@25
//	pH_TOT@25_QC
//	pH_TMP
//	Carbonate ion
//	Carbonate_ion_QC
	
	private static final String[] qcd_vars = {
		"ctd_salinity",
		"salinity",
		"ctd_oxygen",
		"oxygen",
		"silicate",
		"nitrate",
		"nitrite",
		"phosphate",
		"ammonium",
		"inorganic_carbon",
		"alkalinity",
		"ph_total",
		"carbonate_ion"
	};
	
	public static final String PROF_ID_SEPARATOR = "__";
	
	private static final String SAMPLES_PER_CAST_NAME = "rowCount";

	private static final String QC_VARNAME_EXTENSION = "_qc";
	
	enum TimeFormat {
		tf_8601Z("yyyy-mm-dd'T'hh:mm:ssZ"),
		tf_8601_compressed("yyyymmdd'T'hhmmssZ");
		
		private String _format;
		private TimeFormat(String formatString) {
			_format = formatString;
		}
		private String pattern() {
			return _format;
		}
	}
	public ProfileDsgFile(String filename) {
		super(filename);
	}
	public ProfileDsgFile(File parent, String child) {
		super(parent, child);
	}
	
	private StdUserDataArray _stdUser;
	private String _datasetId;
	
	private Collection<CastSet> _casts;
	private Dimension _dimNumProfiles;
	private Dimension _dimNumObs;
	private Dimension _one;
	private int _maxStrLen;
	List<Dimension> _metaDimList = new ArrayList<Dimension>();
	List<Dimension> _metaCharDimList = new ArrayList<Dimension>();
	List<Dimension> _metaStringDimList = new ArrayList<Dimension>();
	List<Dimension> _profileDimList = new ArrayList<Dimension>();
	List<Dimension> _profStringDimList = new ArrayList<Dimension>();
	List<Dimension> _profCharDimList = new ArrayList<Dimension>();
	List<Dimension> _dataDimList = new ArrayList<Dimension>();
	List<Dimension> _dataCharDimList = new ArrayList<Dimension>();
	List<Dimension> _dataStringDimList = new ArrayList<>();
	Dims _metadataDims;
	Dims _profileDims;
	Dims _dataDims;
	
	private void createDimensions(NetcdfFileWriter ncfile) {
		int nCasts = _casts.size();
		int numSamples = stddata.getNumSamples();
		_maxStrLen = metadata.getMaxStringLength(); // XXX This may not be correct for other string lengths...
		
		Dimension dStringLen = ncfile.addDimension(null, "string_length", _maxStrLen);
		Dimension dCharLen = ncfile.addDimension(null, "char_length", 1);
		
		_one = ncfile.addDimension(null, "one", 1);
		_metaDimList.add(_one);
//		_metaCharDimList.add(_one);
		_metaCharDimList.add(dCharLen);
//		_metaStringDimList.add(_one);
		_metaStringDimList.add(dStringLen);
		_metadataDims = new Dims(_metaDimList, _metaCharDimList, _metaStringDimList);
		
		_dimNumProfiles = ncfile.addDimension(null, "profile", nCasts);
		_profileDimList.add(_dimNumProfiles);

		_profStringDimList.add(_dimNumProfiles);
		_profStringDimList.add(dStringLen);

		_profCharDimList.add(_dimNumProfiles);
		_profCharDimList.add(dCharLen);
		_profileDims = new Dims(_profileDimList, _profCharDimList, _profStringDimList);

		_dimNumObs = ncfile.addDimension(null, "obs", numSamples);
		_dataDimList.add(_dimNumObs);

		_dataCharDimList.add(_dimNumObs);
		_dataCharDimList.add(dCharLen);
		
		_dataStringDimList.add(_dimNumObs);
		_dataStringDimList.add(dStringLen);
		_dataDims = new Dims(_dataDimList, _dataCharDimList, _dataStringDimList);
	}
	
	private void addMetadataAttributes(NetcdfFileWriter ncfile) {
		String varName;
		for ( Entry<DashDataType<?>,Object> entry : metadata.getValuesMap().entrySet() ) {
			DashDataType<?> dtype = entry.getKey();
			Object value = entry.getValue();
			varName = dtype.getVarName();
			logger.debug("metadata var:"+varName + ": " + value);
			if ( DashboardUtils.isNullOrNull(value)) { continue; }
			ncfile.addGroupAttribute(null, new Attribute(getAttributeNameFor(dtype), String.valueOf(value)));
		}
	}
	
	private String getAttributeNameFor(DashDataType<?> dtype) {
		String name = dtype.getStandardName();
		if ( name == null || DashboardUtils.STRING_MISSING_VALUE.equals(name)) {
			name = dtype.getVarName();
		}
		return name;
	}
	
	private void addAttributes(NetcdfFileWriter ncfile) {
		ncfile.addGroupAttribute(null, new Attribute("cdm_data_type", "Profile"));
		ncfile.addGroupAttribute(null, new Attribute("featureType", "Profile"));
		ncfile.addGroupAttribute(null, new Attribute("Conventions", "CF-1.6,COARDS,ACDD-1.3"));
		ncfile.addGroupAttribute(null, new Attribute("COORD_SYSTEM", "Geographical"));
		ncfile.addGroupAttribute(null, new Attribute("creation_date", formatUTC(System.currentTimeMillis())));
		ncfile.addGroupAttribute(null, new Attribute("history", DSG_VERSION));
		ncfile.addGroupAttribute(null, new Attribute("id", _stdUser.getDatasetId()));
		ncfile.addGroupAttribute(null, new Attribute("dataset_id", _stdUser.getDatasetId()));
		addMetadataAttributes(ncfile);
	}
	
	private void addMetadataVariables(NetcdfFileWriter ncfile) throws Exception {
		for ( Entry<DashDataType<?>,Object> entry : metadata.getValuesMap().entrySet() ) {
			DashDataType<?> dtype = entry.getKey();
			Object value = entry.getValue();
			String varName = dtype.getVarName();
			logger.debug("metadata var:"+varName + ":" + value);
			if ( DashboardUtils.isEmptyNullOrNull(value)) { continue; }
			Variable var = addVariableFor(ncfile, dtype, ElemCategory.METADATA);
			logger.debug("Added metadata variable " + var);
		}
	}
	private void writeMetadataVariables(NetcdfFileWriter ncfile) throws Exception {
		for (  Entry<DashDataType<?>,Object> entry : metadata.getValuesMap().entrySet() ) {
			DashDataType<?> dtype = entry.getKey();
			String varName = dtype.getVarName();
			Object value = entry.getValue();
			Variable var = ncfile.findVariable(varName);
			if ( var == null || DashboardUtils.isNullOrNull(value)) {
				continue;
			}
			writeVariableData(ncfile, var, dtype, new Object[] { value });
		}
	}
	
	private void addProfileVariables(NetcdfFileWriter ncfile) {
		addProfileIdVariable(ncfile, _profileDimList);
//		addProfileVariable(ncfile, _profileDimList);
		addProfileTimeVariable(ncfile, _profileDimList);
		addLatitudeVariable(ncfile, PROFILE_LAT_VARNAME, "Cast Latitude", _profileDimList);
		addLongitudeVariable(ncfile, PROFILE_LON_VARNAME , "Cast Longitude", _profileDimList);
			
		Variable var = ncfile.addVariable(null, SAMPLES_PER_CAST_NAME, DataType.INT, _profileDimList);
		ncfile.addVariableAttribute(var, new Attribute("sample_dimension", "obs"));
		ncfile.addVariableAttribute(var, new Attribute("long_name", "Number of Observations in this Profile"));
		ncfile.addVariableAttribute(var, new Attribute("ioos_category", "Identifier"));
	}
	
	private void writeProfileVariables(NetcdfFileWriter ncfile) throws IOException, InvalidRangeException {
		int nCasts = _casts.size();
		Variable vProfId = getVariable(ncfile, PROFILE_ID_VARNAME);
		ArrayChar.D2 aProfId = new ArrayChar.D2(nCasts, _maxStrLen);
//		Variable vProf = getVariable(ncfile, PROFILE_VARNAME);
//		ArrayChar.D2 aProf = new ArrayChar.D2(nCasts, _maxStrLen);
		Variable vRowCount = getVariable(ncfile, SAMPLES_PER_CAST_NAME);
		ArrayInt.D1 aRowCount = new ArrayInt.D1(nCasts);
		Variable vProfTime = getVariable(ncfile, "time");
		ArrayDouble.D1 aProfTime = new ArrayDouble.D1(nCasts);
		Variable vProfLat = getVariable(ncfile, "lat");
		ArrayDouble.D1 aProfLat = new ArrayDouble.D1(nCasts);
		Variable vProfLon = getVariable(ncfile, "lon");
		ArrayDouble.D1 aProfLon = new ArrayDouble.D1(nCasts);
		
		int castNum = 0;
		for (CastSet cast : _casts) {
			aProfId.setString(castNum, datasetProfileId(cast.id()));
//			aProfId.setString(castNum, cast.id());
			aRowCount.set(castNum, cast.size());
			aProfTime.set(castNum, cast.expectedTime());
			aProfLat.set(castNum, cast.expectedLat());
			aProfLon.set(castNum, cast.expectedLon());
			castNum += 1;
		}
		ncfile.write(vProfId, aProfId);
		ncfile.write(vRowCount, aRowCount);
		ncfile.write(vProfTime, aProfTime);
		ncfile.write(vProfLat, aProfLat);
		ncfile.write(vProfLon, aProfLon);
		ncfile.flush();
	}
		
	private String datasetProfileId(String id) {
        return _datasetId + PROF_ID_SEPARATOR + id;
    }

    private Map<String, DashDataType<?>> qcCols = new HashMap<>();
	
	private void addObservationVariables(NetcdfFileWriter ncfile) {
		addSampleIdVariable(ncfile, _dataDims.strings());
		addSampleTimeVariable(ncfile, _dataDimList);
		addSampleLocationVariables(ncfile, _dataDimList);
		Collection<DashDataType<?>> dsgVariableTypes = getDataVariablesToWriteToDsgFile();
		for (DashDataType<?> dtype : dsgVariableTypes) {
			if ( excludeType(dtype)) {
				logger.debug("Skipping excluded column: " + dtype);
				continue;
			}
			if ( dtype.isQCType()) {
				qcCols.put(dtype.getVarName(), dtype);
				continue;
			}
			DashDataType<?> dataCol =  _stdUser.findDataColumn(dtype.getVarName());
			if ( dataCol == null ) {
				logger.warn("No data column found for type: " + dtype.getVarName());
				continue;
			}
			Variable var = addVariableFor(ncfile, dtype, ElemCategory.DATA);
			logger.debug("Added data variable " + var);
		}
	}
	
	private void addQcFlagVariables(NetcdfFileWriter ncfile, KnownDataTypes dataTypes) {
		for (String qcdVar : qcd_vars) {
			DashDataType<?> dataCol =  _stdUser.findDataColumn(qcdVar);
			if ( dataCol == null ) {
				logger.info("No data column found for " + qcdVar+". Skipping QC variable.");
				continue;
			}
			String qcName = qcdVar + QC_VARNAME_EXTENSION;
			if ( qcCols.containsKey(qcName)) {
				qcCols.remove(qcName);
			} else {
				System.out.println("Need to add column for :" + qcName);
			}
			DashDataType<?> qcCol =  _stdUser.findDataColumn(qcName);
			if ( qcCol == null ) {
				System.out.println("No column found for " + qcName + ". Adding one.");
				qcCol = dataTypes.getDataType(qcName);
			}
			if ( qcCol == null ) {
				System.out.println("No datatype found for " + qcName + ". Skipping.");
				continue;
			}
			Variable var = addVariableFor(ncfile, qcCol, ElemCategory.DATA);
			logger.debug("Added qc variable " + var);
		}
		if ( ! qcCols.isEmpty()) {
			for (DashDataType<?> qcCol : qcCols.values()) {
				Variable var = addVariableFor(ncfile, qcCol, ElemCategory.DATA);
				logger.debug("Added qc variable " + var);
			}
		}
		
	}
		
	private static boolean excludeType(DashDataType<?> dtype) {
		if ( dtype.getVarName().equals("other") ||
		     dtype.typeNameLike("unknown") ||
//		     dtype.isQCType() ||
		     dtype.typeNameEquals("date") || dtype.typeNameEquals("time") || dtype.typeNameEquals("timestamp") ||
		     dtype.typeNameEquals("dataset_id") || dtype.typeNameEquals("dataset_name") ||
		     dtype.typeNameLike("comment") ||
		     dtype.typeNameLike("time") ||
		     dtype.typeNameEquals("latitude") || dtype.typeNameEquals("longitude") ||
		     dtype.typeNameEquals(SAMPLE_ID_VARNAME)) 
		    return true;
		return false;
	}
	private void writeObservationVariables(NetcdfFileWriter ncfile) throws Exception {
	    writeSampleId(ncfile);
		writeSampleTime(ncfile);
		writeSampleLocations(ncfile);
		Collection<DashDataType<?>> dsgVariableTypes = getDataVariablesToWriteToDsgFile();
		for (DashDataType<?> dtype : dsgVariableTypes) {
			if ( excludeType(dtype)) {
				logger.debug("Skipping excluded type: " + dtype);
				continue;
			}
			String varName = dtype.getVarName();
			DashDataType<?> dataCol =  _stdUser.findDataColumn(varName);
			if ( dataCol == null ) {
				System.err.println("Unexpected Error: No data column found for type: " + varName);
				continue;
			}
			Variable var = ncfile.findVariable(varName);
			if ( var == null ) {
				System.err.println("Did not find Variable for " + varName);
				continue;
			}
			int dataColumnIdx = _stdUser.findDataColumnIndex(varName);
			if ( dataColumnIdx == -1 ) {
				System.err.println("Did not find data column for " + varName);
				continue;
			}
			Object[] data = _stdUser.getStdValues(dataColumnIdx);
			writeVariableData(ncfile, var, dtype, data);
		}
	}
	
	private void writeVariableData(NetcdfFileWriter ncfile, Variable var, DashDataType<?> dtype, Object[] data) 
			throws Exception {
		int numSamples = data.length;
		if ( dtype instanceof CharDashDataType ) {
			ArrayChar dvar = var.getRank() > 1 ?
								new ArrayChar.D2(numSamples, 1) :
								new ArrayChar.D1(1);
			for (int j = 0; j < numSamples; j++) {
				Character dvalue = (Character) data[j];
				if ( dvalue == null ) {
					dvalue = DashboardUtils.CHAR_MISSING_VALUE;
				}
				dvar.setChar(j, dvalue.charValue());
			}
			ncfile.write(var, dvar);
		}
		else if ( dtype instanceof StringDashDataType ) {
			ArrayChar dvar =   var.getRank() > 1 ? 
								new ArrayChar.D2(numSamples, _maxStrLen) :
								new ArrayChar.D1(_maxStrLen);
			for (int j = 0; j < numSamples; j++) {
				String dvalue = (String) data[j];
				if ( dvalue == null ) {
					dvalue = DashboardUtils.STRING_MISSING_VALUE;
				}
				if ( var.getRank() > 1 ) {
					dvar.setString(j, dvalue);
				} else {
					dvar.setString(dvalue);
				}
			}
			ncfile.write(var, dvar);
		}
		else if ( dtype instanceof IntDashDataType ) {
			ArrayInt.D1 dvar = new ArrayInt.D1(numSamples);
			for (int j = 0; j < numSamples; j++) {
				Integer dvalue = (Integer) data[j];
				if ( dvalue == null )
					dvalue = DashboardUtils.INT_MISSING_VALUE;
				dvar.set(j, dvalue.intValue());
			}
			ncfile.write(var, dvar);
		}
		else if ( dtype instanceof DoubleDashDataType ) {
			// Data Doubles
			ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
			for (int j = 0; j < numSamples; j++) {
				Double dvalue = (Double) data[j];
				if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
					dvalue = DashboardUtils.FP_MISSING_VALUE;
				dvar.set(j, dvalue.doubleValue());
			}
			ncfile.write(var, dvar);
		}
		else {
			throw new IllegalArgumentException("unexpected unknown data type: " + dtype.toString());
		}
		ncfile.flush();
	}
		
	private void writeSampleId(NetcdfFileWriter ncfile) throws Exception {
		int numSamples = _stdUser.getNumSamples();
		int idColIdx = _stdUser.getNumDataCols()-2; // _stdUser.findDataColumnIndex(SAMPLE_ID_VARNAME);
		boolean useColumn = idColIdx != -1;
		Variable ncSampleId = ncfile.findVariable(SAMPLE_ID_VARNAME);
		ArrayChar.D2 idVar = new ArrayChar.D2(numSamples, _maxStrLen);
		for (int sampleRow = 0; sampleRow < numSamples; sampleRow++) {
		    String sid = useColumn ? String.valueOf(_stdUser.getStdVal(sampleRow, idColIdx)) : "_"+sampleRow;
		    idVar.setString(sampleRow, sid);
		}
		ncfile.write(ncSampleId, idVar);
	}
	private void writeSampleTime(NetcdfFileWriter ncfile) throws Exception {
		int numSamples = _stdUser.getNumSamples();
		Double[] sampleTimes = _stdUser.getSampleTimes();
		Variable vSampleTime = ncfile.findVariable(SAMPLE_TIME_VARNAME);
		ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
		for (int j = 0; j < numSamples; j++) {
			Double dvalue = sampleTimes[j];
			if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
				dvalue = DashboardUtils.FP_MISSING_VALUE;
			dvar.set(j, dvalue.doubleValue());
		}
		ncfile.write(vSampleTime, dvar);
	}
	private void writeSampleLocations(NetcdfFileWriter ncfile) throws Exception {
		int numSamples = _stdUser.getNumSamples();
		Double[] sampleLats = _stdUser.getSampleLatitudes();
		Double[] sampleLons = _stdUser.getSampleLongitudes();
		Variable vSampleLats = ncfile.findVariable(SAMPLE_LAT_VARNAME);
		Variable vSampleLons = ncfile.findVariable(SAMPLE_LON_VARNAME);
		ArrayDouble.D1 dLatVar = new ArrayDouble.D1(numSamples);
		ArrayDouble.D1 dLonVar = new ArrayDouble.D1(numSamples);
		for (int j = 0; j < numSamples; j++) {
			Double dLatValue = sampleLats[j];
			Double dLonValue = sampleLons[j];
			if ( (dLatValue == null) || dLatValue.isNaN() || dLatValue.isInfinite() )
				dLatValue = DashboardUtils.FP_MISSING_VALUE;
			dLatVar.set(j, dLatValue.doubleValue());
			if ( (dLonValue == null) || dLonValue.isNaN() || dLonValue.isInfinite() )
				dLonValue = DashboardUtils.FP_MISSING_VALUE;
			dLonVar.set(j, dLonValue.doubleValue());
		}
		ncfile.write(vSampleLats, dLatVar);
		ncfile.write(vSampleLons, dLonVar);
	}
    @Override
	protected void checkIndeces(StdDataArray stddata) {
		// Quick check of data column indices already assigned in StdDataArray
		if ( ! stddata.hasLongitude() )
			throw new IllegalArgumentException("no longitude data column");
		if ( ! stddata.hasLatitude() )
			throw new IllegalArgumentException("no latitude data column");
		if ( ! ( stddata.hasSampleDepth() || stddata.hasSamplePressure() ))
			throw new IllegalArgumentException("no sample depth data column");
		if ( ! stddata.hasYear() )
			throw new IllegalArgumentException("no year data column");
		if ( ! stddata.hasMonthOfYear() )
			throw new IllegalArgumentException("no month of year data column");
		if ( ! stddata.hasDayOfMonth() )
			throw new IllegalArgumentException("no day of month data column");
		if ( ! stddata.hasHourOfDay() )
			throw new IllegalArgumentException("no hour of day data column");
		if ( ! stddata.hasMinuteOfHour() )
			throw new IllegalArgumentException("no minute of hour data column");
		if ( ! stddata.hasSecondOfMinute() )
			throw new IllegalArgumentException("no second of minute data column");
	}
	@Override
	public void create(DsgMetadata metaData, StdDataArray fileData) 
			throws IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException {
		throw new IllegalStateException("Not implemented");
	}
		
	@Override
	public void create(DsgMetadata metaData, StdUserDataArray fileData, KnownDataTypes dataTypes) 
			throws Exception, IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException {
		if ( metaData == null )
			throw new IllegalArgumentException("no metadata given");
		metadata = metaData;
		_datasetId = metadata.getDatasetId();
		if ( fileData == null )
			throw new IllegalArgumentException("no data given");
		stddata = fileData;
		_stdUser = (StdUserDataArray)fileData;
		
		checkIndeces(stddata);
		
		
		try ( NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(Version.netcdf3, getPath()); ) {
			_casts = CastSet.extractCastSetsFrom(_stdUser);
			
			createDimensions(ncfile);
			
			addAttributes(ncfile);
			
			addMetadataVariables(ncfile);
			addProfileVariables(ncfile);
			addObservationVariables(ncfile);
			addQcFlagVariables(ncfile, dataTypes);

			ncfile.create();

			// The header has been created.  Now let's fill it up.
			
			writeMetadataVariables(ncfile);
			writeProfileVariables(ncfile);
			writeObservationVariables(ncfile);
		}
	}
	
	private Variable getVariable(NetcdfFileWriter ncfile, String varName) {
		Variable var = ncfile.findVariable(varName);
		if ( var == null ) {
			throw new RuntimeException("Unexpected failure to find ncfile variable " + varName);
		}
		return var;
	}
	private String formatUTC(long timeMillis) {
		Date d = new Date(timeMillis);
		SimpleDateFormat sdf = getDateFormatter(TimeFormat.tf_8601Z.pattern(), TimeZone.getTimeZone("UTC"));
		return sdf.format(d);
	}
	private SimpleDateFormat getDateFormatter(String formatPattern, TimeZone zone) {
		SimpleDateFormat sdf = new SimpleDateFormat(formatPattern);
		if ( zone != null ) {
			sdf.setTimeZone(zone);
		}
		return sdf;
	}
	private void addProfileTimeVariable(NetcdfFileWriter ncfile, List<Dimension> dims) {
		addTimeVariable(PROFILE_TIME_VARNAME, "Time", ncfile, dims);
	}
	private static final String PROFILE_LAT_VARNAME = "lat";
	private static final String PROFILE_LON_VARNAME = "lon";
	private static final String PROFILE_TIME_VARNAME = "time";
	private static final String SAMPLE_ID_VARNAME = "sample_id";
//	private static final String SAMPLE_DEPTH_VARNAME = "sample_depth";
//	private static final String SAMPLE_PRESSURE_VARNAME = "sample_pressure";
	private static final String SAMPLE_TIME_VARNAME = "sample_time";
	private static final String SAMPLE_LAT_VARNAME = "sample_latitude";
	private static final String SAMPLE_LON_VARNAME = "sample_longitude";
	private static final String PROFILE_ID_VARNAME = "profile";
	private static final String PROFILE_VARNAME = "profile";
	
	private void addProfileIdVariable(NetcdfFileWriter ncfile, List<Dimension> dims) {
		Variable var = ncfile.addVariable(null, PROFILE_ID_VARNAME, DataType.CHAR, _profileDims.strings());
		ncfile.addVariableAttribute(var, new Attribute("long_name", "Unique identifier for each feature instance."));
		ncfile.addVariableAttribute(var, new Attribute("cf_role", "profile_id"));
	}
	private void addProfileVariable(NetcdfFileWriter ncfile, List<Dimension> dims) {
		Variable var = ncfile.addVariable(null, PROFILE_VARNAME, DataType.CHAR, _profileDims.strings());
		ncfile.addVariableAttribute(var, new Attribute("long_name", "Unique identifier for each feature instance."));
		ncfile.addVariableAttribute(var, new Attribute("cf_role", "profile_id"));
	}
	private void addLatitudeVariable(NetcdfFileWriter ncfile, String varname, String longName, List<Dimension> dims) {
		Variable var = ncfile.addVariable(null, varname, DataType.DOUBLE, dims);
		addAttributes(ncfile, var, DashboardUtils.FP_MISSING_VALUE, longName, "latitude", "Location", "degrees_north");
		ncfile.addVariableAttribute(var, new Attribute("_CoordinateAxisType", "Lat"));
		ncfile.addVariableAttribute(var, new Attribute("axis", "Y"));
	}
	private Variable addLongitudeVariable(NetcdfFileWriter ncfile, String varname, String longName, List<Dimension> dims) {
		Variable var = ncfile.addVariable(null, varname, DataType.DOUBLE, dims);
		addAttributes(ncfile, var, DashboardUtils.FP_MISSING_VALUE, longName, "longitude", "Location", "degrees_east");
		ncfile.addVariableAttribute(var, new Attribute("_CoordinateAxisType", "Lon"));
		ncfile.addVariableAttribute(var, new Attribute("axis", "X"));
		return var;
	}
	private Variable addSampleIdVariable(NetcdfFileWriter ncfile, List<Dimension> dims) {
		Variable var = ncfile.addVariable(null, SAMPLE_ID_VARNAME, DataType.CHAR, dims);
		ncfile.addVariableAttribute(var, new Attribute("long_name", "Unique identifier for each sample."));
		ncfile.addVariableAttribute(var, new Attribute("cf_role", "sample_id"));
		return var;
	}
	private Variable addSampleTimeVariable(NetcdfFileWriter ncfile, List<Dimension> dims) {
		return addTimeVariable(SAMPLE_TIME_VARNAME, "Sample Time", ncfile, dims);
	}
	private Variable addSampleLocationVariables(NetcdfFileWriter ncfile, List<Dimension> dims) {
		addLatitudeVariable(ncfile, SAMPLE_LAT_VARNAME, "Sample Latitude", dims);
		return addLongitudeVariable(ncfile, SAMPLE_LON_VARNAME, "Sample Longitude", dims);
	}
	private Variable addTimeVariable(String stdName, String longName, NetcdfFileWriter ncfile, List<Dimension> dims) {
		Variable var = ncfile.addVariable(null, stdName, DataType.DOUBLE, dims);
		addAttributes(ncfile, var, Double.NaN, longName, stdName, "Time", "seconds since 1970-01-01T00:00:00Z");
		ncfile.addVariableAttribute(var, new Attribute("_CoordinateAxisType", "Time"));
		ncfile.addVariableAttribute(var, new Attribute("axis", "T"));
		ncfile.addVariableAttribute(var, new Attribute("time_origin", "1970-01-01T00:00:00Z"));
		return var;
	}
	private Collection<DashDataType<?>> getDataVariablesToWriteToDsgFile() {
//		String[] varNames = new String[] { // "cast", "cruise", "time", "latitude", "longitude",
//											"date",
//											"ctd_pressure", "sample_depth", "ctd_temperature", 
//											"ctd_salinity", "ctd_oxygen", "ctd_flouride",
//											"bottle_oxygen", "nitrite", "nitrate", "phosphate", "ammonia",
//											"alkalinity", "carbon", "pH" };
		List<DashDataType<?>> dataTypesToWrite = new ArrayList<>(stddata.getDataTypes());
		// TODO: remove excluded columns
		return dataTypesToWrite;
	}

	protected Variable addVariableFor(NetcdfFileWriter ncfile, DashDataType<?> dashType, ElemCategory category) {
		DataType ncDType = dataTypeFor(dashType);
		List<Dimension> dims = dimensionsFor(dashType, category);
		Variable var = addVariable(ncfile, dashType.getVarName(), ncDType, dims);
		addStandardAttributes(ncfile, var, dashType);
		return var;
	}
	protected void addStandardAttributes(NetcdfFileWriter ncfile, Variable var, DashDataType<?> dtype) {
		addAttributes(ncfile, var, null, dtype.getDescription(), 
					  dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
	}
	protected DataType dataTypeFor(DashDataType<?> dashType) {
		String dataTypeName = dashType.getDataClassName().toLowerCase();
		return dataTypeFor(dataTypeName);
	}
	protected DataType dataTypeFor(String dataTypeName) {
		switch (dataTypeName.toLowerCase()) {
			case "double":
				return DataType.DOUBLE;
			case "integer":
				return DataType.INT;
			case "string":
			case "character":
				return DataType.CHAR;
			default:
				throw new IllegalArgumentException("Unknown DashDataType:"+dataTypeName);
		}
	}
	private List<Dimension> dimensionsFor(DashDataType<?> dashType, ElemCategory category) {
		String dataTypeName = dashType.getDataClassName().toLowerCase();
		Dims dims = _dataDims;
		switch (category) {
			case METADATA:
				dims = _metadataDims;
				break;
			case PROFILE:
				dims = _profileDims;
				break;
			case DATA:
				dims = _dataDims;
				break;
		}
		switch (dataTypeName) {
			case "double":
			case "integer":
				return dims.nums();
			case "string":
				return dims.strings();
			case "character":
				return dims.chars();
			default:
				throw new IllegalArgumentException("Unknown DashDataType:"+dataTypeName);
		}
	}
}
