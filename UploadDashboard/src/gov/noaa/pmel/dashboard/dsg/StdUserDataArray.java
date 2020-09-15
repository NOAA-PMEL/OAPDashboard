/**
 * 
 */
package gov.noaa.pmel.dashboard.dsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeSet;

import gov.noaa.pmel.dashboard.datatype.CharDashDataType;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.DoubleDashDataType;
import gov.noaa.pmel.dashboard.datatype.IntDashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.datatype.StringDashDataType;
import gov.noaa.pmel.dashboard.datatype.TimestampConverter;
import gov.noaa.pmel.dashboard.datatype.ValueConverter;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.DataLocation;
import gov.noaa.pmel.dashboard.shared.QCFlag;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;
import gov.noaa.pmel.oads.util.StringUtils;

/**
 * A 2-D array of objects corresponding to the standardized values of string values 
 * provided by the user.  Also contains 1-D arrays of information describing each 
 * data column.
 * 
 * @author Karl Smith
 */
public class StdUserDataArray extends StdDataArray {

	public static final String INCONSISTENT_NUMBER_OF_DATA_VALUES_MSG = 
			"inconstistent number of data values";

	private String[] userColNames;
	private String[] userUnits;
	private String[] userMissVals;
	/**
	 * Tri-state flag representing a column's standarization state.
	 * Boolean.TRUE = column's data has been standardized,<br/>
	 * Boolean.FALSE = column's data has not been standardized (FALSE),<br/>
	 * null = column's data cannot be standardized<br/>
	 * per the check in {@link #getStdVal(int, int)}
	 */
	private Boolean[] standardized;
	private ArrayList<ADCMessage> stdMsgList;
	int woceAutocheckIndex;
	
//	public StdUserDataArray(StdUserDataArray other, KnownDataTypes knownTypes) {
//		super(other, knownTypes);
//		userUnits = other.userUnits;
//		userMissVals = other.userMissVals;
//		userColNames = other.userColNames;
//		stdMsgList = other.stdMsgList;
//		standardized = other.standardized;
//		datasetId = other.datasetId;
//	}
	
