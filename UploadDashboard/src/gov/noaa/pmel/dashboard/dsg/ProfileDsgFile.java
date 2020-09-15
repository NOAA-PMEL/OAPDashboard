/**
 * 
 */
package gov.noaa.pmel.dashboard.dsg;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.datatype.CastSet;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * @author kamb
 *
 */
public class ProfileDsgFile extends DsgNcFile {

	private static final long serialVersionUID = -7178421505101183090L;

	private static Logger logger = LogManager.getLogger(ProfileDsgFile.class);
	
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
	
	public static final String PROF_ID_SEPARATOR = "__";
	

	private Collection<CastSet> _casts;
    
	public ProfileDsgFile(String filename) {
		super(FeatureType.PROFILE, filename);
	}
	public ProfileDsgFile(File parent, String child) {
		super(FeatureType.PROFILE, parent, child);
	}
	
    protected int getNumFeatures() {
        return _casts.size();
    }
    
//	private void addMetadataVariables(NetcdfFileWriter ncFile) throws Exception {
//		for ( Entry<DashDataType<?>,Object> entry : _metadata.getValuesMap().entrySet() ) {
//			DashDataType<?> dtype = entry.getKey();
//			Object value = entry.getValue();
//			String varName = dtype.getVarName();
//			logger.debug("metadata var:"+varName + ":" + value);
//			if ( DashboardUtils.isEmptyNullOrNull(value)) { continue; }
//			Variable var = addVariableFor(ncFile, dtype, ElemCategory.METADATA);
//			logger.debug("Added metadata variable " + var);
//		}
//	}
//	private void writeMetadataVariables(NetcdfFileWriter ncFile) throws Exception {
//		for (  Entry<DashDataType<?>,Object> entry : _metadata.getValuesMap().entrySet() ) {
//			DashDataType<?> dtype = entry.getKey();
//			String varName = dtype.getVarName();
//			Object value = entry.getValue();
//			Variable var = ncFile.findVariable(varName);
//			if ( var == null || DashboardUtils.isNullOrNull(value)) {
//				continue;
//			}
//			writeVariableData(ncFile, var, dtype, new Object[] { value });
//		}
//	}
	
//		var = addFeatureIdVariable(ncFile, _featureDimList);
//        _typeVariables.put(var.getShortName(), var);CAST_
    @Override
    protected void doFeatureTypeSpecificInitialization() {
        _casts = CastSet.extractCastSetsFrom(_stdUser);
    }
	@Override
	protected void addFeatureTypeVariables(NetcdfFileWriter ncFile) {
		Variable var = addTimeVariable(ncFile, FEATURE_TIME_VARNAME, "Time", _featureDimList, true);
        _typeSpecificVariables.put(var.getShortName(), var);
		var = addLatitudeVariable(ncFile, FEATURE_LAT_VARNAME, "Cast Latitude", _featureDimList, true);
        _typeSpecificVariables.put(var.getShortName(), var);
		var = addLongitudeVariable(ncFile, FEATURE_LON_VARNAME , "Cast Longitude", _featureDimList, true);
        _typeSpecificVariables.put(var.getShortName(), var);
        if ( _stdUser.hasSamplePressure()) {
            var = addPressureVariable(ncFile, SAMPLE_PRESSURE_VARNAME, "Sample Pressure", _obsDimList, true);
            _typeSpecificVariables.put(var.getShortName(), var);
            _typeSpecificVariables.put("ctd_pressure", var); // XXX Hack!
        } else if ( _stdUser.hasSampleDepth()) {
            var = addDepthVariable(ncFile, SAMPLE_DEPTH_VARNAME, "Sample Depth", _obsDimList, true);
            _typeSpecificVariables.put(var.getShortName(), var);
        }
	}
	
//    @Override
//    protected boolean excludeColumnFromVariables(int columnIndex, DashDataType<?> dtype) {
//        return super.excludeColumnFromVariables(columnIndex, dtype) 
//                || _typeSpecificVariables.containsKey(dtype.getVarName());
////                 || dtype.typeNameEquals(DashboardUtils.LATITUDE)
////                 || dtype.typeNameEquals(DashboardUtils.LONGITUDE)
////                 || dtype.typeNameEquals(DashboardUtils.SAMPLE_DEPTH)
////                 || dtype.typeNameLike("time");
//    }
//    @Override
//    protected boolean excludeColumnFromData(int columnIndex, DashDataType<?> dtype) {
//        return super.excludeColumnFromVariables(columnIndex, dtype) 
//                || _typeSpecificVariables.containsKey(dtype.getVarName());
////                 || dtype.typeNameEquals(DashboardUtils.LATITUDE)
////                 || dtype.typeNameEquals(DashboardUtils.LONGITUDE)
////                 || dtype.typeNameEquals(DashboardUtils.SAMPLE_DEPTH)
////                 || dtype.typeNameLike("time");
//    }

