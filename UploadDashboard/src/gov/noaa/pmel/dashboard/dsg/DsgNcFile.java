package gov.noaa.pmel.dashboard.dsg;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.datatype.CharDashDataType;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.DoubleDashDataType;
import gov.noaa.pmel.dashboard.datatype.IntDashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.datatype.StringDashDataType;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;


public abstract class DsgNcFile extends File {

	private static final long serialVersionUID = -7695491814772713480L;

    protected static final Logger logger = LogManager.getLogger(DsgNcFile.class);
    
	protected static final String DSG_VERSION = "DsgNcFile 2.0";
	protected static final String TIME_ORIGIN_ATTRIBUTE = "01-JAN-1970 00:00:00";
    
    // for ragged array-type DSG files
	static final String SAMPLES_PER_FEATURE_VARNAME = "rowcount";
    // FEATURE_*_VARNAME identify feature-instance values
	// SAMPLE_*_VARNAME identify observation-instance value
	// ie, each PROFILE will have a single lat/lon/time.  
	// whereas each PROFILE observation will have a depth (or possibly alternatively a pressure)
	// and MAY also have a sample lat/lon/time. 
	// Each TIMESERIES will have a single lat/lon and possibly depth/pressure,
	// with each observation having a time.
    // A TRAJECTORY will have no feature-type variables other than ID,
	// with each observation varying in lat/lon/time and possibly depth.
    // FEATURE_ID_VARNAME = _featureType.dsgTypeName();
//	static final String FEATURE_ID_VARNAME = "dsg_feature_id";
	public static final String FEATURE_LAT_VARNAME = "latitude"; // "dsg_feature_latitude";
	public static final String FEATURE_LON_VARNAME = "longitude"; //  "dsg_feature_longitude";
	public static final String FEATURE_TIME_VARNAME = "time"; //  "dsg_feature_time";
	public static final String FEATURE_DEPTH_VARNAME = "depth"; // "dsg_feature_depth";
	public static final String FEATURE_PRESSURE_VARNAME = "pressure"; // "dsg_feature_pressure";
	public static final String SAMPLE_ID_VARNAME = "sample_id";
	public static final String SAMPLE_DEPTH_VARNAME = "sample_depth";
	public static final String SAMPLE_PRESSURE_VARNAME = "sample_pressure";
	public static final String SAMPLE_TIME_VARNAME = "sample_time";
	public static final String SAMPLE_LAT_VARNAME = "sample_latitude";
	public static final String SAMPLE_LON_VARNAME = "sample_longitude";

	private static enum ElemCategory {
		METADATA,
		FEATURE,
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
	enum UtcTimeFormat {
		tf_8601Z("yyyy-mm-dd'T'hh:mm:ssZ"),
		tf_8601_compressed("yyyymmdd'T'hhmmssZ");
		
		private String _format;
		private UtcTimeFormat(String formatString) {
			_format = formatString;
		}
		private String pattern() {
			return _format;
		}
        String now() {
            return format(System.currentTimeMillis());
        }
        String format(Date date) {
            return format(date.getTime());
        }
    	String format(long timeMillis) {
    		Date d = new Date(timeMillis);
    		SimpleDateFormat sdf = getDateFormatter(pattern(), TimeZone.getTimeZone("UTC"));
    		return sdf.format(d);
    	}
    	private static SimpleDateFormat getDateFormatter(String formatPattern, TimeZone zone) {
    		SimpleDateFormat sdf = new SimpleDateFormat(formatPattern);
    		if ( zone != null ) {
    			sdf.setTimeZone(zone);
    		}
    		return sdf;
    	}
	}
	
    protected Map<String, Variable> _typeSpecificVariables = new LinkedHashMap<>();
    protected FeatureType _featureType;
	protected DsgMetadata _metadata;
	protected StdUserDataArray _stdUser;
    protected String _datasetId;
	protected int _maxStrLen;
    protected int _numObservations;

	private Dimension _dimNumFeatures;
	private Dimension _dimNumObs;
	private Dimension _dimOfOne;
	List<Dimension> _metaDimList = new ArrayList<Dimension>();
	List<Dimension> _metaCharDimList = new ArrayList<Dimension>();
	List<Dimension> _metaStringDimList = new ArrayList<Dimension>();
	List<Dimension> _featureDimList = new ArrayList<Dimension>();
	List<Dimension> _featureStringDimList = new ArrayList<Dimension>();
	List<Dimension> _featureCharDimList = new ArrayList<Dimension>();
	List<Dimension> _obsDimList = new ArrayList<Dimension>();
	List<Dimension> _obsCharDimList = new ArrayList<Dimension>();
	List<Dimension> _obsStringDimList = new ArrayList<>();
	Dims _metadataDims;
	Dims _featureDims;
	Dims _obsDims;
    
	protected abstract int getNumFeatures();
	protected abstract void doFeatureTypeSpecificInitialization();
	protected abstract void checkFeatureTypeSpecificIndeces();
//	protected abstract void createTypeSpecificDimensions(NetcdfFileWriter ncFile);
	protected abstract void addFeatureTypeVariables(NetcdfFileWriter ncFile);
//	protected abstract void addObservationVariables(NetcdfFileWriter ncFile);
//	protected abstract boolean excludeType(DashDataType<?> dtype);
    
//	protected abstract void addQcVariables(NetcdfFileWriter ncFile, KnownDataTypes dataTypes);
    
