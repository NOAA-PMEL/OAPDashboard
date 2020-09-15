/**
 * 
 */
package gov.noaa.pmel.dashboard.dsg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFileWriter.Version;

/**
 * Builds and creates a DSG NetCDF file of type Timeseries.
 * 
 * note this was copied and reduced from TrajectoryDsgFile, 
 * which was copied and reduced from ProfileDsgFile.
 * 
 * @author kamb
 *
 */
public class TimeseriesDsgFile extends DsgNcFile {
	
	private static final long serialVersionUID = 7939093989396422342L;

	public TimeseriesDsgFile(String filename) {
		super(FeatureType.TIMESERIES, filename);
	}
	public TimeseriesDsgFile(File parent, String child) {
		super(FeatureType.TIMESERIES, parent, child);
	}

//	@Override
    /*
     * From the old TrajectoryDsgNcFile class;
			tsDims.add(dimTs);

			int numSamples = stddata.getNumSamples();
			Dimension obslen = ncfile.addDimension(null, "obs", numSamples);
			List<Dimension> dataDims = new ArrayList<Dimension>();
			dataDims.add(obslen);

			List<Dimension> charDataDims = new ArrayList<Dimension>();
			charDataDims.add(obslen);
			charDataDims.add(charlen);

			List<Dimension> stringDataDims = new ArrayList<Dimension>();
			stringDataDims.add(obslen);
			stringDataDims.add(stringlen);

			ncfile.addGroupAttribute(null, new Attribute("featureType", "Timeseries"));
			ncfile.addGroupAttribute(null, new Attribute("Conventions", "CF-1.6"));
			ncfile.addGroupAttribute(null, new Attribute("history", DSG_VERSION));

			// Add the "num_obs" variable which will be assigned using the number of data points
			Variable var = ncfile.addVariable(null, "num_obs", DataType.INT, tsDims);
			ncfile.addVariableAttribute(var, new Attribute("sample_dimension", "obs"));
			ncfile.addVariableAttribute(var, new Attribute("long_name", "Number of Observations"));
			ncfile.addVariableAttribute(var, new Attribute("missing_value", DashboardUtils.INT_MISSING_VALUE));
			ncfile.addVariableAttribute(var, new Attribute("_FillValue", DashboardUtils.INT_MISSING_VALUE));

			String varName;
			// Make netCDF variables of all the metadata and data variables
			for ( Entry<DashDataType<?>,Object> entry : _metadata.getValuesMap().entrySet() ) {
				DashDataType<?> dtype = entry.getKey();
				varName = dtype.getVarName();
				Object value = entry.getValue();
				if ( DashboardUtils.isNullOrNull(value)) { 
                    System.out.println("Null value for metadata type " + varName);
                    logger.info("Null value for metadata type " + varName);
				    continue; 
				}
				
				if ( dtype instanceof StringDashDataType ) {
					// Metadata Strings
					var = ncfile.addVariable(null, varName, DataType.CHAR, tsStringDims);
					// No missing_value, _FillValue, or units for strings
					addAttributes(ncfile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
					if ( DashboardServerUtils.DATASET_ID.typeNameEquals(dtype) ) {
						ncfile.addVariableAttribute(var, new Attribute("cf_role", "timeseries_id"));
					}
				}
				else if ( dtype instanceof CharDashDataType ) {
					// Metadata characters
					var = ncfile.addVariable(null, varName, DataType.CHAR, tsCharDims);
					// No missing_value, _FillValue, or units for characters
					addAttributes(ncfile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
				}
				else if ( dtype instanceof IntDashDataType ) {
					// Metadata Integers
					var = ncfile.addVariable(null, varName, DataType.INT, tsDims);
					addAttributes(ncfile, var, DashboardUtils.INT_MISSING_VALUE, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					// Metadata Doubles
					var = ncfile.addVariable(null, varName, DataType.DOUBLE, tsDims);
					addAttributes(ncfile, var, DashboardUtils.FP_MISSING_VALUE, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
					if ( DashboardServerUtils.TIME_UNITS.get(0).equals(dtype.getUnits().get(0)) ) {
						// Additional attribute giving the time origin (although also mentioned in the units)
						ncfile.addVariableAttribute(var, new Attribute("time_origin", TIME_ORIGIN_ATTRIBUTE));
					}
				}
				else {
					throw new IllegalArgumentException("unexpected unknown metadata type: " + dtype.toString());
				}
			}

            boolean hasTime = false;
			List<DashDataType<?>> dataTypes = stddata.getDataTypes();
			for ( DashDataType<?> dtype : dataTypes )  {
			    if ( dtype.typeNameEquals(DashboardServerUtils.OTHER) ||
			         dtype.typeNameEquals(DashboardServerUtils.DATASET_ID) || 
			         dtype.typeNameEquals(DashboardServerUtils.DATASET_NAME)) {
			        continue;
			    }
			    varName = dtype.getVarName();
				if ( dtype instanceof CharDashDataType ) {
					// Data Characters
					var = ncfile.addVariable(null, varName, DataType.CHAR, charDataDims);
					// No missing_value, _FillValue, or units for characters
					addAttributes(ncfile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
				}
				else if ( dtype instanceof StringDashDataType ) {
					// Data Strings
					var = ncfile.addVariable(null, varName, DataType.CHAR, stringDataDims);
					// No missing_value, _FillValue, or units for characters
					addAttributes(ncfile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
				}
				else if ( dtype instanceof IntDashDataType ) {
					// Data Integers
					var = ncfile.addVariable(null, varName, DataType.INT, dataDims);
					addAttributes(ncfile, var, DashboardUtils.INT_MISSING_VALUE, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					// Data Doubles
					var = ncfile.addVariable(null, varName, DataType.DOUBLE, dataDims);
					addAttributes(ncfile, var, DashboardUtils.FP_MISSING_VALUE, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
					if ( DashboardServerUtils.TIME.typeNameEquals(dtype) ) {
						// Additional attribute giving the time origin (although also mentioned in the units)
                        hasTime = true;
						ncfile.addVariableAttribute(var, new Attribute("time_origin", TIME_ORIGIN_ATTRIBUTE));
					}
					if ( dtype.getStandardName().endsWith("depth") ) {
						ncfile.addVariableAttribute(var, new Attribute("positive", "down"));
					}
				}
				else {
					throw new IllegalArgumentException("unexpected unknown data type: " + dtype.toString());
				}
			}
            
            if ( !hasTime ) {
                DoubleDashDataType dtype = DashboardServerUtils.TIME;
                var = ncfile.addVariable(null, "time", DataType.DOUBLE, dataDims);
    			addAttributes(ncfile, var, DashboardUtils.FP_MISSING_VALUE, dtype.getDescription(), 
            				  dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
    			ncfile.addVariableAttribute(var, new Attribute("time_origin", TIME_ORIGIN_ATTRIBUTE));
            }
			ncfile.create();

			// The header has been created.  Now let's fill it up.
			var = ncfile.findVariable("num_obs");
			if ( var == null )
				throw new RuntimeException("Unexpected failure to find ncfile variable num_obs");
			ArrayInt.D1 obscount = new ArrayInt.D1(1);
			obscount.set(0, numSamples);
			ncfile.write(var, obscount);

			for (  Entry<DashDataType<?>,Object> entry : _metadata.getValuesMap().entrySet() ) {
				DashDataType<?> dtype = entry.getKey();
				varName = dtype.getVarName();
				var = ncfile.findVariable(varName);
				if ( var == null ) {
//					throw new RuntimeException("Unexpected failure to find ncfile variable " + varName);
                    System.out.println("Unexpected failure to find ncfile metadata variable " + varName);
                    logger.warn("Unexpected failure to find ncfile metadata variable " + varName);
                    continue;
				}
				
				if ( dtype instanceof StringDashDataType ) {
					// Metadata Strings
					String dvalue = (String) entry.getValue();
					if ( dvalue == null )
						dvalue = DashboardUtils.STRING_MISSING_VALUE;
					ArrayChar.D2 mvar = new ArrayChar.D2(1, maxchar);
					mvar.setString(0, dvalue);
					ncfile.write(var, mvar);
				}
				else if ( dtype instanceof CharDashDataType ) {
					// Metadata characters
					Character dvalue = (Character) entry.getValue();
					if ( dvalue == null )
						dvalue = DashboardUtils.CHAR_MISSING_VALUE;
					ArrayChar.D2 mvar = new ArrayChar.D2(1, 1);
					mvar.set(0, 0, dvalue);
					ncfile.write(var, mvar);
				}
				else if ( dtype instanceof IntDashDataType ) {
					// Metadata Integers
					Integer dvalue = (Integer) entry.getValue();
					if ( dvalue == null )
						dvalue = DashboardUtils.INT_MISSING_VALUE;
					ArrayInt.D1 mvar = new ArrayInt.D1(1);
					mvar.set(0, dvalue);
					ncfile.write(var, mvar);
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					// Metadata Doubles
					Double dvalue = (Double) entry.getValue();
					if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
						dvalue = DashboardUtils.FP_MISSING_VALUE;
					ArrayDouble.D1 mvar = new ArrayDouble.D1(1);
					mvar.set(0, dvalue);
					ncfile.write(var, mvar);
				}
				else {
					throw new IllegalArgumentException("unexpected unknown metadata type: " + dtype.toString());
				}
			}

            if ( !hasTime ) {
                Variable tvar = ncfile.findVariable("time");
                ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
                Double[] sampleTimes = stddata.getSampleTimes();
				for (int j = 0; j < numSamples; j++) {
					dvar.set(j, sampleTimes[j].doubleValue());
				}
                ncfile.write(tvar, dvar);
            }
			for (int k = 0; k < stddata.getNumDataCols(); k++) {
				DashDataType<?> dtype = dataTypes.get(k);
				varName = dtype.getVarName();
				var = ncfile.findVariable(varName);
				if ( var == null ) {
//					throw new RuntimeException("Unexpected failure to find ncfile variable " + varName);
                    System.out.println("Unexpected failure to find ncfile data variable " + varName);
                    logger.warn("Unexpected failure to find ncfile data variable " + varName);
                    continue;
				}
				if ( dtype instanceof StringDashDataType ) {
					// Data Strings
					ArrayChar.D2 dvar = new ArrayChar.D2(numSamples, maxchar);
					for (int j = 0; j < numSamples; j++) {
    					String dvalue = (String) stddata.getStdVal(j, k);
    					if ( dvalue == null )
    						dvalue = DashboardUtils.STRING_MISSING_VALUE;
        					dvar.setString(j, dvalue);
					}
					ncfile.write(var, dvar);
				} else if ( dtype instanceof CharDashDataType ) {
					// Data Characters
					ArrayChar.D2 dvar = new ArrayChar.D2(numSamples, 1);
					for (int j = 0; j < numSamples; j++) {
						Character dvalue = (Character) stddata.getStdVal(j, k);
						if ( dvalue == null )
							dvalue = DashboardUtils.CHAR_MISSING_VALUE;
						dvar.set(j, 0, dvalue);
					}
					ncfile.write(var, dvar);
				}
				else if ( dtype instanceof IntDashDataType ) {
					// Data Integers
					ArrayInt.D1 dvar = new ArrayInt.D1(numSamples);
					for (int j = 0; j < numSamples; j++) {
						Integer dvalue = (Integer) stddata.getStdVal(j, k);
						if ( dvalue == null )
							dvalue = DashboardUtils.INT_MISSING_VALUE;
						dvar.set(j, dvalue);
					}
					ncfile.write(var, dvar);
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					// Data Doubles
					ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
					for (int j = 0; j < numSamples; j++) {
						Double dvalue = (Double) stddata.getStdVal(j, k);
						if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
							dvalue = DashboardUtils.FP_MISSING_VALUE;
						dvar.set(j, dvalue);
					}
					ncfile.write(var, dvar);
				}
				else {
					throw new IllegalArgumentException("unexpected unknown data type: " + dtype.toString());
				}
			}
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	}
    */
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgNcFile#typeSpecificInitializations()
     */
    @Override
    protected void doFeatureTypeSpecificInitialization() {
        // TODO Auto-generated method stub
    }
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgNcFile#addTypeVariables(ucar.nc2.NetcdfFileWriter)
     */
    @Override
    protected void addFeatureTypeVariables(NetcdfFileWriter ncFile) {
        // featureId added by superclass
        Variable var = addTimeVariable(ncFile, SAMPLE_TIME_VARNAME, "Time", _obsDimList, true);
        _typeSpecificVariables.put(var.getShortName(), var);
        var = addLatitudeVariable(ncFile, FEATURE_LAT_VARNAME, "Timeseries Latitude", _featureDimList, true);
        _typeSpecificVariables.put(var.getShortName(), var);
        var = addLongitudeVariable(ncFile, FEATURE_LON_VARNAME , "Timeseries Longitude", _featureDimList, true);
        _typeSpecificVariables.put(var.getShortName(), var);
        // prefer depth to pressure
        if ( _stdUser.hasDataColumn(FEATURE_PRESSURE_VARNAME)) {
            var = addPressureVariable(ncFile, FEATURE_PRESSURE_VARNAME, "Timeseries Pressure", _featureDimList, true);
            _typeSpecificVariables.put(var.getShortName(), var);
        } else if ( _stdUser.hasDataColumn(FEATURE_DEPTH_VARNAME)) {
            var = addDepthVariable(ncFile, FEATURE_DEPTH_VARNAME, "Timeseries Depth", _featureDimList, true);
            _typeSpecificVariables.put(var.getShortName(), var);
        }
    }
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgNcFile#writeTypeVariables(ucar.nc2.NetcdfFileWriter)
     */
    @Override
    protected void writeFeatureTypeVariables(NetcdfFileWriter ncFile) throws Exception {
		Variable vFeaturId = getVariable(ncFile, _featureType.dsgTypeName());
		ArrayChar.D2 aFeaturId = new ArrayChar.D2(getNumFeatures(), _maxStrLen);
		Variable vRowCount = getVariable(ncFile, SAMPLES_PER_FEATURE_VARNAME);
		ArrayInt.D1 aRowCount = new ArrayInt.D1(getNumFeatures());

        // XXX TODO: Multiple timeseries in one file?
        aFeaturId.setString(0, _stdUser.getDatasetName());
        aRowCount.set(0, _stdUser.numSamples);

		ncFile.write(vFeaturId, aFeaturId);
		ncFile.write(vRowCount, aRowCount);
        writeObsCoordinateVaiables(ncFile);
		ncFile.flush();
        Double[] times = _stdUser.getSampleTimes();
        Variable v_time = getVariable(ncFile, SAMPLE_TIME_VARNAME);
        ArrayDouble.D1 a_time = new ArrayDouble.D1(_numObservations);
        for (int row=0; row<_numObservations; row++) {
            a_time.set(row, times[row].doubleValue());
        }
		ncFile.write(v_time, a_time);
	}
    