	/**
	 * Create from the user's data column descriptions, data strings,  
	 * data row numbers, and data check flags given for this dataset.  Any 
	 * data columns types matching {@link DashboardServerUtils#UNKNOWN} 
	 * or {@link DashboardServerUtils#OTHER} are ignored; 
	 * {@link #isUsableIndex(int)} will return false, and 
	 * {@link #getStdVal(int, int)} will throw an exception 
	 * for data columns of these types.  
	 * <br /><br />
	 * The list of automated data check messages describing problems 
	 * (critical errors) encountered when standardizing the data can 
	 * be retrieved using {@link #getStandardizationMessages()}.
	 * <br /><br />
	 * No bounds checking of standardized data values is performed.
	 * 
	 * @param dataset
	 * 		dataset, with user's strings data, to use
	 * @param knownTypes
	 * 		all known user data types
	 * @throws IllegalArgumentException
	 * 		if there are no data values,
	 * 		if a data column description is not a known user data type,
	 * 		if a required unit conversion is not supported, or
	 * 		if a standardizer for a given data type is not known
	 */
	public StdUserDataArray(DashboardDatasetData dataset, 
							KnownDataTypes knownTypes) throws IllegalArgumentException {
		super(dataset.getRecordId(), dataset.getDataColTypes(), knownTypes);
		
		// Add the user's units, missing values, and user column names
		userUnits = new String[numDataCols];
		userMissVals = new String[numDataCols];
		userColNames = new String[numDataCols];
		ArrayList<DataColumnType> dataColumnTypes = dataset.getDataColTypes();
		int numUserDataCols = dataColumnTypes.size();
		ArrayList<String> names = dataset.getUserColNames();
		if ( names.size() != numUserDataCols )
			throw new IllegalArgumentException("number of user column names (" + 
					names.size() + ") does not match the number of user column types (" + 
					numUserDataCols + ")");
		for (int k = 0; k < numUserDataCols; k++) {
			DataColumnType dataColType = dataColumnTypes.get(k);
			userUnits[k] = dataColType.getUnits().get(dataColType.getSelectedUnitIndex());
			if ( DashboardUtils.STRING_MISSING_VALUE.equals(userUnits[k]) )
				userUnits[k] = null;
			userMissVals[k] = dataColType.getSelectedMissingValue();
			if ( DashboardUtils.STRING_MISSING_VALUE.equals(userMissVals[k]) )
				userMissVals[k] = null;
			userColNames[k] = names.get(k);
		}
		// the StdDataArray constructor used adds SAMPLE_NUMBER and WOCE_AUTOCHECK
		woceAutocheckIndex = -1;
		for (int k = numUserDataCols; k < numDataCols; k++) {
			if ( DashboardServerUtils.WOCE_AUTOCHECK.typeNameEquals(dataTypes[k]) )
				woceAutocheckIndex = k;
			// use the standard unit, a default missing value string, 
			// and the type display name for these added types
			userUnits[k] = dataTypes[k].getUnits().get(0);
			if ( DashboardUtils.STRING_MISSING_VALUE.equals(userUnits[k]) )
				userUnits[k] = null;
			userMissVals[k] = null;
			userColNames[k] = dataTypes[k].getDisplayName();
		}

		standardized = new Boolean[numDataCols];
		for (int k = 0; k < numDataCols; k++)
			standardized[k] = Boolean.FALSE;

		ArrayList<ArrayList<String>> dataVals = dataset.getDataValues();
		if ( dataVals.isEmpty() )
			throw new IllegalArgumentException("no data values given");
		numSamples = dataVals.size();

		ArrayList<Integer> rowNums = dataset.getRowNums();
		if ( rowNums.size() != numSamples )
			throw new IllegalArgumentException("number of row numbers (" + 
					rowNums.size() + ") does not match the number of samples (" + 
					numSamples + ")");

		stdObjects = new Object[numSamples][numDataCols];
		stdMsgList = new ArrayList<ADCMessage>();

		// Create a 2-D array of these Strings for efficiency
		String[][] strDataVals = new String[numSamples][numDataCols];
		int woceIdx = -1;
		for (int rowIdx = 0; rowIdx < numSamples; rowIdx++) {
			ArrayList<String> rowVals = dataVals.get(rowIdx);
			if ( rowVals.size() != numUserDataCols ) {
				// Generate a general message for this row - in case too long
				ADCMessage msg = new ADCMessage();
				msg.setSeverity(Severity.CRITICAL);
				msg.setRowIndex(rowIdx);
				msg.setGeneralComment(INCONSISTENT_NUMBER_OF_DATA_VALUES_MSG);
				msg.setDetailedComment(INCONSISTENT_NUMBER_OF_DATA_VALUES_MSG + "; " + 
						numUserDataCols + " expected but " + rowVals.size() + " found");
				stdMsgList.add(msg);
				// Continue on, assuming the missing values are at the end
			}
			for (int col = 0; col < numUserDataCols; col++) {
				try {
					strDataVals[rowIdx][col] = rowVals.get(col);
				} catch ( IndexOutOfBoundsException ex ) {
					// Setting it to null will generate a "no value given" message
					strDataVals[rowIdx][col] = null;
				}
			}
			for (int col = numUserDataCols; col < numDataCols; col++) {
				if ( DashboardServerUtils.SAMPLE_NUMBER.typeNameEquals(dataTypes[col]) ) {
					strDataVals[rowIdx][col] = rowNums.get(rowIdx).toString();
				}
				else if ( DashboardServerUtils.WOCE_AUTOCHECK.typeNameEquals(dataTypes[col]) ) {
					// Default to acceptable; update afterwards
					strDataVals[rowIdx][col] = DashboardServerUtils.FLAG_ACCEPTABLE.toString();
					woceIdx = col;
				}
				else {
					throw new IllegalArgumentException("unexpected unknown added data types");
				}
			}
		}
		// Add the automatic data checker WOCE flags
		if ( woceIdx >= 0 ) {
			for ( QCFlag flag : dataset.getCheckerFlags() ) {
				if ( DashboardServerUtils.WOCE_AUTOCHECK.getVarName().equals(flag.getFlagName()) ) {
					Integer j = flag.getRowIndex();
					if ( ! DashboardUtils.INT_MISSING_VALUE.equals(j) ) {
						strDataVals[j][woceIdx] = flag.getFlagValue().toString();
					}
				}
			}
		}

		// Standardize data columns
		boolean needsAnotherPass;
		do {
			needsAnotherPass = false;
			for (int col = 0; col < numDataCols; col++) {
				DashDataType<?> colType = dataTypes[col];
				if ( standardized[col] == null || standardized[col].booleanValue()) {
					continue;
				}
				if ( DashboardServerUtils.UNKNOWN.typeNameEquals(colType) ||
						DashboardServerUtils.OTHER.typeNameEquals(colType) ) {
					for (int row = 0; row < numSamples; row++) {
						stdObjects[row][col] = null;
					}
					standardized[col] = null;
				}
				else {
					try {
						ValueConverter<?> stdizer = colType.getStandardizer(userUnits[col], userMissVals[col], this);
                        boolean lastRowHadError = false;
                        String lastVal = "";
                        String origVal = "";
						for (int row = 0; row < numSamples; row++) {
							if ( ! Boolean.TRUE.equals(standardized[col])) {
								try {
                                    origVal = strDataVals[row][col];
                                    Object stdVal = stdizer.convertValueOf(origVal, row);
									stdObjects[row][col] = stdVal;
                                    if ( stdVal == null && StringUtils.emptyOrNull(origVal) && 
                                         stdizer instanceof TimestampConverter ) {
    									ADCMessage msg = new ADCMessage();
    									msg.setSeverity(Severity.CRITICAL);
    									msg.setRowIndex(row);
    									msg.setColIndex(col);
    									msg.setColName(userColNames[col]);
    									msg.setGeneralComment("Missing value");
                                        msg.setDetailedComment("Missing date/time value.");
    									stdMsgList.add(msg);
                                    } else {
                                        lastRowHadError = false;
                                    }
								} catch ( IllegalArgumentException ex ) {
									ADCMessage msg = null;
                                    if ( stdizer instanceof TimestampConverter) {
                                        if ( lastRowHadError && lastVal.equals(origVal) ) {
                                            continue;
                                        }
    									msg = new ADCMessage();
                                        if (row != 0) { // && ! lastRowHadError ) {
        									msg.setRowIndex(row);
                                        }
                                    } else {
    									msg = new ADCMessage();
    									msg.setRowIndex(row);
                                    }
									stdObjects[row][col] = null;
                                    if ( msg != null ) {
    									msg.setSeverity(Severity.CRITICAL);
    									msg.setColIndex(col);
    									msg.setColName(userColNames[col]);
    									msg.setGeneralComment(ex.getMessage());
   										msg.setDetailedComment(ex.getMessage());
    									stdMsgList.add(msg);
                                    }
                                    lastRowHadError = true;
								}
							}
                            lastVal = origVal != null ? origVal : "";
						}
						standardized[col] = Boolean.TRUE;
					} catch ( IllegalStateException ex ) {
						standardized[col] = Boolean.FALSE;
						needsAnotherPass = true;
					}
				}
			}
		} while ( needsAnotherPass );
	}