	protected abstract void writeFeatureTypeVariables(NetcdfFileWriter ncFile) throws Exception;
//	protected abstract void writeObservationVariables(NetcdfFileWriter ncFile) throws Exception;
//	protected abstract void writeQcVariables(NetcdfFileWriter ncFile) throws Exception;
	
    
    public static DsgNcFile newDsgFile(FeatureType type, String filename) {
        return newDsgFile(type, null, filename);
    }
    public static DsgNcFile newDsgFile(FeatureType type, File parent, String filename) {
        switch (type) {
            case PROFILE:
                return new ProfileDsgFile(parent, filename);
            case TIMESERIES:
                return new TimeseriesDsgFile(parent, filename);
            case TIMESERIES_PROFILE:
                throw new IllegalStateException("Feature type " + FeatureType.TIMESERIES_PROFILE + ": Not yet implemented.");
            case TRAJECTORY:
                return new TrajectoryDsgFile(parent, filename);
            case TRAJECTORY_PROFILE:
                throw new IllegalStateException("Feature type " + FeatureType.TRAJECTORY_PROFILE + ": Not yet implemented.");
            case UNSPECIFIED:
                throw new IllegalArgumentException("Cannot create DSG files for feature type: " + type);
            case OTHER:
                throw new IllegalArgumentException("Cannot create DSG files for feature type: " + type);
            default:
                throw new IllegalArgumentException("Unknown FeatureType:"+type);
        }
    }
    
	/**
	 * See {@link java.io.File#File(java.lang.String)}
	 * The internal metadata and data array references are set null.
	 */
	protected DsgNcFile(FeatureType type, String filename) {
		this(type, null, filename);
	}

	/**
	 * See {@link java.io.File#File(java.io.File,java.lang.String)}
	 * The internal metadata and data array references are set null.
	 */
	protected DsgNcFile(FeatureType type, File parent, String child) {
		super(parent, child);
        _typeSpecificVariables = new LinkedHashMap<>();
		_metadata = null;
		_stdUser = null;
        _featureType = type;
	}

	/**
	 * Creates this NetCDF DSG file with the given metadata and standardized user 
	 * provided data.  The internal metadata reference is updated to the given 
	 * DsgMetadata object and the internal data array reference is updated to a new 
	 * standardized data array object created from the appropriate user provided data. 
	 * Every data sample must have a valid longitude, latitude, sample depth, and 
	 * complete date and time specification, to at least the minute.  If the seconds 
	 * of the time is not provided, zero seconds will be used.
	 * 
	 * @param metaData
	 * 		metadata for the dataset
	 * @param userStdData
	 * 		standardized user-provided data
	 * @param dataFileTypes
	 * 		known data types for data files
	 * @throws IllegalArgumentException
	 * 		if any argument is null,
	 * 		if any of the data types in userStdData is {@link DashboardServerUtils#UNKNOWN}
	 * 		if any sample longitude, latitude, sample depth is missing,
	 * 		if any sample time cannot be computed
	 * @throws IOException
	 * 		if creating the NetCDF file throws one
	 * @throws InvalidRangeException
	 * 		if creating the NetCDF file throws one
	 * @throws IllegalAccessException
	 * 		if creating the NetCDF file throws one
	 */
	public void create(DsgMetadata metaData, StdUserDataArray fileData, KnownDataTypes dataTypes) 
			throws Exception, IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException {
		if ( metaData == null )
			throw new IllegalArgumentException("no metadata given");
		_metadata = metaData;
		_datasetId = _metadata.getDatasetId();
		if ( fileData == null )
			throw new IllegalArgumentException("no data given");
		_stdUser = fileData;
		
		initialize();
		checkIndeces();
		
		try ( NetcdfFileWriter ncFile = NetcdfFileWriter.createNew(Version.netcdf3, getPath()); ) {
			
			createDimensions(ncFile);
			
			addAttributes(ncFile);
			
            addVariables(ncFile);

			ncFile.create();

			// The header has been created.  Now let's fill it up.
			
            writeVariables(ncFile);
            ncFile.flush();
		}
	}
	
    protected void initialize() {
        int metaMax = _metadata.getMaxStringLength();
        int dataMax = findLongestDataString();
        _maxStrLen = Math.max(metaMax, dataMax);
		_numObservations = _stdUser.getNumSamples();
        doFeatureTypeSpecificInitialization();
    }
	protected void checkIndeces() {
        checkCommonIndeces();
        checkFeatureTypeSpecificIndeces();
	}
	protected void checkCommonIndeces() {
		// Quick check of data column indices already assigned in stdUser
		if ( ! _stdUser.hasLatitude() )
			throw new IllegalArgumentException("no latitude data column");
		if ( ! _stdUser.hasLongitude() )
			throw new IllegalArgumentException("no longitude data column");
        if ( ! _stdUser.hasSampleTime()) 
			throw new IllegalArgumentException("incomplete or missing sample time");
//		if ( ! stdUser.hasLongitude() )
//			throw new IllegalArgumentException("no longitude data column");
//		if ( ! stdUser.hasLatitude() )
//			throw new IllegalArgumentException("no latitude data column");
//		if ( ! ( stdUser.hasSampleDepth() || stdUser.hasSamplePressure() ))
//			throw new IllegalArgumentException("no sample depth data column");
//		if ( ! stdUser.hasYear() )
//			throw new IllegalArgumentException("no year data column");
//		if ( ! stdUser.hasMonthOfYear() )
//			throw new IllegalArgumentException("no month of year data column");
//		if ( ! stdUser.hasDayOfMonth() )
//			throw new IllegalArgumentException("no day of month data column");
//		if ( ! stdUser.hasHourOfDay() )
//			throw new IllegalArgumentException("no hour of day data column");
//		if ( ! stdUser.hasMinuteOfHour() )
//			throw new IllegalArgumentException("no minute of hour data column");
//		if ( ! stdUser.hasSecondOfMinute() )
//			throw new IllegalArgumentException("no second of minute data column");
	}
	
