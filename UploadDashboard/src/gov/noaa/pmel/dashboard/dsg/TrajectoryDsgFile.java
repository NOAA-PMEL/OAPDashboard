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
 * @author kamb
 *
 */
public class TrajectoryDsgFile extends DsgNcFile {
	
	private static final long serialVersionUID = 7939093989396422342L;

	public TrajectoryDsgFile(String filename) {
		super(filename);
	}
	public TrajectoryDsgFile(File parent, String child) {
		super(parent, child);
	}

	protected void checkIndeces(StdDataArray stddata) {
		// Quick check of data column indices already assigned in StdDataArray
		if ( ! stddata.hasLongitude() )
			throw new IllegalArgumentException("no longitude data column");
		if ( ! stddata.hasLatitude() )
			throw new IllegalArgumentException("no latitude data column");
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
		if ( metaData == null )
			throw new IllegalArgumentException("no metadata given");
		metadata = metaData;
		if ( fileData == null )
			throw new IllegalArgumentException("no data given");
		stddata = fileData;
		checkIndeces(stddata);
        if ( ! stddata.hasSampleTime()) {
			throw new IllegalArgumentException("no data sample time found");
        }

		
		try ( NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(Version.netcdf3, getPath()); ) {
			// According to the CF standard if a file only has one trajectory, 
			// then the trajectory dimension is not necessary.
			// However, who knows what would break downstream from this process without it...
			Dimension traj = ncfile.addDimension(null, "trajectory", 1);

			// There will be a number of trajectory variables of type character from the metadata.
			// Which is the longest?
			int maxchar = metadata.getMaxStringLength();
			Dimension stringlen = ncfile.addDimension(null, "string_length", maxchar);
			List<Dimension> trajStringDims = new ArrayList<Dimension>();
			trajStringDims.add(traj);
			trajStringDims.add(stringlen);

			Dimension charlen = ncfile.addDimension(null, "char_length", 1);
			List<Dimension> trajCharDims = new ArrayList<Dimension>();
			trajCharDims.add(traj);
			trajCharDims.add(charlen);

			List<Dimension> trajDims = new ArrayList<Dimension>();
			trajDims.add(traj);

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

			ncfile.addGroupAttribute(null, new Attribute("featureType", "Trajectory"));
			ncfile.addGroupAttribute(null, new Attribute("Conventions", "CF-1.6"));
			ncfile.addGroupAttribute(null, new Attribute("history", DSG_VERSION));

			// Add the "num_obs" variable which will be assigned using the number of data points
			Variable var = ncfile.addVariable(null, "num_obs", DataType.INT, trajDims);
			ncfile.addVariableAttribute(var, new Attribute("sample_dimension", "obs"));
			ncfile.addVariableAttribute(var, new Attribute("long_name", "Number of Observations"));
			ncfile.addVariableAttribute(var, new Attribute("missing_value", DashboardUtils.INT_MISSING_VALUE));
			ncfile.addVariableAttribute(var, new Attribute("_FillValue", DashboardUtils.INT_MISSING_VALUE));

			String varName;
			// Make netCDF variables of all the metadata and data variables
			for ( Entry<DashDataType<?>,Object> entry : metadata.getValuesMap().entrySet() ) {
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
					var = ncfile.addVariable(null, varName, DataType.CHAR, trajStringDims);
					// No missing_value, _FillValue, or units for strings
					addAttributes(ncfile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
					if ( DashboardServerUtils.DATASET_ID.typeNameEquals(dtype) ) {
						ncfile.addVariableAttribute(var, new Attribute("cf_role", "trajectory_id"));
					}
				}
				else if ( dtype instanceof CharDashDataType ) {
					// Metadata characters
					var = ncfile.addVariable(null, varName, DataType.CHAR, trajCharDims);
					// No missing_value, _FillValue, or units for characters
					addAttributes(ncfile, var, null, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
				}
				else if ( dtype instanceof IntDashDataType ) {
					// Metadata Integers
					var = ncfile.addVariable(null, varName, DataType.INT, trajDims);
					addAttributes(ncfile, var, DashboardUtils.INT_MISSING_VALUE, dtype.getDescription(), 
							dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
				}
				else if ( dtype instanceof DoubleDashDataType ) {
					// Metadata Doubles
					var = ncfile.addVariable(null, varName, DataType.DOUBLE, trajDims);
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

			for (  Entry<DashDataType<?>,Object> entry : metadata.getValuesMap().entrySet() ) {
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


}