	/**
	 * Check for missing longitude, latitude, depth, and time columns 
	 * or data values.  Any problems found generate messages that are added 
	 * to the internal list of messages.
	 * 
	 * @return
	 * 		the sample times for the data;  may be null if there was incomplete 
	 * 		specification of sample time, or may contain null values if there 
	 * 		were problems computing the sample time
	 */
	public boolean checkMissingLonLatTime() {
		boolean isOk = true;
		try {
			Double[] longitudes = getSampleLongitudes();
			for (int rowIdx = 0; rowIdx < numSamples; rowIdx++) {
				if ( longitudes[rowIdx] == null ) {
					isOk = false;
					ADCMessage msg = new ADCMessage();
					msg.setSeverity(Severity.CRITICAL);
					msg.setRowIndex(rowIdx);
					msg.setColIndex(longitudeIndex);
					msg.setColName(userColNames[longitudeIndex]);
					String comment = "missing longitude";
					msg.setGeneralComment(comment);
					msg.setDetailedComment(comment);
					stdMsgList.add(msg);
				}
			}
		} catch ( Exception ex ) {
			isOk = false;
			ADCMessage msg = new ADCMessage();
			msg.setSeverity(Severity.CRITICAL);
			String comment = "no longitude column";
			msg.setGeneralComment(comment);
			msg.setDetailedComment(comment);
			stdMsgList.add(msg);
		}

		try {
			Double[] latitudes = getSampleLatitudes();
			for (int rowIdx = 0; rowIdx < numSamples; rowIdx++) {
				if ( latitudes[rowIdx] == null ) {
					isOk = false;
					ADCMessage msg = new ADCMessage();
					msg.setSeverity(Severity.CRITICAL);
					msg.setRowIndex(rowIdx);
					msg.setColIndex(latitudeIndex);
					msg.setColName(userColNames[latitudeIndex]);
					String comment = "missing latitude";
					msg.setGeneralComment(comment);
					msg.setDetailedComment(comment);
					stdMsgList.add(msg);
				}
			}
		} catch ( Exception ex ) {
			isOk = false;
			ADCMessage msg = new ADCMessage();
			msg.setSeverity(Severity.CRITICAL);
			String comment = "no latitude column";
			msg.setGeneralComment(comment);
			msg.setDetailedComment(comment);
			stdMsgList.add(msg);
		}

//	    DashDataType<?> pressureColumn = findDataColumn("water_pressure");
//	    DashDataType<?> depthColumn = findDataColumn("sample_depth");
//	    if ( depthColumn != null ) {
//			try {
//				Double[] depths = getSampleDepths();
//				for (int rowIdx = 0; rowIdx < numSamples; rowIdx++) {
//					if ( depths[rowIdx] == null ) {
//						isOk = pressureColumn != null;
//						ADCMessage msg = new ADCMessage();
//						msg.setSeverity(pressureColumn == null ? Severity.ERROR : Severity.WARNING);
//						msg.setRowIndex(rowIdx);
//						msg.setColIndex(sampleDepthIndex);
//						msg.setColName(userColNames[sampleDepthIndex]);
//						String comment = "missing sample depth";
//						msg.setGeneralComment(comment);
//						msg.setDetailedComment(comment);
//						stdMsgList.add(msg);
//					}
//				}
//			} catch ( Exception ex ) {
//			    ex.printStackTrace();
//			}
//		} else if ( pressureColumn == null ) {
//			isOk = false;
//			ADCMessage msg = new ADCMessage();
//			msg.setSeverity(Severity.CRITICAL);
//			String comment = "no sample depth column";
//			msg.setGeneralComment(comment);
//			msg.setDetailedComment(comment);
//			stdMsgList.add(msg);
//		}

		Double[] times = null;
		try {
			times = getSampleTimes();
			for (int rowIdx = 0; rowIdx < numSamples; rowIdx++) {
				if ( times[rowIdx] == null ) {
					isOk = false;
//					ADCMessage msg = new ADCMessage();
//					msg.setSeverity(Severity.CRITICAL);
//					msg.setRowIndex(rowIdx);
//					String comment = "incomplete sample date/time specification";
//					msg.setGeneralComment("Bad date/time value");
//					msg.setDetailedComment(comment);
//					stdMsgList.add(msg);
				}
			}
		} catch ( Exception ex ) {
			isOk = false;
			ex.printStackTrace();
			ADCMessage msg = new ADCMessage();
			msg.setSeverity(Severity.CRITICAL);
			String comment = "incomplete columns specifying sample date/time";
			msg.setGeneralComment(comment);
			msg.setDetailedComment(ex.getMessage());
			stdMsgList.add(msg);
		}

		return isOk;
	}