    /**
     * @return the length of the longest value in String-type data
     */
    private int findLongestDataString() {
        int max = 0;
        for (int col=0; col<_stdUser.numDataCols; col++) {
            if ( _stdUser.getDataTypes().get(col) instanceof StringDashDataType
                    && _stdUser.isStandardized(col)) {
                String[] values = (String[])_stdUser.getStdValuesAsString(col);
                for (int row=0; row<_stdUser.getNumSamples(); row++) {
                    int len = values[row] != null ? values[row].trim().length() : 0;
                    if ( len > max ) { max = len; }
                }
            }
        }
        return max;
    }
	
	protected void createDimensions(NetcdfFileWriter ncFile) {
//	    createCommonDimensions(ncFile);
//	    createTypeSpecificDimensions(ncFile);
//	}
//	protected void createCommonDimensions(NetcdfFileWriter ncFile) {
		Dimension dStringLen = ncFile.addDimension(null, "string_length", _maxStrLen);
		Dimension dCharLen = ncFile.addDimension(null, "char_length", 1);
		
		_dimOfOne = ncFile.addDimension(null, "one", 1);
		_metaDimList.add(_dimOfOne);
//		_metaCharDimList.add(_one);
		_metaCharDimList.add(dCharLen);
//		_metaStringDimList.add(_one);
		_metaStringDimList.add(dStringLen);
		_metadataDims = new Dims(_metaDimList, _metaCharDimList, _metaStringDimList);
		
		_dimNumFeatures = ncFile.addDimension(null, _featureType.dsgTypeName(), getNumFeatures());
		_featureDimList.add(_dimNumFeatures);

		_featureStringDimList.add(_dimNumFeatures);
		_featureStringDimList.add(dStringLen);

		_featureCharDimList.add(_dimNumFeatures);
		_featureCharDimList.add(dCharLen);
		_featureDims = new Dims(_featureDimList, _featureCharDimList, _featureStringDimList);

		_dimNumObs = ncFile.addDimension(null, "obs", _numObservations);
		_obsDimList.add(_dimNumObs);

		_obsCharDimList.add(_dimNumObs);
		_obsCharDimList.add(dCharLen);
		
		_obsStringDimList.add(_dimNumObs);
		_obsStringDimList.add(dStringLen);
		_obsDims = new Dims(_obsDimList, _obsCharDimList, _obsStringDimList);
	}
	