	@Override
	protected void writeFeatureTypeVariables(NetcdfFileWriter ncFile) throws IOException, InvalidRangeException {
        int numFeatures = getNumFeatures();
		Variable vProfId = getVariable(ncFile, _featureType.dsgTypeName());
		ArrayChar.D2 aProfId = new ArrayChar.D2(numFeatures, _maxStrLen);
		Variable vRowCount = getVariable(ncFile, SAMPLES_PER_FEATURE_VARNAME);
		ArrayInt.D1 aRowCount = new ArrayInt.D1(numFeatures);
		Variable vProfTime = getVariable(ncFile, FEATURE_TIME_VARNAME);
		ArrayDouble.D1 aProfTime = new ArrayDouble.D1(numFeatures);
		Variable vProfLat = getVariable(ncFile, FEATURE_LAT_VARNAME);
		ArrayDouble.D1 aProfLat = new ArrayDouble.D1(numFeatures);
		Variable vProfLon = getVariable(ncFile, FEATURE_LON_VARNAME);
		ArrayDouble.D1 aProfLon = new ArrayDouble.D1(numFeatures);
		
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
		ncFile.write(vProfId, aProfId);
		ncFile.write(vRowCount, aRowCount);
		ncFile.write(vProfTime, aProfTime);
		ncFile.write(vProfLat, aProfLat);
		ncFile.write(vProfLon, aProfLon);
        writeObsCoordinateVaiables(ncFile);
		ncFile.flush();
	}
    private void writeObsCoordinateVaiables(NetcdfFileWriter ncFile) 
            throws IOException, InvalidRangeException {
        Variable v_PresDep = null;
        ArrayDouble.D1 a_PresDep = null;
        Double[] presDep = null;
        if ( _stdUser.hasSamplePressure()) {
            presDep = _stdUser.getSamplePressures();
            v_PresDep = getVariable(ncFile, SAMPLE_PRESSURE_VARNAME);
            a_PresDep = new ArrayDouble.D1(_numObservations);
        } else if ( _stdUser.hasSampleDepth()) {
            presDep = _stdUser.getSampleDepths();
            v_PresDep = getVariable(ncFile, SAMPLE_DEPTH_VARNAME);
            a_PresDep = new ArrayDouble.D1(_numObservations);
        }
        if ( presDep != null ) {
            for (int row=0; row<_numObservations; row++) {
                if ( a_PresDep != null ) {
                    a_PresDep.set(row, presDep[row].doubleValue());
                }
            }
            ncFile.write(v_PresDep, a_PresDep);
        }
    }
		