	/**
	 * Reorders the data rows as best possible so that the data is
	 * 		(1) ascending in time (old to new),
	 * 		(2) ascending in longitude
	 * 		(3) ascending in latitude
	 * 		(4) ascending in depth (shallow to deep)
	 * 		(5) original row number
	 * Missing data columns (lon/lat/depth/time) will be treated 
	 * as an array of missing values.  Missing values in an array 
	 * will be ordered such that they appear before valid values.
	 * 
	 * @param times
	 * 		sample times to be used for this data array; 
	 * 		can be null to indicate sample times are not fully specified
	 */
	public void reorderData(Double[] times) {
		Double[] longitudes;
		try {
			longitudes = getSampleLongitudes();
		} catch ( Exception ex ) {
			longitudes = null;
		}
		Double[] latitudes;
		try {
			latitudes = getSampleLatitudes();
		} catch ( Exception ex ) {
			latitudes = null;
		}
		Double[] depths;
		try {
			depths = getSampleDepths();
		} catch ( Exception ex ) {
			depths = null;
		}
		
		TreeSet<DataLocation> orderedSet = new TreeSet<DataLocation>();
		for (int rowIdx = 0; rowIdx < numSamples; rowIdx++) {
			DataLocation dataLoc = new DataLocation();
			// Assign the row index instead of the number
			dataLoc.setRowIndex(rowIdx);
			if ( longitudes != null )
				dataLoc.setLongitude(longitudes[rowIdx]);
			if ( latitudes != null )
				dataLoc.setLatitude(latitudes[rowIdx]);
			if ( depths != null )
				dataLoc.setDepth(depths[rowIdx]);
			if ( times != null ) {
				Double timeValSecs = times[rowIdx];
				if ( timeValSecs != null )
					dataLoc.setDataDate( new Date(Math.round(timeValSecs * 1000.0)) );
			}
			// Leave dataValue as the missing value and add to the ordered set
			if ( ! orderedSet.add(dataLoc) )
				throw new RuntimeException("Unexpected duplicate data location with row number");
		}

		// Reorder the rows according to the ordering in orderedSet
		// Just assign the new order of the object arrays; no need to duplicate the objects themselves
		Object[][] orderedRows = new Object[numSamples][];
		int rowIdx = 0;
		for ( DataLocation dataLoc : orderedSet ) {
			// getRowNumber returns the row index assigned above
			orderedRows[rowIdx] = stdObjects[dataLoc.getRowIndex()];
			rowIdx++;
		}
		// Update the array of array of objects to the new ordering
		stdObjects = orderedRows;
        // force refetch of sample times
        _sampleTimes = null;
	}

	/**
	 * Checks that all values given (not missing values) are within the 
	 * acceptable range for that data type.  Any problems found generate 
	 * (error or warning) messages that are added to the internal list of 
	 * messages.
	 */
	public void checkBounds() {
		for (int k = 0; k < numDataCols; k++) {
			DashDataType<?> dtype = dataTypes[k];

			if ( dtype instanceof StringDashDataType ) {
				StringDashDataType strtype = (StringDashDataType) dtype;
				for (int j = 0; j < numSamples; j++) {
					ADCMessage msg = strtype.boundsCheckStandardValue(
											(String) stdObjects[j][k]);
					if ( msg != null ) {
						msg.setRowNumber(j+1);
						msg.setColNumber(k+1);
						msg.setColName(userColNames[k]);
						stdMsgList.add(msg);
					}
				}
			}
			else if ( dtype instanceof CharDashDataType ) {
				CharDashDataType chartype = (CharDashDataType) dtype;
				for (int j = 0; j < numSamples; j++) {
					ADCMessage msg = chartype.boundsCheckStandardValue(
											(Character) stdObjects[j][k]);
					if ( msg != null ) {
						msg.setRowNumber(j+1);
						msg.setColNumber(k+1);
						msg.setColName(userColNames[k]);
						stdMsgList.add(msg);
					}
				}
			}
			else if ( dtype instanceof IntDashDataType ) {
				IntDashDataType inttype = (IntDashDataType) dtype;
				for (int j = 0; j < numSamples; j++) {
					ADCMessage msg = inttype.boundsCheckStandardValue(
											(Integer) stdObjects[j][k]);
					if ( msg != null ) {
						msg.setRowNumber(j+1);
						msg.setColNumber(k+1);
						msg.setColName(userColNames[k]);
						stdMsgList.add(msg);
					}
				}
			}
			else if ( dtype instanceof DoubleDashDataType ) {
				DoubleDashDataType dbltype = (DoubleDashDataType) dtype;
				for (int j = 0; j < numSamples; j++) {
					ADCMessage msg = dbltype.boundsCheckStandardValue(
											(Double) stdObjects[j][k]);
					if ( msg != null ) {
						msg.setRowNumber(j+1);
						msg.setColNumber(k+1);
						msg.setColName(userColNames[k]);
						stdMsgList.add(msg);
					}
				}
			}
			else {
				throw new IllegalArgumentException("unexpected data type encountered " + 
						"in bounds checking: " + dtype);
			}
		}
	}

	/**
	 * @return
	 * 		the list of automated data check messages describing 
	 * 		problems detected in the data.  The messages that are 
	 * 		in this list comes from the constructor as well as any
	 * 		check methods that were called.
	 */
	public ArrayList<ADCMessage> getStandardizationMessages() {
		return stdMsgList;
	}

	/**
	 * Adds a new data standardization message to the message list.
	 * @param msg The standardization message to add.
	 */
	public void addStandardizationMessage(ADCMessage msg) {
		stdMsgList.add(msg);
	}
	
	/**
	 * Determines is this data column is an appropriate index.
	 * Checks that the value is in the appropriate range and 
	 * that the column with this index has been standardized.
	 * 
	 * @param idx
	 * 		index to test
	 * @return
	 * 		if the index is valid
	 */
	@Override
	public boolean isUsableIndex(int idx) {
		if ( idx < 0 )
			return false;
		if ( idx >= numDataCols )
			return false;
		return Boolean.TRUE.equals(standardized[idx]);
	}

	public boolean isStandardized(int columnIdx) {
	    return Boolean.TRUE.equals(standardized[columnIdx]);
	}
	