	protected void addAttributes(NetcdfFileWriter ncFile) {
        addCommonAttributes(ncFile);
		addMetadataAttributes(ncFile);
        addDsgTypeSpecificAttributes(ncFile);
	}
	protected void addCommonAttributes(NetcdfFileWriter ncFile) {
		ncFile.addGroupAttribute(null, new Attribute("cdm_data_type", _featureType.dsgTypeName()));
		ncFile.addGroupAttribute(null, new Attribute("featureType", _featureType.dsgTypeName()));
		ncFile.addGroupAttribute(null, new Attribute("Conventions", "CF-1.6,COARDS,ACDD-1.3"));
		ncFile.addGroupAttribute(null, new Attribute("COORD_SYSTEM", "Geographical"));
		ncFile.addGroupAttribute(null, new Attribute("creation_date", UtcTimeFormat.tf_8601Z.now()));
		ncFile.addGroupAttribute(null, new Attribute("history", DSG_VERSION));
		ncFile.addGroupAttribute(null, new Attribute("id", _stdUser.getDatasetName()));
		ncFile.addGroupAttribute(null, new Attribute("dataset_id", _stdUser.getDatasetName()));
	}
	protected void addMetadataAttributes(NetcdfFileWriter ncFile) {
		String varName;
		for ( Entry<DashDataType<?>,Object> entry : _metadata.getValuesMap().entrySet() ) {
			DashDataType<?> dtype = entry.getKey();
			Object value = entry.getValue();
			varName = dtype.getVarName();
			logger.debug("metadata var as attribute:"+varName + ": " + value);
			if ( DashboardUtils.isNullOrNull(value)) { continue; }
			ncFile.addGroupAttribute(null, new Attribute(getAttributeNameFor(dtype), String.valueOf(value)));
		}
	}
	protected void addDsgTypeSpecificAttributes(NetcdfFileWriter ncFile) {
	    // default empty
	}
	protected static String getAttributeNameFor(DashDataType<?> dtype) {
		String name = dtype.getStandardName();
		if ( name == null || DashboardUtils.STRING_MISSING_VALUE.equals(name)) {
			name = dtype.getVarName();
		}
		return name;
	}
    protected void addVariables(NetcdfFileWriter ncFile) {
//		addMetadataVariables(ncFile);
        addCommonVariables(ncFile);
		addFeatureTypeVariables(ncFile);
		addObservationVariables(ncFile);
//		addQcVariables(ncFile, dataTypes);
    }        
    private void addCommonVariables(NetcdfFileWriter ncFile) {
		addFeatureIdVariable(ncFile, _featureDimList);
		Variable var = ncFile.addVariable(null, SAMPLES_PER_FEATURE_VARNAME, DataType.INT, _featureDimList);
		ncFile.addVariableAttribute(var, new Attribute("sample_dimension", "obs"));
		ncFile.addVariableAttribute(var, new Attribute("long_name", "Number of Observations in this Feature"));
    }
    private Variable addFeatureIdVariable(NetcdfFileWriter ncFile, List<Dimension> dims) {
		Variable var = ncFile.addVariable(null, _featureType.dsgTypeName(), DataType.CHAR, _featureDims.strings());
		ncFile.addVariableAttribute(var, new Attribute("long_name", "Unique identifier for each feature instance."));
		ncFile.addVariableAttribute(var, new Attribute("cf_role", _featureType.dsgTypeName()+"_id"));
		ncFile.addVariableAttribute(var, new Attribute("ioos_category", "Identifier"));
        return var;
	}
	protected static Variable addTimeVariable(NetcdfFileWriter ncFile, String stdName, String longName, 
        	                                  List<Dimension> dims, boolean isDsgAxis) {
		Variable var = ncFile.addVariable(null, stdName, DataType.DOUBLE, dims);
		addAttributes(ncFile, var, Double.NaN, longName, stdName, "Time", "seconds since 1970-01-01T00:00:00Z");
		ncFile.addVariableAttribute(var, new Attribute("_CoordinateAxisType", "Time"));
		ncFile.addVariableAttribute(var, new Attribute("time_origin", "1970-01-01T00:00:00Z"));
        if ( isDsgAxis ) {
    		ncFile.addVariableAttribute(var, new Attribute("axis", "T"));
        }
		return var;
	}
	protected static Variable addLatitudeVariable(NetcdfFileWriter ncFile, String varname, String longName, 
            	                                  List<Dimension> dims, boolean isDsgAxis) {
		Variable var = ncFile.addVariable(null, varname, DataType.DOUBLE, dims);
		addAttributes(ncFile, var, DashboardUtils.FP_MISSING_VALUE, longName, "latitude", "Location", "degrees_north");
		ncFile.addVariableAttribute(var, new Attribute("_CoordinateAxisType", "Lat"));
        if ( isDsgAxis ) {
    		ncFile.addVariableAttribute(var, new Attribute("axis", "Y"));
        }
        return var;
	}
	protected static Variable addLongitudeVariable(NetcdfFileWriter ncFile, String varname, String longName, 
    	                                           List<Dimension> dims, boolean isDsgAxis) {
		Variable var = ncFile.addVariable(null, varname, DataType.DOUBLE, dims);
		addAttributes(ncFile, var, DashboardUtils.FP_MISSING_VALUE, longName, "longitude", "Location", "degrees_east");
		ncFile.addVariableAttribute(var, new Attribute("_CoordinateAxisType", "Lon"));
        if ( isDsgAxis ) {
    		ncFile.addVariableAttribute(var, new Attribute("axis", "X"));
        }
		return var;
	}
	protected static Variable addDepthVariable(NetcdfFileWriter ncFile, String varname, String longName, 
        	                                   List<Dimension> dims, boolean isDsgAxis) {
		Variable var = ncFile.addVariable(null, varname, DataType.DOUBLE, dims);
		addAttributes(ncFile, var, DashboardUtils.FP_MISSING_VALUE, longName, "depth", "Location", "meters");
		ncFile.addVariableAttribute(var, new Attribute("_CoordinateAxisType", "Depth"));
		ncFile.addVariableAttribute(var, new Attribute("positive", "down"));
        if ( isDsgAxis ) {
    		ncFile.addVariableAttribute(var, new Attribute("axis", "Z"));
        }
		return var;
	}
	protected static Variable addPressureVariable(NetcdfFileWriter ncFile, String varname, String longName, 
                                                  List<Dimension> dims, boolean isDsgAxis) {
		Variable var = ncFile.addVariable(null, varname, DataType.DOUBLE, dims);
		addAttributes(ncFile, var, DashboardUtils.FP_MISSING_VALUE, longName, "pressure", "Location", "decibars");
		ncFile.addVariableAttribute(var, new Attribute("_CoordinateAxisType", "Pressure"));
		ncFile.addVariableAttribute(var, new Attribute("positive", "down"));
        if ( isDsgAxis ) {
    		ncFile.addVariableAttribute(var, new Attribute("axis", "Z"));
        }
		return var;
	}
	
	/**
     * NOT IMPLEMENTED
	 * Creates this NetCDF DSG file with the given metadata and standardized data
	 * for data files.  The internal metadata and stddata references are updated 
	 * to the given DsgMetadata and StdDataArray object.  Every data sample should 
	 * have a valid longitude, latitude, sample depth, year, month of year, day of 
	 * month, hour of day, minute of hour, second of minute, time, sample number, 
	 * and WOCE autocheck value, although this is not fully verified.
	 * 
	 * @param metaData
	 * 		metadata for the dataset
	 * @param fileData
	 * 		standardized data appropriate for data files
	 * @throws IllegalArgumentException
	 * 		if any argument is null, or
	 * 		if there is no longitude, latitude, sample depth, year, month of year,
	 * 			day of month, hour of day, minute of hour, or second of minute, or 
	 * 			time data column
	 * @throws IOException
	 * 		if creating the NetCDF file throws one
	 * @throws InvalidRangeException
	 * 		if creating the NetCDF file throws one
	 * @throws IllegalAccessException
	 * 		if creating the NetCDF file throws one
	 *
     */
	public void create(DsgMetadata metaData, StdDataArray fileData) 
		throws Exception, IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException  {
        throw new IllegalStateException("Not implemented"); // XXX TODO: Do we need this? Or do we need the other create?
	}
	