    private void writeObsCoordinateVaiables(NetcdfFileWriter ncFile) 
            throws IOException, InvalidRangeException {
//        Double[] times = _stdUser.getSampleTimes();
//        Double[] lats = _stdUser.getSampleLatitudes();
//        Double[] lons = _stdUser.getSampleLongitudes();
        Double presDep = null;
//        Variable v_time = getVariable(ncFile, SAMPLE_TIME_VARNAME);
//        ArrayDouble.D1 a_time = new ArrayDouble.D1(_numObservations);
        Variable v_lat = getVariable(ncFile, FEATURE_LAT_VARNAME);
        ArrayDouble.D1 a_lat = new ArrayDouble.D1(getNumFeatures());
        Variable v_lon = getVariable(ncFile, FEATURE_LON_VARNAME);
        ArrayDouble.D1 a_lon = new ArrayDouble.D1(getNumFeatures());
        Variable v_PresDep = null;
        ArrayDouble.D1 a_PresDep = null;
        if ( _stdUser.hasSampleDepth()) {
            presDep = _stdUser.getSampleDepth(0);
            v_PresDep = getVariable(ncFile, FEATURE_DEPTH_VARNAME);
            a_PresDep = new ArrayDouble.D1(getNumFeatures());
        } else if ( _stdUser.hasSamplePressure()) {
            presDep = _stdUser.getSamplePressure(0);
            v_PresDep = getVariable(ncFile, FEATURE_DEPTH_VARNAME);
            a_PresDep = new ArrayDouble.D1(getNumFeatures());
        }
        
//        for (int row=0; row<_numObservations; row++) {
//            a_time.set(row, times[row].doubleValue());
            a_lat.set(0, _stdUser.getSampleLatitude(0).doubleValue());
            a_lon.set(0, _stdUser.getSampleLongitude(0).doubleValue());
            if ( presDep != null ) {
                a_PresDep.set(0, presDep);
            }
//        }
//        ncFile.write(v_time, a_time);
        ncFile.write(v_lat, a_lat);
        ncFile.write(v_lon, a_lon);
        if ( presDep != null ) {
            ncFile.write(v_PresDep, a_PresDep);
        }
    }
    
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgNcFile#getNumFeatures()
     */
    @Override
    protected int getNumFeatures() {
        // TODO Auto-generated method stub
        return 1;
    }
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgNcFile#checkFeatureTypeSpecificIndeces()
     */
    @Override
    protected void checkFeatureTypeSpecificIndeces() {
        // TODO Auto-generated method stub
        
    }
}