	private String datasetProfileId(String id) {
        return _datasetId + PROF_ID_SEPARATOR + id;
    }

//    private Map<String, DashDataType<?>> qcCols = new HashMap<>();
	
//	protected void XaddQcVariables(NetcdfFileWriter ncFile, KnownDataTypes dataTypes) {
//		for (String qcdVar : qcd_vars) {
//			DashDataType<?> dataCol =  _stdUser.findDataColumn(qcdVar);
//			if ( dataCol == null ) {
//				logger.info("No data column found for " + qcdVar+". Skipping QC variable.");
//				continue;
//			}
//			String qcName = qcdVar + QC_VARNAME_EXTENSION;
//			if ( qcCols.containsKey(qcName)) {
//				qcCols.remove(qcName);
//			} else {
//				System.out.println("Need to add column for :" + qcName);
//			}
//			DashDataType<?> qcCol =  _stdUser.findDataColumn(qcName);
//			if ( qcCol == null ) {
//				System.out.println("No column found for " + qcName + ". Adding one.");
//				qcCol = dataTypes.getDataType(qcName);
//			}
//			if ( qcCol == null ) {
//				System.out.println("No datatype found for " + qcName + ". Skipping.");
//				continue;
//			}
//			Variable var = addVariableFor(ncFile, qcCol, ElemCategory.DATA);
//			logger.debug("Added qc variable " + var);
//		}
//		if ( ! qcCols.isEmpty()) {
//			for (DashDataType<?> qcCol : qcCols.values()) {
//				Variable var = addVariableFor(ncFile, qcCol, ElemCategory.DATA);
//				logger.debug("Added qc variable " + var);
//			}
//		}
//		
//	}
//	@Override
//	protected void writeQcVariables(NetcdfFileWriter ncFile) throws Exception {
//        
//	}
		
    
//	private void writeVariableData(NetcdfFileWriter ncFile, Variable var, DashDataType<?> dtype, Object[] data) 
//			throws Exception {
//		int numSamples = data.length;
//		if ( dtype instanceof CharDashDataType ) {
//			ArrayChar dvar = var.getRank() > 1 ?
//								new ArrayChar.D2(numSamples, 1) :
//								new ArrayChar.D1(1);
//			for (int j = 0; j < numSamples; j++) {
//				Character dvalue = (Character) data[j];
//				if ( dvalue == null ) {
//					dvalue = DashboardUtils.CHAR_MISSING_VALUE;
//				}
//				dvar.setChar(j, dvalue.charValue());
//			}
//			ncFile.write(var, dvar);
//		}
//		else if ( dtype instanceof StringDashDataType ) {
//			ArrayChar dvar =   var.getRank() > 1 ? 
//								new ArrayChar.D2(numSamples, _maxStrLen) :
//								new ArrayChar.D1(_maxStrLen);
//			for (int j = 0; j < numSamples; j++) {
//				String dvalue = (String) data[j];
//				if ( dvalue == null ) {
//					dvalue = DashboardUtils.STRING_MISSING_VALUE;
//				}
//				if ( var.getRank() > 1 ) {
//					dvar.setString(j, dvalue);
//				} else {
//					dvar.setString(dvalue);
//				}
//			}
//			ncFile.write(var, dvar);
//		}
//		else if ( dtype instanceof IntDashDataType ) {
//			ArrayInt.D1 dvar = new ArrayInt.D1(numSamples);
//			for (int j = 0; j < numSamples; j++) {
//				Integer dvalue = (Integer) data[j];
//				if ( dvalue == null )
//					dvalue = DashboardUtils.INT_MISSING_VALUE;
//				dvar.set(j, dvalue.intValue());
//			}
//			ncFile.write(var, dvar);
//		}
//		else if ( dtype instanceof DoubleDashDataType ) {
//			// Data Doubles
//			ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
//			for (int j = 0; j < numSamples; j++) {
//				Double dvalue = (Double) data[j];
//				if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
//					dvalue = DashboardUtils.FP_MISSING_VALUE;
//				dvar.set(j, dvalue.doubleValue());
//			}
//			ncFile.write(var, dvar);
//		}
//		else {
//			throw new IllegalArgumentException("unexpected unknown data type: " + dtype.toString());
//		}
//		ncFile.flush();
//	}
//		
//	private void writeSampleId(NetcdfFileWriter ncFile) throws Exception {
//		int numSamples = _stdUser.getNumSamples();
//		int idColIdx = _stdUser.getNumDataCols()-2; // _stdUser.findDataColumnIndex(SAMPLE_ID_VARNAME);
//		boolean useColumn = idColIdx != -1;
//		Variable ncSampleId = ncFile.findVariable(SAMPLE_ID_VARNAME);
//		ArrayChar.D2 idVar = new ArrayChar.D2(numSamples, _maxStrLen);
//		for (int sampleRow = 0; sampleRow < numSamples; sampleRow++) {
//		    String sid = useColumn ? String.valueOf(_stdUser.getStdVal(sampleRow, idColIdx)) : "_"+sampleRow;
//		    idVar.setString(sampleRow, sid);
//		}
//		ncFile.write(ncSampleId, idVar);
//	}
//	private void writeSampleTime(NetcdfFileWriter ncFile) throws Exception {
//		int numSamples = _stdUser.getNumSamples();
//		Double[] sampleTimes = _stdUser.getSampleTimes();
//		Variable vSampleTime = ncFile.findVariable(SAMPLE_TIME_VARNAME);
//		ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
//		for (int j = 0; j < numSamples; j++) {
//			Double dvalue = sampleTimes[j];
//			if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
//				dvalue = DashboardUtils.FP_MISSING_VALUE;
//			dvar.set(j, dvalue.doubleValue());
//		}
//		ncFile.write(vSampleTime, dvar);
//	}
//	private void writeSampleDepths(NetcdfFileWriter ncFile) throws Exception {
//		int numSamples = _stdUser.getNumSamples();
//		Double[] sampleDepths = _stdUser.getSampleDepths();
//		Variable vSampleDepth = ncFile.findVariable(SAMPLE_DEPTH_VARNAME);
//		ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
//		for (int j = 0; j < numSamples; j++) {
//			Double dvalue = sampleDepths[j];
//			if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
//				dvalue = DashboardUtils.FP_MISSING_VALUE;
//			dvar.set(j, dvalue.doubleValue());
//		}
//		ncFile.write(vSampleDepth, dvar);
//	}
//	private void writeSampleLocations(NetcdfFileWriter ncFile) throws Exception {
//		int numSamples = _stdUser.getNumSamples();
//		Double[] sampleLats = _stdUser.getSampleLatitudes();
//		Double[] sampleLons = _stdUser.getSampleLongitudes();
//		Variable vSampleLats = ncFile.findVariable(SAMPLE_LAT_VARNAME);
//		Variable vSampleLons = ncFile.findVariable(SAMPLE_LON_VARNAME);
//		ArrayDouble.D1 dLatVar = new ArrayDouble.D1(numSamples);
//		ArrayDouble.D1 dLonVar = new ArrayDouble.D1(numSamples);
//		for (int j = 0; j < numSamples; j++) {
//			Double dLatValue = sampleLats[j];
//			Double dLonValue = sampleLons[j];
//			if ( (dLatValue == null) || dLatValue.isNaN() || dLatValue.isInfinite() )
//				dLatValue = DashboardUtils.FP_MISSING_VALUE;
//			dLatVar.set(j, dLatValue.doubleValue());
//			if ( (dLonValue == null) || dLonValue.isNaN() || dLonValue.isInfinite() )
//				dLonValue = DashboardUtils.FP_MISSING_VALUE;
//			dLonVar.set(j, dLonValue.doubleValue());
//		}
//		ncFile.write(vSampleLats, dLatVar);
//		ncFile.write(vSampleLons, dLonVar);
//	}
    @Override
	protected void checkFeatureTypeSpecificIndeces() {
		// Quick recheck of data column indices already assigned in StdDataArray
		if ( ! ( _stdUser.hasSampleDepth() || _stdUser.hasSamplePressure() ))
			throw new IllegalArgumentException("no sample pressure or depth data column");
	}
	
//	private Variable addSampleIdVariable(NetcdfFileWriter ncFile, List<Dimension> dims) {
//		Variable var = ncFile.addVariable(null, SAMPLE_ID_VARNAME, DataType.CHAR, dims);
//		ncFile.addVariableAttribute(var, new Attribute("long_name", "Unique identifier for each sample."));
////		ncFile.addVariableAttribute(var, new Attribute("cf_role", "sample_id"));
//		return var;
//	}
//	private Variable addSampleTimeVariable(NetcdfFileWriter ncFile, List<Dimension> dims) {
//		return addTimeVariable(SAMPLE_TIME_VARNAME, "Sample Time", ncFile, dims, true);
//	}
//	private Variable addSampleLocationVariables(NetcdfFileWriter ncFile, List<Dimension> dims) {
//		addLatitudeVariable(ncFile, SAMPLE_LAT_VARNAME, "Sample Latitude", dims, true);
//		return addLongitudeVariable(ncFile, SAMPLE_LON_VARNAME, "Sample Longitude", dims, true);
//	}
//	private Variable addSampleDepthVariable(NetcdfFileWriter ncFile, List<Dimension> dims) {
//        return addDepthVariable(ncFile, SAMPLE_DEPTH_VARNAME, "Sample Depth", dims, true);
//	}
//	private Collection<DashDataType<?>> getDataVariablesToWriteToDsgFile() {
////		String[] varNames = new String[] { // "cast", "cruise", "time", "latitude", "longitude",
////											"date",
////											"ctd_pressure", "sample_depth", "ctd_temperature", 
////											"ctd_salinity", "ctd_oxygen", "ctd_flouride",
////											"bottle_oxygen", "nitrite", "nitrate", "phosphate", "ammonia",
////											"alkalinity", "carbon", "pH" };
//		List<DashDataType<?>> dataTypesToWrite = new ArrayList<>(_stdUser.getDataTypes());
//		// TODO: remove excluded columns
//		return dataTypesToWrite;
//	}

}