	/**
	 * Adds the missing_value, _FillValue, long_name, standard_name, ioos_category, 
	 * and units attributes to the given variables in the given NetCDF file.
	 * 
	 * @param ncFile
	 * 		NetCDF file being written containing the variable
	 * @param var
	 * 		the variables to add attributes to
	 * @param missVal
	 * 		if not null, the value for the missing_value and _FillValue attributes
	 * @param longName
	 * 		if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, 
	 * 		the value for the long_name attribute
	 * @param standardName
	 * 		if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, 
	 * 		the value for the standard_name attribute
	 * @param ioosCategory
	 * 		if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, 
	 * 		the value for the ioos_category attribute
	 * @param units
	 * 		if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, 
	 * 		the value for the units attribute
	 */
	protected static void addAttributes(NetcdfFileWriter ncFile, Variable var, Object missVal, 
			String longName, String standardName, String ioosCategory, String units) {
        if ( var == null ) {
            logger.warn("Add attributes: No ncVar found for varName " + longName);
            return;
        }
		if ( missVal != null ) {
			ncFile.addVariableAttribute(var, new Attribute("missing_value", String.valueOf(missVal)));
			ncFile.addVariableAttribute(var, new Attribute("_FillValue", String.valueOf(missVal)));
		}
		if ( (longName != null) && ! DashboardUtils.STRING_MISSING_VALUE.equals(longName) ) {
			ncFile.addVariableAttribute(var, new Attribute("long_name", longName));
		}
		if ( (standardName != null) && ! DashboardUtils.STRING_MISSING_VALUE.equals(standardName) ) {
			ncFile.addVariableAttribute(var, new Attribute("standard_name", standardName));
		}
		if ( (ioosCategory != null) && ! DashboardUtils.STRING_MISSING_VALUE.equals(ioosCategory) ) {
			ncFile.addVariableAttribute(var, new Attribute("ioos_category", ioosCategory));
		}
		if ( (units != null) && ! DashboardUtils.STRING_MISSING_VALUE.equals(units) ) {
			ncFile.addVariableAttribute(var, new Attribute("units", units));
		}
	}
    
	protected Variable addVariable(NetcdfFileWriter ncFile, String varName, DataType dtype, List<Dimension> dataDims) {
		return addVariable(ncFile, null, varName, dtype, dataDims);
	}
	protected Variable addVariable(NetcdfFileWriter ncFile, Group group, String varName, DataType dtype, List<Dimension> dataDims) {
		Variable var = ncFile.addVariable(group, varName, dtype, dataDims);
		if ( var == null ) { 
			String msg = "Failed to add NetCDF Variable for " + varName +".";
			var = ncFile.findVariable(varName);
			if ( var != null ) {
				msg += " Variable already exists with dimensions " + var.getDimensions();
			}
			throw new IllegalStateException(msg);
		}
		return var;
	}
	protected Variable getVariable(NetcdfFileWriter ncFile, String varName) {
		Variable var = ncFile.findVariable(varName);
		if ( var == null ) {
			throw new RuntimeException("Unexpected failure to find ncFile variable " + varName);
		}
		return var;
	}
	
	/**
	 * Creates and assigns the internal metadata 
	 * reference from the contents of this netCDF DSG file.
	 * 
	 * @param metadataTypes
	 * 		metadata file types to read
	 * @return
	 * 		variable names of the metadata fields not assigned from 
	 * 		this netCDF file (will have its default/missing value)
	 * @throws IllegalArgumentException
	 * 		if there are no metadata types given, or
	 * 		if an invalid type for metadata is encountered
	 * @throws IOException
	 * 		if there are problems opening or reading from the netCDF file
	 */
	public ArrayList<String> readMetadata(KnownDataTypes metadataTypes) 
			throws IllegalArgumentException, IOException{
		if ( (metadataTypes == null) || metadataTypes.isEmpty() )
			throw new IllegalArgumentException("no metadata file types given");
		ArrayList<String> namesNotFound = new ArrayList<String>();
		
		try ( NetcdfFile ncFile = NetcdfFile.open(getPath()); ) {
			// Create the metadata with default (missing) values
			_metadata = new DsgMetadata(metadataTypes);

			for ( DashDataType<?> dtype : metadataTypes.getKnownTypesSet() ) {
				String varName = dtype.getVarName();
				Variable var = ncFile.findVariable(varName);
				if ( var == null ) {
					namesNotFound.add(varName);
					continue;
				}
				if ( var.getShape(0) != 1 ) 
					throw new IOException("more than one value for a metadata type");
				if ( dtype instanceof StringDashDataType ) {
					ArrayChar.D2 mvar = (ArrayChar.D2) var.read();
					String strval = mvar.getString(0);
					if ( ! DashboardUtils.STRING_MISSING_VALUE.equals(strval) )
						_metadata.setValue(dtype, strval);
				}
				else if ( dtype instanceof CharDashDataType ) {
					if ( var.getShape(1) != 1 )
						throw new IOException("more than one character for a character type");
					ArrayChar.D2 mvar = (ArrayChar.D2) var.read();
					Character charval = mvar.get(0, 0);
					if ( ! DashboardUtils.CHAR_MISSING_VALUE.equals(charval) )
						_metadata.setValue(dtype, charval);
				}
				else if ( dtype instanceof IntDashDataType ) {
					ArrayInt.D1 mvar = (ArrayInt.D1) var.read();
					Integer intval = mvar.getInt(0);
					if ( ! DashboardUtils.INT_MISSING_VALUE.equals(intval) )
						_metadata.setValue(dtype, intval);
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					ArrayDouble.D1 mvar = (ArrayDouble.D1) var.read();
					Double dblval = mvar.getDouble(0);
					if ( ! DashboardUtils.closeTo(DashboardUtils.FP_MISSING_VALUE, dblval, 
							DashboardUtils.MAX_RELATIVE_ERROR, DashboardUtils.MAX_ABSOLUTE_ERROR) )
						_metadata.setValue(dtype, dblval);
				}
				else {
					throw new IllegalArgumentException("invalid metadata file type " + dtype.getVarName());
				}
			}
		}
		return namesNotFound;
	}