	/**
	 * Get the standard value object for the specified value (column index) 
	 * of the specified sample (row index).
	 * 
	 * @param sampleIdx
	 * 		index of the sample (row)
	 * @param columnIdx
	 * 		index of the data column
	 * @return
	 * 		standard value object; null is returned for "missing value" or
	 * 		values that could not be interpreted
	 * @throws IndexOutOfBoundsException
	 * 		if either the sample index or the column index is invalid
	 * @throws IllegalArgumentException 
	 * 		if the value cannot be standardized
	 * @throws IllegalStateException 
	 * 		if the value has not been standardized
	 */
	@Override
	public Object getStdVal(int sampleIdx, int columnIdx) 
			throws IndexOutOfBoundsException, IllegalArgumentException, IllegalStateException {
		if ( (sampleIdx < 0) || (sampleIdx >= numSamples) )
			throw new IndexOutOfBoundsException("sample index is invalid: " + sampleIdx);
		if ( (columnIdx < 0) || (columnIdx >= numDataCols) )
			throw new IndexOutOfBoundsException("data column index is invalid: " + columnIdx);
		if ( standardized[columnIdx] == null )
			throw new IllegalArgumentException("data column index " + columnIdx + " cannot be standardized");
		if ( ! standardized[columnIdx].booleanValue() )
			throw new IllegalStateException("value has not been standardized");
		return stdObjects[sampleIdx][columnIdx];
	}
	public Double getStdValAsDouble(int row, int columnIdx) 
			throws IndexOutOfBoundsException, IllegalArgumentException, IllegalStateException
	{
		try {
		    Double val = (Double)getStdVal(row, columnIdx);
            return val;
        } catch (ClassCastException cce) {
            throw new IllegalStateException("Unable to retrieve value as Double for column " + columnIdx);
		}
	}
	public Integer getStdValAsInteger(int row, int columnIdx) 
			throws IndexOutOfBoundsException, IllegalArgumentException, IllegalStateException
	{
		try {
		    Integer val = (Integer)getStdVal(row, columnIdx);
            return val;
        } catch (ClassCastException cce) {
            throw new IllegalStateException("Unable to retrieve value as Integer for column " + columnIdx);
		}
	}
	public String getStdValAsString(int row, int columnIdx) 
			throws IndexOutOfBoundsException, IllegalArgumentException, IllegalStateException
	{
        Object stdVal = getStdVal(row, columnIdx);
	    return stdVal != null ? String.valueOf(stdVal) : DashboardUtils.STRING_MISSING_VALUE;
	}
	
	public Object[] getStdValues(int columnIdx) 
			throws IndexOutOfBoundsException, IllegalArgumentException, IllegalStateException {
		Object[] values = new Object[numSamples];
		for (int row = 0; row < numSamples; row++ ) {
			values[row] = getStdVal(row, columnIdx);
		}
		return values;
	}

    public String[] getStdValuesAsString(int columnIdx)  
			throws IndexOutOfBoundsException, IllegalArgumentException, IllegalStateException {
		String[] values = new String[numSamples];
		for (int row = 0; row < numSamples; row++ ) {
            Object stdVal = getStdVal(row, columnIdx);
		    values[row] = stdVal != null ? String.valueOf(stdVal) : DashboardUtils.STRING_MISSING_VALUE;
		}
		return values;
	}
    /** 
     * May return nulls.
     * 
     * @param columnIdx
     * @return
     * @throws IndexOutOfBoundsException
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     */
    public String[] getAllValuesAsString(int columnIdx)  
			throws IndexOutOfBoundsException, IllegalArgumentException, IllegalStateException {
		String[] values = new String[numSamples];
		for (int row = 0; row < numSamples; row++ ) {
			try {
                Object stdVal = stdObjects[row][columnIdx];
			    values[row] = stdVal != null ? String.valueOf(getStdVal(row, columnIdx)) : null;
            } catch (ClassCastException cce) {
                throw new IllegalStateException("Unable to retrieve values as integers for column " + columnIdx);
			}
		}
		return values;
	}
	public Integer[] getStdValuesAsInteger(int columnIdx) 
			throws IndexOutOfBoundsException, IllegalArgumentException, IllegalStateException {
		Integer[] values = new Integer[numSamples];
		for (int row = 0; row < numSamples; row++ ) {
			try {
			    values[row] = (Integer)getStdVal(row, columnIdx);
            } catch (ClassCastException cce) {
                throw new IllegalStateException("Unable to retrieve values as integers for column " + columnIdx);
			}
		}
		return values;
	}

	public Double[] getStdValuesAsDouble(int columnIdx) 
			throws IndexOutOfBoundsException, IllegalArgumentException, IllegalStateException {
		Double[] values = new Double[numSamples];
		for (int row = 0; row < numSamples; row++ ) {
            try {
    			values[row] = (Double)getStdVal(row, columnIdx);
            } catch (ClassCastException cce) {
                throw new IllegalStateException("Unable to retrieve values as doubles for column " + columnIdx);
            }
		}
		return values;
	}

//    public Object getStdValuesForType(DashDataType<?> type) throws Exception {
////        String typeName = "java.lang." + type.getDataClassName();
////        Class<?> dataType = Class.forName(typeName);
////        Object[] values = (Object[])Array.newInstance(dataType, numSamples);
//        Object[] stdValues = getStdValues(type.getStandardName());
////        for (int i = 0; i < numSamples; i++ ) {
////            values[i] = type.dataValueOf((String)stdValues[i]);
////        }
////        return values;
//        return type.standardValues(stdValues);
//    }
	public Object[] getStdValuesForUserType(String userTypeColumnName)  
			throws NoSuchFieldException, IllegalArgumentException, IllegalStateException {
		if ( (userTypeColumnName == null) || "".equals(userTypeColumnName.trim()))
			throw new NoSuchFieldException("data column name is invalid: " + userTypeColumnName);
		int columnIdx = findUserTypeColumn(userTypeColumnName);
		return getStdValues(dataTypes[columnIdx].getStandardName());
	}
	
