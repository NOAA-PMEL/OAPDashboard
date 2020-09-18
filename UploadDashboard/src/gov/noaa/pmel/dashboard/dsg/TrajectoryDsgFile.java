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
 * Builds and creates a DSG NetCDF file of type Trajectory.
 * 
 * note this was copied and reduced from ProfileDsgFile. 
 * 
 * @author kamb
 *
 */
public class TrajectoryDsgFile extends DsgNcFile {
	
	private static final long serialVersionUID = 7939093989396422342L;

	public TrajectoryDsgFile(String filename) {
		super(FeatureType.TRAJECTORY, filename);
	}
	public TrajectoryDsgFile(File parent, String child) {
		super(FeatureType.TRAJECTORY, parent, child);
	}

//	@Override
	public void Xcreate(DsgMetadata metaData, StdUserDataArray fileData) 
			throws IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException {
		if ( metaData == null )
			throw new IllegalArgumentException("no metadata given");
		_metadata = metaData;
		if ( fileData == null )
			throw new IllegalArgumentException("no data given");
		_stdUser = fileData;
//		checkIndeces(_stdUser);
		
		try ( NetcdfFileWriter ncFile = NetcdfFileWriter.createNew(Version.netcdf3, getPath()); ) {
			// According to the CF standard if a file only has one trajectory, 
			// then the trajectory dimension is not necessary.
			// However, who knows what would break downstream from this process without it...
			Dimension traj = ncFile.addDimension(null, "trajectory", 1);

			// There will be a number of trajectory variables of type character from the metadata.
			// Which is the longest?
			int maxchar = _metadata.getMaxStringLength();
			Dimension stringlen = ncFile.addDimension(null, "string_length", maxchar);
			List<Dimension> trajStringDims = new ArrayList<Dimension>();
			trajStringDims.add(traj);
			trajStringDims.add(stringlen);

			Dimension charlen = ncFile.addDimension(null, "char_length", 1);
			List<Dimension> trajCharDims = new ArrayList<Dimension>();
			trajCharDims.add(traj);
			trajCharDims.add(charlen);

			List<Dimension> trajDims = new ArrayList<Dimension>();
			trajDims.add(traj);

			int numSamples = _stdUser.getNumSamples();
			Dimension obslen = ncFile.addDimension(null, "obs", numSamples);
			List<Dimension> dataDims = new ArrayList<Dimension>();
			dataDims.add(obslen);

			List<Dimension> charDataDims = new ArrayList<Dimension>();
			charDataDims.add(obslen);
			charDataDims.add(charlen);

			List<Dimension> stringDataDims = new ArrayList<Dimension>();
			stringDataDims.add(obslen);
			stringDataDims.add(stringlen);

			ncFile.addGroupAttribute(null, new Attribute("featureType", "Trajectory"));
			ncFile.addGroupAttribute(null, new Attribute("Conventions", "CF-1.6"));
			ncFile.addGroupAttribute(null, new Attribute("history", DSG_VERSION));

			// Add the "num_obs" variable which will be assigned using the number of data points
			Variable var = ncFile.addVariable(null, "num_obs", DataType.INT, trajDims);
			ncFile.addVariableAttribute(var, new Attribute("sample_dimension", "obs"));
			ncFile.addVariableAttribute(var, new Attribute("long_name", "Number of Observations"));
			ncFile.addVariableAttribute(var, new Attribute("missing_value", DashboardUtils.INT_MISSING_VALUE));
			ncFile.addVariableAttribute(var, new Attribute("_FillValue", DashboardUtils.INT_MISSING_VALUE));

			String varName;
			// Make netCDF variables of all the metadata and data variables
			for ( Entry<DashDataType<?>,Object> entry : _metadata.getValuesMap().entrySet() ) {
				DashDataType<?> dtype = entry.getKey();
				varName = dtype.getVarName();
				Object value = entry.getValue();
				if ( DashboardUtils.isEmptyNullOrNull(value)) { 
                    System.out.println("Null value for metadata type " + varName);
                    logger.info("Null value for metadata type " + varName);
				    continue; 
				}
				
				if ( dtype instanceof StringDashDataType ) {
					// Metadata Strings
					var = ncFile.addVariable(null, varName, DataType.CHAR, trajStringDims);
					// No missing_value, _FillValue, or units for strings
					addAttributes(ncFile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
					if ( DashboardServerUtils.DATASET_ID.typeNameEquals(dtype) ) {
						ncFile.addVariableAttribute(var, new Attribute("cf_role", "trajectory_id"));
					}
				}
				else if ( dtype instanceof CharDashDataType ) {
					// Metadata characters
					var = ncFile.addVariable(null, varName, DataType.CHAR, trajCharDims);
					// No missing_value, _FillValue, or units for characters
					addAttributes(ncFile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
				}
				else if ( dtype instanceof IntDashDataType ) {
					// Metadata Integers
					var = ncFile.addVariable(null, varName, DataType.INT, trajDims);
					addAttributes(ncFile, var, DashboardUtils.INT_MISSING_VALUE, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					// Metadata Doubles
					var = ncFile.addVariable(null, varName, DataType.DOUBLE, trajDims);
					addAttributes(ncFile, var, DashboardUtils.FP_MISSING_VALUE, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
					if ( DashboardServerUtils.TIME_UNITS.get(0).equals(dtype.getUnits().get(0)) ) {
						// Additional attribute giving the time origin (although also mentioned in the units)
						ncFile.addVariableAttribute(var, new Attribute("time_origin", TIME_ORIGIN_ATTRIBUTE));
					}
				}
				else {
					throw new IllegalArgumentException("unexpected unknown metadata type: " + dtype.toString());
				}
			}

            boolean hasTime = false;
			List<DashDataType<?>> dataTypes = _stdUser.getDataTypes();
			for ( DashDataType<?> dtype : dataTypes )  {
			    if ( dtype.typeNameEquals(DashboardServerUtils.OTHER) ||
			         dtype.typeNameEquals(DashboardServerUtils.DATASET_ID) || 
			         dtype.typeNameEquals(DashboardServerUtils.DATASET_NAME)) {
			        continue;
			    }
			    varName = dtype.getVarName();
				if ( dtype instanceof CharDashDataType ) {
					// Data Characters
					var = ncFile.addVariable(null, varName, DataType.CHAR, charDataDims);
					// No missing_value, _FillValue, or units for characters
					addAttributes(ncFile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
				}
				else if ( dtype instanceof StringDashDataType ) {
					// Data Strings
					var = ncFile.addVariable(null, varName, DataType.CHAR, stringDataDims);
					// No missing_value, _FillValue, or units for characters
					addAttributes(ncFile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
				}
				else if ( dtype instanceof IntDashDataType ) {
					// Data Integers
					var = ncFile.addVariable(null, varName, DataType.INT, dataDims);
					addAttributes(ncFile, var, DashboardUtils.INT_MISSING_VALUE, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					// Data Doubles
					var = ncFile.addVariable(null, varName, DataType.DOUBLE, dataDims);
					addAttributes(ncFile, var, DashboardUtils.FP_MISSING_VALUE, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
					if ( DashboardServerUtils.TIME.typeNameEquals(dtype) ) {
						// Additional attribute giving the time origin (although also mentioned in the units)
                        hasTime = true;
						ncFile.addVariableAttribute(var, new Attribute("time_origin", TIME_ORIGIN_ATTRIBUTE));
					}
					if ( dtype.getStandardName().endsWith("depth") ) {
						ncFile.addVariableAttribute(var, new Attribute("positive", "down"));
					}
				}
				else {
					throw new IllegalArgumentException("unexpected unknown data type: " + dtype.toString());
				}
			}
            
            if ( !hasTime ) {
                DoubleDashDataType dtype = DashboardServerUtils.TIME;
                var = ncFile.addVariable(null, "time", DataType.DOUBLE, dataDims);
    			addAttributes(ncFile, var, DashboardUtils.FP_MISSING_VALUE, dtype.getDescription(), 
            				  dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
    			ncFile.addVariableAttribute(var, new Attribute("time_origin", TIME_ORIGIN_ATTRIBUTE));
            }
			ncFile.create();

			// The header has been created.  Now let's fill it up.
			var = ncFile.findVariable("num_obs");
			if ( var == null )
				throw new RuntimeException("Unexpected failure to find ncFile variable num_obs");
			ArrayInt.D1 obscount = new ArrayInt.D1(1);
			obscount.set(0, numSamples);
			ncFile.write(var, obscount);

			for (  Entry<DashDataType<?>,Object> entry : _metadata.getValuesMap().entrySet() ) {
				DashDataType<?> dtype = entry.getKey();
				varName = dtype.getVarName();
				var = ncFile.findVariable(varName);
				if ( var == null ) {
//					throw new RuntimeException("Unexpected failure to find ncFile variable " + varName);
                    System.out.println("Unexpected failure to find ncFile metadata variable " + varName);
                    logger.warn("Unexpected failure to find ncFile metadata variable " + varName);
                    continue;
				}
				
				if ( dtype instanceof StringDashDataType ) {
					// Metadata Strings
					String dvalue = (String) entry.getValue();
					if ( dvalue == null )
						dvalue = DashboardUtils.STRING_MISSING_VALUE;
					ArrayChar.D2 mvar = new ArrayChar.D2(1, maxchar);
					mvar.setString(0, dvalue);
					ncFile.write(var, mvar);
				}
				else if ( dtype instanceof CharDashDataType ) {
					// Metadata characters
					Character dvalue = (Character) entry.getValue();
					if ( dvalue == null )
						dvalue = DashboardUtils.CHAR_MISSING_VALUE;
					ArrayChar.D2 mvar = new ArrayChar.D2(1, 1);
					mvar.set(0, 0, dvalue);
					ncFile.write(var, mvar);
				}
				else if ( dtype instanceof IntDashDataType ) {
					// Metadata Integers
					Integer dvalue = (Integer) entry.getValue();
					if ( dvalue == null )
						dvalue = DashboardUtils.INT_MISSING_VALUE;
					ArrayInt.D1 mvar = new ArrayInt.D1(1);
					mvar.set(0, dvalue);
					ncFile.write(var, mvar);
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					// Metadata Doubles
					Double dvalue = (Double) entry.getValue();
					if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
						dvalue = DashboardUtils.FP_MISSING_VALUE;
					ArrayDouble.D1 mvar = new ArrayDouble.D1(1);
					mvar.set(0, dvalue);
					ncFile.write(var, mvar);
				}
				else {
					throw new IllegalArgumentException("unexpected unknown metadata type: " + dtype.toString());
				}
			}

            if ( !hasTime ) {
                Variable tvar = ncFile.findVariable("time");
                ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
                Double[] sampleTimes = _stdUser.getSampleTimes();
				for (int j = 0; j < numSamples; j++) {
					dvar.set(j, sampleTimes[j].doubleValue());
				}
                ncFile.write(tvar, dvar);
            }
			for (int k = 0; k < _stdUser.getNumDataCols(); k++) {
				DashDataType<?> dtype = dataTypes.get(k);
				varName = dtype.getVarName();
				var = ncFile.findVariable(varName);
				if ( var == null ) {
//					throw new RuntimeException("Unexpected failure to find ncFile variable " + varName);
                    System.out.println("Unexpected failure to find ncFile data variable " + varName);
                    logger.warn("Unexpected failure to find ncFile data variable " + varName);
                    continue;
				}
				if ( dtype instanceof StringDashDataType ) {
					// Data Strings
					ArrayChar.D2 dvar = new ArrayChar.D2(numSamples, maxchar);
					for (int j = 0; j < numSamples; j++) {
    					String dvalue = (String) _stdUser.getStdVal(j, k);
    					if ( dvalue == null )
    						dvalue = DashboardUtils.STRING_MISSING_VALUE;
        					dvar.setString(j, dvalue);
					}
					ncFile.write(var, dvar);
				} else if ( dtype instanceof CharDashDataType ) {
					// Data Characters
					ArrayChar.D2 dvar = new ArrayChar.D2(numSamples, 1);
					for (int j = 0; j < numSamples; j++) {
						Character dvalue = (Character) _stdUser.getStdVal(j, k);
						if ( dvalue == null )
							dvalue = DashboardUtils.CHAR_MISSING_VALUE;
						dvar.set(j, 0, dvalue);
					}
					ncFile.write(var, dvar);
				}
				else if ( dtype instanceof IntDashDataType ) {
					// Data Integers
					ArrayInt.D1 dvar = new ArrayInt.D1(numSamples);
					for (int j = 0; j < numSamples; j++) {
						Integer dvalue = (Integer) _stdUser.getStdVal(j, k);
						if ( dvalue == null )
							dvalue = DashboardUtils.INT_MISSING_VALUE;
						dvar.set(j, dvalue);
					}
					ncFile.write(var, dvar);
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					// Data Doubles
					ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
					for (int j = 0; j < numSamples; j++) {
						Double dvalue = (Double) _stdUser.getStdVal(j, k);
						if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
							dvalue = DashboardUtils.FP_MISSING_VALUE;
						dvar.set(j, dvalue);
					}
					ncFile.write(var, dvar);
				}
				else {
					throw new IllegalArgumentException("unexpected unknown data type: " + dtype.toString());
				}
			}
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	}
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgncFile#typeSpecificInitializations()
     */
    @Override
    protected void doFeatureTypeSpecificInitialization() {
        // TODO Auto-generated method stub
    }
        
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgncFile#createDimensions(ucar.nc2.NetcdfFileWriter)
    @Override
    protected void createDimensions(NetcdfFileWriter ncFile) {
        // TODO Auto-generated method stub
			// According to the CF standard if a file only has one trajectory, 
			// then the trajectory dimension is not necessary.
			// However, who knows what would break downstream from this process without it...
			Dimension traj = ncFile.addDimension(null, "trajectory", 1);

			// There will be a number of trajectory variables of type character from the metadata.
			// Which is the longest?
			int maxchar = _metadata.getMaxStringLength();
			Dimension stringlen = ncFile.addDimension(null, "string_length", maxchar);
			List<Dimension> trajStringDims = new ArrayList<Dimension>();
			trajStringDims.add(traj);
			trajStringDims.add(stringlen);

			Dimension charlen = ncFile.addDimension(null, "char_length", 1);
			List<Dimension> trajCharDims = new ArrayList<Dimension>();
			trajCharDims.add(traj);
			trajCharDims.add(charlen);

			List<Dimension> trajDims = new ArrayList<Dimension>();
			trajDims.add(traj);

			int numSamples = _stdUser.getNumSamples();
			Dimension obslen = ncFile.addDimension(null, "obs", numSamples);
			List<Dimension> dataDims = new ArrayList<Dimension>();
			dataDims.add(obslen);

			List<Dimension> charDataDims = new ArrayList<Dimension>();
			charDataDims.add(obslen);
			charDataDims.add(charlen);

			List<Dimension> stringDataDims = new ArrayList<Dimension>();
			stringDataDims.add(obslen);
			stringDataDims.add(stringlen);

    }
     */
        
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgncFile#addAttributes(ucar.nc2.NetcdfFileWriter)
    @Override
    protected void addAttributes(NetcdfFileWriter ncFile) {
        // TODO Auto-generated method stub
    }
     */
        
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgncFile#addTypeVariables(ucar.nc2.NetcdfFileWriter)
     */
    @Override
    protected void addFeatureTypeVariables(NetcdfFileWriter ncFile) {
        Variable var = addTimeVariable(ncFile, SAMPLE_TIME_VARNAME, "Time", _obsDimList, true);
        _typeSpecificVariables.put(var.getShortName(), var);
        var = addLatitudeVariable(ncFile, SAMPLE_LAT_VARNAME, "Sample Latitude", _obsDimList, true);
        _typeSpecificVariables.put(var.getShortName(), var);
        var = addLongitudeVariable(ncFile, SAMPLE_LON_VARNAME , "Sample Longitude", _obsDimList, true);
        _typeSpecificVariables.put(var.getShortName(), var);
        if ( _stdUser.hasDataColumn(SAMPLE_DEPTH_VARNAME)) {
            var = addDepthVariable(ncFile, SAMPLE_DEPTH_VARNAME, "Sample Depth", _obsDimList, true);
            _typeSpecificVariables.put(var.getShortName(), var);
        } else if ( _stdUser.hasDataColumn(SAMPLE_PRESSURE_VARNAME)) {
            var = addPressureVariable(ncFile, SAMPLE_PRESSURE_VARNAME, "Sample Pressure", _obsDimList, true);
            _typeSpecificVariables.put(var.getShortName(), var);
        }
    }
        
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgncFile#writeTypeVariables(ucar.nc2.NetcdfFileWriter)
     */
    @Override
	protected void writeFeatureTypeVariables(NetcdfFileWriter ncFile) throws IOException, InvalidRangeException {
		Variable vFeaturId = getVariable(ncFile, _featureType.dsgTypeName());
		ArrayChar.D2 aFeaturId = new ArrayChar.D2(getNumFeatures(), _maxStrLen);
		Variable vRowCount = getVariable(ncFile, SAMPLES_PER_FEATURE_VARNAME);
		ArrayInt.D1 aRowCount = new ArrayInt.D1(getNumFeatures());

        // XXX TODO: Multiple trajectories in one file?
        aFeaturId.setString(0, _stdUser.getDatasetName());
        aRowCount.set(0, _stdUser.numSamples);

		ncFile.write(vFeaturId, aFeaturId);
		ncFile.write(vRowCount, aRowCount);
        writeObsCoordinateVaiables(ncFile);
		ncFile.flush();
	}
    
    private void writeObsCoordinateVaiables(NetcdfFileWriter ncFile) 
            throws IOException, InvalidRangeException {
        Double[] times = _stdUser.getSampleTimes();
        Double[] lats = _stdUser.getSampleLatitudes();
        Double[] lons = _stdUser.getSampleLongitudes();
        Double[] presDep = null;
        Variable v_time = getVariable(ncFile, SAMPLE_TIME_VARNAME);
        ArrayDouble.D1 a_time = new ArrayDouble.D1(_numObservations);
        Variable v_lat = getVariable(ncFile, SAMPLE_LAT_VARNAME);
        ArrayDouble.D1 a_lat = new ArrayDouble.D1(_numObservations);
        Variable v_lon = getVariable(ncFile, SAMPLE_LON_VARNAME);
        ArrayDouble.D1 a_lon = new ArrayDouble.D1(_numObservations);
        Variable v_PresDep = null;
        ArrayDouble.D1 a_PresDep = null;
        if ( _stdUser.hasSampleDepth()) {
            presDep = _stdUser.getSampleDepths();
            v_PresDep = getVariable(ncFile, SAMPLE_DEPTH_VARNAME);
            a_PresDep = new ArrayDouble.D1(_numObservations);
        } else if ( _stdUser.hasSamplePressure()) {
            presDep = _stdUser.getSamplePressures();
            v_PresDep = getVariable(ncFile, SAMPLE_DEPTH_VARNAME);
            a_PresDep = new ArrayDouble.D1(_numObservations);
        }
        
        for (int row=0; row<_numObservations; row++) {
            a_time.set(row, times[row].doubleValue());
            a_lat.set(row, lats[row].doubleValue());
            a_lon.set(row, lons[row].doubleValue());
            if ( presDep != null ) {
                a_PresDep.set(row, presDep[row].doubleValue());
            }
        }
        ncFile.write(v_time, a_time);
        ncFile.write(v_lat, a_lat);
        ncFile.write(v_lon, a_lon);
        if ( presDep != null ) {
            ncFile.write(v_PresDep, a_PresDep);
        }
    }
    
        
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgncFile#getNumFeatures()
     */
    @Override
    protected int getNumFeatures() {
        return 1; //      XXX TODO number of trajectories
    }
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.dsg.DsgncFile#checkTypeSpecificIndeces()
     */
    @Override
    protected void checkFeatureTypeSpecificIndeces() {
        // TODO Auto-generated method stub
        
    }

}