	/**
     * NOT IMPLEMENTED
     * 
	 * Creates and assigns the internal standard data array 
	 * reference from the contents of this netCDF DSG file.
	 * 
	 * @param dataTypes
	 * 		data files types to read
	 * @return
	 * 		variable names of the data types not assigned from 
	 * 		this netCDF file (will have its default/missing value)
	 * @throws IllegalArgumentException
	 * 		if no known data types are given, or
	 * 		if an invalid type for data files is encountered
	 * @throws IOException
	 * 		if the netCDF file is invalid: it must have a 'time' 
	 * 		variable and all data variables must have the same
	 * 		number of values as the 'time' variable, or
	 * 		if there are problems opening or reading from the netCDF file
	 */
	public ArrayList<String> readData(KnownDataTypes dataTypes) 
			throws IllegalArgumentException, IOException {
        throw new RuntimeException("Not implemented");
        /*
		if ( (dataTypes == null) || dataTypes.isEmpty() )
			throw new IllegalArgumentException("no data file types given");
		int numColumns;
		DashDataType<?>[] dataTypesArray;
		{
			TreeSet<DashDataType<?>> dataTypesSet = dataTypes.getKnownTypesSet();
			numColumns = dataTypesSet.size();
			dataTypesArray = new DashDataType<?>[numColumns];
			int idx = -1;
			for ( DashDataType<?> dtype : dataTypesSet ) {
				idx++;
				dataTypesArray[idx] = dtype;
			}
		}

		ArrayList<String> namesNotFound = new ArrayList<String>();
		
		try ( NetcdfFile ncFile = NetcdfFile.open(getPath()); ) {
			// Get the number of samples from the length of the time 1D array
			String varName = DashboardServerUtils.TIME.getVarName();
			Variable var = ncFile.findVariable(varName);
			if ( var == null )
				throw new IOException("unable to find variable 'time' in " + getName());
			int numSamples = var.getShape(0);

			// Create the array of data values
			Object[][] dataArray = new Object[numSamples][numColumns];

			for (int k = 0; k < numColumns; k++) {
				DashDataType<?> dtype = dataTypesArray[k];
				varName = dtype.getVarName();
				var = ncFile.findVariable(varName);
				if ( var == null ) {
					namesNotFound.add(varName);
					for (int j = 0; j < numSamples; j++)
						dataArray[j][k] = null;
					continue;
				}

				if ( var.getShape(0) != numSamples )
					throw new IOException("number of values for '" + varName + 
							"' (" + Integer.toString(var.getShape(0)) + ") does not match " +
							"the number of values for 'time' (" + Integer.toString(numSamples) + ")");

				if ( dtype instanceof CharDashDataType ) {
					if ( var.getShape(1) != 1 )
						throw new IOException("more than one character for a character type");
					ArrayChar.D2 dvar = (ArrayChar.D2) var.read();
					for (int j = 0; j < numSamples; j++) {
						Character charval = dvar.get(j,0);
						if ( DashboardUtils.CHAR_MISSING_VALUE.equals(charval) )
							dataArray[j][k] = null;
						else
							dataArray[j][k] = charval;
					}
				}
				else if ( dtype instanceof IntDashDataType ) {
					ArrayInt.D1 dvar = (ArrayInt.D1) var.read();
					for (int j = 0; j < numSamples; j++) {
						Integer intval = dvar.get(j);
						if ( DashboardUtils.INT_MISSING_VALUE.equals(intval) )
							dataArray[j][k] = null;
						else
							dataArray[j][k] = intval;
					}
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					ArrayDouble.D1 dvar = (ArrayDouble.D1) var.read();
					for (int j = 0; j < numSamples; j++) {
						Double dblval = dvar.get(j);
						if ( DashboardUtils.closeTo(DashboardUtils.FP_MISSING_VALUE, dblval, 
								DashboardUtils.MAX_RELATIVE_ERROR, DashboardUtils.MAX_ABSOLUTE_ERROR) )
							dataArray[j][k] = null;
						else
							dataArray[j][k] = dblval;
					}
				}
				else {
					throw new IllegalArgumentException("invalid data file type " + dtype.toString());
				}
			}
			// _stdUser = new StdUserDataArray(dataTypesArray, dataArray); // Old way
			_stdUser = new StdUserDataArray(dataTypesArray, dataArray);
		}
		return namesNotFound;
    */
	}

	/**
	 * @return
	 * 		the internal metadata reference; may be null
	 */
	public DsgMetadata getMetadata() {
		return _metadata;
	}

	/**
	 * @return
	 * 		the internal standard data array reference; may be null
	 */
	public StdDataArray getStdDataArray() {
		return _stdUser;
	}