	public Object[] getStdValues(String stdTypeColumnName) 
			throws NoSuchFieldException, IllegalArgumentException, IllegalStateException {
		if ( (stdTypeColumnName == null) || "".equals(stdTypeColumnName.trim()))
			throw new NoSuchFieldException("data column name is invalid: " + stdTypeColumnName);
		int columnIdx = findDataColumnIndex(stdTypeColumnName);
		if ( columnIdx == -1 ) {
		    throw new NoSuchFieldException("No standard type found for " + stdTypeColumnName);
		}
		Object[] values = new Object[numSamples];
		for (int row = 0; row < numSamples; row++ ) {
			values[row] = getStdVal(row, columnIdx);
		}
		return values;
	}

	public int findUserTypeColumn(String userTypeColumnName) throws NoSuchFieldException {
		int columnIdx = -1;
		for (int i = 0; i < numDataCols; i++ ) {
			if ( userColNames[i].equals(userTypeColumnName)) {
				columnIdx = i;
				break;
			}
		}
		if ( columnIdx == -1 ) {
			throw new NoSuchFieldException("No user type found for " + userTypeColumnName);
		}
		return columnIdx;
	}
	public String[] getUserColumnNames() {
	    return userColNames;
	}
	public String getUserColumnTypeName(int colIdx) {
		return userColNames[colIdx];
	}
	public String getUserColumnUnits(int colIdx) {
		return userUnits[colIdx];
	}
	public DashDataType<?> findDataColumn(String varName) {
		if ( dataTypeMap.containsKey(varName)) {
			return dataTypeMap.get(varName);
		}
		for (int i = 0; i < numDataCols; i++ ) {
			if ( dataTypes[i].typeNameEquals(varName)) {
				return dataTypes[i];
			}
		}
		return null;
	}
	/**
	 * Find the index of the column whose type matches the requested name.  Type name matching is based on
	 * gov.noaa.pmel.dashboard.datatype.DashDataType.typeNameEquals()
	 * 
	 * @see gov.noaa.pmel.dashboard.datatype.DashDataType#typeNameEquals(String)
	 * 
	 * @param varName The type name of the column to be found.
	 * @return The 0-based index of the column matching the requested name, or -1 if not found.
	 * @throws NoSuchFieldException
	 */
	public int findDataColumnIndex(String varName) throws NoSuchFieldException {
		int columnIdx = -1;
		for (int i = 0; i < numDataCols; i++ ) {
			if ( dataTypes[i].typeNameEquals(varName)) {
				columnIdx = i;
				break;
			}
		}
//		if ( columnIdx == -1 ) {
//			throw new NoSuchFieldException("No standard type found for " + varName);
//		}
		return columnIdx;
	}

	/**
	 * @return
	 * 		if this standardized user data array has a WOCE_AUTOCHECK column
	 */
	public boolean hasWoceAutocheck() {
		if ( (woceAutocheckIndex < 0) || (woceAutocheckIndex >= numDataCols) )
			return false;
		return true;
	}

	/**
	 * Reset all values in the WOCE_AUTOCHECK column, if it exists,
	 * to {@link DashboardServerUtils#FLAG_ACCEPTABLE}.
	 */
	public void resetWoceAutocheck() {
		if ( (woceAutocheckIndex < 0) || (woceAutocheckIndex >= numDataCols) )
			return;
		for (int j = 0; j < numSamples; j++)
			stdObjects[j][woceAutocheckIndex] = DashboardServerUtils.FLAG_ACCEPTABLE;
	}

	/**
	 * Get the current WOCE_AUTOCHECK flag for a data sample (row).
	 * 
	 * @param sampleIdx
	 * 		index of the data sample (row) to check
	 * @return
	 * 		the current WOCE_AUTOCHECK flag for the data sample
	 * @throws IllegalArgumentException
	 * 		if the sample index is invalid, or
	 * 		if there is not WOCE_AUTOCHECK column
	 */
	public Character getWoceAutocheckFlag(int sampleIdx) throws IllegalArgumentException {
		if ( (sampleIdx < 0) || (sampleIdx >= numSamples) )
			throw new IndexOutOfBoundsException("sample index is invalid: " + sampleIdx);
		if ( (woceAutocheckIndex < 0) || (woceAutocheckIndex >= numDataCols) )
			throw new IllegalArgumentException("no WOCE autocheck column");
		return (Character) stdObjects[sampleIdx][woceAutocheckIndex];
	}