	/**
	 * Reads and returns the array of data values for the specified variable 
	 * contained in this DSG file.  The variable must be saved in the DSG 
	 * file as characters.  For some variables, this DSG file must have been 
	 * processed by Ferret for the data values to be meaningful.
	 * 
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid, or
	 * 		if the variable is not a single-character array variable
	 */
	public char[] readCharVarDataValues(String varName) 
								throws IOException, IllegalArgumentException {
		char[] dataVals;
		
		try ( NetcdfFile ncFile = NetcdfFile.open(getPath()); ) {
			Variable var = ncFile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayChar.D2 cvar = (ArrayChar.D2) var.read();
			if ( var.getShape(1) != 1 ) 
				throw new IllegalArgumentException("Variable '" + varName + 
						"' is not a single-character array variable in " + getName());
			int numVals = var.getShape(0);
			dataVals = new char[numVals];
			for (int k = 0; k < numVals; k++)
				dataVals[k] = cvar.get(k,0);
		}
		return dataVals;
	}

	/**
	 * Reads and returns the array of data values for the specified variable
	 * contained in this DSG file.  The variable must be saved in the DSG file
	 * as integers.  For some variables, this DSG file must have been processed 
	 * by Ferret for the data values to be meaningful.
	 * 
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid
	 */
	public int[] readIntVarDataValues(String varName) 
								throws IOException, IllegalArgumentException {
		int[] dataVals;
		
		try ( NetcdfFile ncFile = NetcdfFile.open(getPath()); ) {
			Variable var = ncFile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayInt.D1 dvar = (ArrayInt.D1) var.read();
			int numVals = var.getShape(0);
			dataVals = new int[numVals];
			for (int k = 0; k < numVals; k++)
				dataVals[k] = dvar.get(k);
		}
		return dataVals;
	}

	/**
	 * Reads and returns the array of data values for the specified variable contained 
	 * in this DSG file.  The variable must be saved in the DSG file as doubles.  
	 * NaN and infinite values are changed to {@link DsgData#FP_MISSING_VALUE}.  
	 * For some variables, this DSG file must have been processed by Ferret for the data 
	 * values to be meaningful.
	 * 
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid
	 */
	public double[] readDoubleVarDataValues(String varName) 
								throws IOException, IllegalArgumentException {
		double[] dataVals;
		
		try ( NetcdfFile ncFile = NetcdfFile.open(getPath()); ) {
			Variable var = ncFile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayDouble.D1 dvar = (ArrayDouble.D1) var.read();
			int numVals = var.getShape(0);
			dataVals = new double[numVals];
			for (int k = 0; k < numVals; k++) {
				double value = dvar.get(k);
				if ( Double.isNaN(value) || Double.isInfinite(value) )
					value = DashboardUtils.FP_MISSING_VALUE;
				dataVals[k] = value;
			}
		}
		return dataVals;
	}

	/**
	 * Updates the string recorded for the given variable in this DSG file.
	 * 
	 * @param varName
	 * 		name of the variable in this DSG file
	 * @param newValue
	 * 		new string value to record in this DSG file
	 * @throws IllegalArgumentException
	 * 		if this DSG file is not valid
	 * @throws IOException
	 * 		if opening or updating this DSG file throws one
	 * @throws InvalidRangeException 
	 * 		if writing the updated string to this DSG file throws one 
	 * 		or if the updated string is too long for this DSG file
	 */
	public void updateStringVarValue(String varName, String newValue) 
		throws IllegalArgumentException, IOException, InvalidRangeException {
		
		try ( NetcdfFileWriter ncFile = NetcdfFileWriter.openExisting(getPath()); ) {
			Variable var = ncFile.findVariable(varName);
			if ( var == null ) 
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			int varLen = var.getShape(1);
			if ( newValue.length() > varLen )
				throw new InvalidRangeException("Length of new string (" + 
						newValue.length() + ") exceeds available space (" + 
						varLen + ")");
			ArrayChar.D2 valArray = new ArrayChar.D2(1, varLen);
			valArray.setString(0, newValue);
			ncFile.write(var, valArray);
		}
	}

	/*  *
	 * Writes the given array of characters as the values 
	 * for the given character data variable.
	 * 
	 * @param varName
	 * 		character data variable name
	 * @param values
	 * 		character values to assign
	 * @throws IOException
	 * 		if reading from or writing to the file throws one
	 * @throws IllegalArgumentException
	 * 		if the variable name or number of provided values
	 * 		is invalid
	public void writeCharVarDataValues(String varName, char[] values) 
								throws IOException, IllegalArgumentException {
		
		try ( NetcdfFileWriter ncFile = NetcdfFileWriter.openExisting(getPath()); ) {
			Variable var = ncFile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			if ( var.getShape(1) != 1 ) 
				throw new IllegalArgumentException("Variable '" + varName + 
						"' is not a single-character array variable in " + getName());
			int numVals = var.getShape(0);
			if ( numVals != values.length )
				throw new IllegalArgumentException("Inconstistent number of variables for '" + 
						varName + "' (" + Integer.toString(numVals) + 
						") and provided data (" + Integer.toString(values.length) + ")");
			ArrayChar.D2 dvar = new ArrayChar.D2(numVals, 1);
			for (int k = 0; k < numVals; k++) {
				dvar.set(k, 0, values[k]);
			}
			try {
				ncFile.write(var, dvar);
			} catch (InvalidRangeException ex) {
				throw new IllegalArgumentException(ex);
			}
		}
	}
	 */

	protected void addObservationVariables(NetcdfFileWriter ncFile) {
        for (int col=0; col<_stdUser.getNumDataCols(); col++) {
            DashDataType<?> dtype = _stdUser.getDataTypes().get(col);
            if ( ! (dtype instanceof DoubleDashDataType)) {
                logger.debug("Skipping non-double column " + dtype.getVarName() + " at index " + col);
                continue;
            }
            if ( ! _stdUser.isStandardized(col)) {
                logger.debug("Skipping non-standardized column " + dtype.getVarName() + " at index " + col);
                continue;
            }
			if ( excludeColumn(col, dtype)) {
				logger.debug("Not adding variable for excluded column: " + dtype);
				continue;
			}
			DashDataType<?> dataCol =  _stdUser.findDataColumn(dtype.getVarName());
			if ( dataCol == null ) {
				logger.warn("No data column found for type: " + dtype.getVarName());
				continue;
			}
			Variable var = addVariableFor(ncFile, dtype, ElemCategory.DATA);
			logger.debug("Added data variable " + var);
		}
	}
    
	protected void writeVariables(NetcdfFileWriter ncFile) throws Exception {
//		writeMetadataVariables(ncFile);
		writeFeatureTypeVariables(ncFile);
		writeObservationVariables(ncFile);
	}
	protected void writeObservationVariables(NetcdfFileWriter ncFile) throws Exception {
//		Collection<DashDataType<?>> dsgVariableTypes = getDataVariablesToWriteToDsgFile();
//		for (DashDataType<?> dtype : dsgVariableTypes) {
        for (int col=0; col<_stdUser.getNumDataCols(); col++) {
            DashDataType<?> dtype = _stdUser.getDataTypes().get(col);
			if ( excludeColumn(col, dtype)) {
				logger.debug("Not adding data for excluded column: " + dtype);
				continue;
			}
			String varName = dtype.getVarName();
			Variable var = ncFile.findVariable(varName);
			if ( var == null ) {
				logger.info(_datasetId + ": DsgFile: Did not find Variable for " + varName);
				continue;
			}
			int dataColumnIdx = _stdUser.findDataColumnIndex(varName);
			if ( dataColumnIdx == -1 ) {
				logger.warn(_datasetId + ": Did not find data column for " + varName);
				continue;
			}
            if ( _stdUser.isStandardized(dataColumnIdx)) {
    			Object[] data = _stdUser.getStdValues(dataColumnIdx);
    			writeVariableData(ncFile, var, dtype, data);
            } else {
                Object[] data = _stdUser.getAllValuesAsString(dataColumnIdx);
    			writeVariableData(ncFile, var, dtype, data);
            }
		}
	}
	private void writeVariableData(NetcdfFileWriter ncFile, Variable var, DashDataType<?> dtype, Object[] data) 
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
			ncFile.write(var, dvar);
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
			ncFile.write(var, dvar);
		}
		else if ( dtype instanceof IntDashDataType ) {
			ArrayInt.D1 dvar = new ArrayInt.D1(numSamples);
			for (int j = 0; j < numSamples; j++) {
				Integer dvalue = (Integer) data[j];
				if ( dvalue == null )
					dvalue = DashboardUtils.INT_MISSING_VALUE;
				dvar.set(j, dvalue.intValue());
			}
			ncFile.write(var, dvar);
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
			ncFile.write(var, dvar);
		}
		else {
			throw new IllegalArgumentException("unexpected unknown data type: " + dtype.toString());
		}
		ncFile.flush();
	}
	
    protected boolean excludeColumn(int columnIndex, DashDataType<?> dtype) {
        if ( dtype.getVarName().equals("other") ||
             dtype.typeNameLike("unknown") ||
             _typeSpecificVariables.containsKey(dtype.getVarName())) {
            return true;
        }
        return false;
    }
//    protected boolean excludeColumnFromData(int columnIndex, DashDataType<?> dtype) {
//        if ( dtype.getVarName().equals("other") ||
//             dtype.typeNameLike("unknown") ||
//             _typeSpecificVariables.containsKey(dtype.getVarName())) {
//            return true;
//        }
//        return false;
//    }
	protected Variable addVariableFor(NetcdfFileWriter ncFile, DashDataType<?> dashType, ElemCategory category) {
		DataType ncDType = dataTypeFor(dashType);
		List<Dimension> dims = dimensionsFor(dashType, category);
		Variable var = addVariable(ncFile, dashType.getVarName(), ncDType, dims);
		addStandardAttributes(ncFile, var, dashType);
		return var;
	}
	protected static void addStandardAttributes(NetcdfFileWriter ncFile, Variable var, DashDataType<?> dtype) {
		addAttributes(ncFile, var, null, dtype.getDescription(), 
					  dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
	}
	protected DataType dataTypeFor(DashDataType<?> dashType) {
		String dataTypeName = dashType.getDataClassName().toLowerCase();
		return dataTypeFor(dataTypeName);
	}
	protected static DataType dataTypeFor(String dataTypeName) {
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
	
	protected List<Dimension> dimensionsFor(DashDataType<?> dashType, ElemCategory category) {
		String dataTypeName = dashType.getDataClassName().toLowerCase();
		Dims dims = _obsDims;
		switch (category) {
			case METADATA:
				dims = _metadataDims;
				break;
			case FEATURE:
				dims = _featureDims;
				break;
			case DATA:
				dims = _obsDims;
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