	public boolean hasCriticalError() {
		for (ADCMessage msg : stdMsgList) {
			if (Severity.CRITICAL.equals(msg.getSeverity())) {
				return true;
			}
		}
		return false;
	}
//	public boolean hasCriticalError(int colIdx) {
//		for (ADCMessage msg : stdMsgList) {
//			if (Severity.CRITICAL.equals(msg.getSeverity()) && 
//			    msg.getColNumber().equals((colIdx+1))) {
//				return true;
//			}
//		}
//		return false;
//	}
	/**
	 * Set the WOCE_AUTOCHECK flag for a data sample (row).
	 * 
	 * @param sampleIdx
	 * 		index of the data sample (row) to set
	 * @param newFlag
	 * 		WOCE flag to assign
	 * @throws IllegalArgumentException
	 * 		if the sample index is invalid,
	 * 		if there is not WOCE_AUTOCHECK column, or 
	 * 		if the flag given is not a valid WOCE flag
	 */
	public void setWoceAutocheck(int sampleIdx, Character newFlag) throws IllegalArgumentException {
		if ( (sampleIdx < 0) || (sampleIdx >= numSamples) )
			throw new IndexOutOfBoundsException("sample index is invalid: " + sampleIdx);
		if ( (woceAutocheckIndex < 0) || (woceAutocheckIndex >= numDataCols) )
			throw new IllegalArgumentException("no WOCE autocheck column");
		if ( ! ( DashboardServerUtils.FLAG_ACCEPTABLE.equals(newFlag) ||
				 DashboardServerUtils.WOCE_QUESTIONABLE.equals(newFlag) ||
				 DashboardServerUtils.WOCE_BAD.equals(newFlag) ) )
			throw new IllegalArgumentException("invalid WOCE flag value");
		stdObjects[sampleIdx][woceAutocheckIndex] = newFlag;
	}

	@Override
	public int hashCode() {
		final int prime = 37;
		int result = super.hashCode();
		result = prime * result + stdMsgList.hashCode();
		result = prime * result + woceAutocheckIndex;
		result = prime * result + Arrays.hashCode(standardized);
		result = prime * result + Arrays.hashCode(userMissVals);
		result = prime * result + Arrays.hashCode(userUnits);
		result = prime * result + Arrays.hashCode(userColNames);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( ! super.equals(obj) )
			return false;

		if ( ! ( obj instanceof StdUserDataArray ) )
			return false;
		StdUserDataArray other = (StdUserDataArray) obj;

		if ( ! stdMsgList.equals(other.stdMsgList) )
			return false;
		if ( woceAutocheckIndex != other.woceAutocheckIndex )
			return false;
		if ( ! Arrays.equals(standardized, other.standardized) )
			return false;
		if ( ! Arrays.equals(userColNames, other.userColNames) )
			return false;
		if ( ! Arrays.equals(userMissVals, other.userMissVals) )
			return false;
		if ( ! Arrays.equals(userUnits, other.userUnits) )
			return false;

		return true;
	}

	@Override
	public String toString() {
		String repr = "StdUserDataArray[numSamples=" + numSamples + 
				", numDataCols=" + numDataCols + 
				"' woceAutocheckIndex=" + woceAutocheckIndex;
		repr += ",\n  stdMsgList=[";
		boolean first = true;
		for ( ADCMessage msg : stdMsgList ) {
			if ( first )
				first = false;
			else
				repr += ",";
			repr += "\n    " + msg.toString();
		}
		repr += "\n  ]";
		repr += ",\n  userColNames=" + Arrays.toString(userColNames);
		repr += ",\n  userUnits=" + Arrays.toString(userUnits);
		repr += ",\n  userMissVals=" + Arrays.toString(userMissVals);
		repr += ",\n  standardized=" + Arrays.toString(standardized);
		String superRepr = super.toString();
		int idx = superRepr.indexOf('\n');
		repr += "," + superRepr.substring(idx);
		return repr;
	}

//	public String getExpoCode() {
//		if ( expoCode == null ) {
//			expoCode = findExpoCode();
//		}
//		return expoCode;
//	}
//
//	public void setExpoCode(String expoCode) {
//		this.expoCode = expoCode;
//	}
//	
//	private String findExpoCode() {
//		if ( expoIdx == null ) {
//			expoIdx = lookForDataColumnIndex("expocode");
//		}
//		if ( expoIdx != null ) {
//			return (String)getStdVal(0, expoIdx.intValue());
//		} else {
//			return generateExpoCode();
//		}
//	}
//	private synchronized String generateExpoCode() {
//		if (expoCode != null) { return expoCode; }
//		String code = null;
//		Integer shipCodeIdx = lookForDataColumnIndex("platform_code");
//		if ( shipCodeIdx == null ) {
//			throw new IllegalStateException("No platform_code column found to generate expocode.");
//		}
//		String shipCode = (String)getStdVal(0, shipCodeIdx.intValue());
//		Double[] times = getSampleTimes();
//		SortedSet<Double> orderedTimes = new TreeSet<>(Arrays.asList(times));
//		Double firstObs = orderedTimes.first();
//		Date firstObsDate = new Date((long)(firstObs.doubleValue()*1000));
//		String dateStr = DashboardServerUtils.formatUTC(firstObsDate, DashboardServerUtils.EXPO_DATE);
//		code = shipCode+dateStr;
//		return code;
//	}

//	public boolean hasRequiredColumns() {
//		boolean gotem = true;
//		if ( ! hasDate()) {
//			gotem = false;
//			ADCMessage msg = new ADCMessage();
//			msg.setSeverity(Severity.CRITICAL);
//			msg.setGeneralComment("missing column");
//			msg.setDetailedComment("The dataset does not identify the sample Date.");
//			addStandardizationMessage(msg);
//		}
//		if ( ! hasTime()) {
//			gotem = false;
//			ADCMessage msg = new ADCMessage();
//			msg.setSeverity(Severity.CRITICAL);
//			msg.setGeneralComment("missing column");
//			msg.setDetailedComment("The dataset does not identify the sample Time.");
//			addStandardizationMessage(msg);
//		}
//		if ( ! hasDataColumn(DashboardServerUtils.LATITUDE.getStandardName())) {
//			gotem = false;
//			ADCMessage msg = new ADCMessage();
//			msg.setSeverity(Severity.CRITICAL);
//			msg.setGeneralComment("missing column");
//			msg.setDetailedComment("The dataset does not identify the cast Latitude.");
//			addStandardizationMessage(msg);
//		}
//		if ( ! hasDataColumn(DashboardServerUtils.LONGITUDE.getStandardName())) {
//			gotem = false;
//			ADCMessage msg = new ADCMessage();
//			msg.setSeverity(Severity.CRITICAL);
//			msg.setGeneralComment("missing column");
//			msg.setDetailedComment("The dataset does not identify the cast Longitude.");
//			addStandardizationMessage(msg);
//		}
//		if ( ! ( hasDataColumn(DashboardServerUtils.SAMPLE_DEPTH.getStandardName()) || 
//				 hasDataColumn("water_pressure"))) {
//			gotem = false;
//			ADCMessage msg = new ADCMessage();
//			msg.setSeverity(Severity.CRITICAL);
//			msg.setGeneralComment("missing column");
//			msg.setDetailedComment("The dataset does not identify either the sample Depth or Pressure.");
//			addStandardizationMessage(msg);
//		}
////		if ( ! ( hasDataColumn(DashboardServerUtils.EXPO_CODE.getStandardName()) || 
////				 hasDataColumn(DashboardServerUtils.PLATFORM_CODE.getStandardName()))) {
////			gotem = false;
////			ADCMessage msg = new ADCMessage();
////			msg.setSeverity(Severity.CRITICAL);
////			msg.setGeneralComment("missing column");
////			msg.setDetailedComment("The dataset must identify either the Platform Code or the Cruise Expocode.");
////			addStandardizationMessage(msg);
////		}
//		
//		return gotem;
//	}

    public int getOriginalRowIndex(int rowIdx) {
        int sampleNo = ((Integer)getStdVal(rowIdx, numDataCols-2)).intValue();
        return sampleNo -1;
    }

    public ADCMessage messageFor(Severity severity, Integer row, Integer column, 
                                 String generalComment, String detailedComment) {
		ADCMessage msg = new ADCMessage();
		msg.setSeverity(severity);
        if ( row != null ) { msg.setRowIndex(row); }
		if ( column != null ) { 
		    msg.setColIndex(column); 
    		msg.setColName(userColNames[column.intValue()]);
		}
		msg.setGeneralComment(generalComment);
		msg.setDetailedComment(detailedComment);
        addTimeAndLocation(msg, row);
        return msg;
    }
	public void addTimeAndLocation(ADCMessage msg, Integer row) {
        if ( row == null ) { return; }
        int rowIdx = row.intValue();
        if ( hasTimeOfDay()) {
    		Date rowTime = getSampleTime(rowIdx);
            if ( rowTime != null ) {
        		msg.setTimestamp(DashboardServerUtils.formatUTC(rowTime));
            }
        }
        if ( hasLatitude()) {
            Double latitude = getSampleLatitude(rowIdx);
            if ( latitude != null ) {
        		msg.setLatitude(latitude);
            }
        }
            
        Double longitude = getSampleLongitude(rowIdx);
		msg.setLongitude(getSampleLongitudes()[rowIdx]);
	}
    /**
     * @param dataType
     * @return
     */
    public boolean checkForMissingValues(DashDataType dataType, Severity severity) {
        if ( ! hasDataColumn(dataType.getStandardName())) {
            return false;
        }
        boolean isOk = true;
		try {
            int columnIdx = findDataColumnIndex(dataType.getStandardName());
            String userColName = userColNames[columnIdx];
			Object[] values = getStdValues(columnIdx);
			for (int rowIdx = 0; rowIdx < numSamples; rowIdx++) {
                Object value = values[rowIdx];
				if ( value == null ||
                     value.equals(dataType.missingValue())) {
					isOk = false;
					ADCMessage msg = new ADCMessage();
					msg.setSeverity(severity);
					msg.setRowIndex(rowIdx);
					msg.setColIndex(columnIdx);
					msg.setColName(userColName);
					String comment = "missing value for " + userColName;
					msg.setGeneralComment(comment);
					msg.setDetailedComment(comment);
					stdMsgList.add(msg);
				}
			}
		} catch ( Exception ex ) {
			isOk = false;
			ADCMessage msg = new ADCMessage();
			msg.setSeverity(Severity.CRITICAL);
			String comment = "no column found for " + dataType.getVarName();
			msg.setGeneralComment(comment);
			msg.setDetailedComment(comment);
			stdMsgList.add(msg);
		}
        return isOk;
    }

    /**
     * @return
     */
    public boolean hasMissingTimeOrLocation() {
        if ( ! hasSampleTime()) { return true; }
        Double[] times = getSampleTimes();
        if ( times == null || times.length == 0 ) { return true; }
        for (Double d : times) {
            if ( d == null ) { // || d.equals(DashboardUtils.FP_MISSING_VALUE)) { // ??? XXX TODO: What about missing values?
                return true;
            }
        }
        if ( ! hasLatitude()) { return true; }
        Double[] lats = getSampleLatitudes();
        for (Double d : lats) {
            if ( d == null ) { // || d.equals(DashboardUtils.FP_MISSING_VALUE)) { // ??? XXX TODO: What about missing values?
                return true;
            }
        }
        if ( ! hasLongitude()) { return true; }
        Double[] lons = getSampleLongitudes();
        for (Double d : lons) {
            if ( d == null ) { // || d.equals(DashboardUtils.FP_MISSING_VALUE)) { // ??? XXX TODO: What about missing values?
                return true;
            }
        }
        return false;
    }
	
}
