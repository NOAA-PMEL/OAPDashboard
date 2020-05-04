/**
 * 
 */
package gov.noaa.pmel.dashboard.dsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import gov.noaa.pmel.dashboard.datatype.CharDashDataType;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.DoubleDashDataType;
import gov.noaa.pmel.dashboard.datatype.IntDashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;
import gov.noaa.pmel.tws.util.StringUtils;
import static gov.noaa.pmel.dashboard.shared.DashboardUtils.*;

/**
 * A 2-D array of objects corresponding to the standardized values in a dataset, 
 * as well as 1-D arrays of information describing each data column.
 * 
 * @author Karl Smith
 */
public class StdDataArray {

	private String datasetName;
	
	protected int numSamples;
	protected int numDataCols;
	protected DashDataType<?>[] dataTypes;
	protected Object[][] stdObjects;
	protected int longitudeIndex;
	protected int latitudeIndex;
	protected int sampleDepthIndex;
	protected int samplePressureIndex;
	protected int datasetIdIndex;
	protected int stationIdIndex;
	protected int castIdIndex;
	protected int timestampIndex;
	protected int dateIndex;
	protected int yearIndex;
	protected int monthOfYearIndex;
	protected int dayOfMonthIndex;
	protected int timeOfDayIndex;
	protected int hourOfDayIndex;
	protected int minuteOfHourIndex;
	protected int secondOfMinuteIndex;
	protected int dayOfYearIndex;
	protected int secondOfDayIndex;

	protected Map<String, DashDataType<?>> dataTypeMap = new HashMap<>();
	
	/**
	 * Create and assign the 1-D arrays of data column types from 
	 * the given user's descriptions of the data column.  Appends 
	 * the non-user types {@link DashboardServerUtils#SAMPLE_NUMBER} 
	 * and {@link DashboardServerUtils#WOCE_AUTOCHECK}.  The 2-D 
	 * array of standard data objects is not created.
	 * 
	 * @param dataColumnTypes
	 * 		user's description of the data columns in each sample
	 * @param knownTypes
	 * 		all known user data types
	 * @throws IllegalArgumentException
	 * 		if there are no user data column descriptions, 
	 * 		if there are no known user data types, or 
	 * 		if a data column description is not a known user data type
	 */
	protected StdDataArray(String datasetName, List<DataColumnType> dataColumnTypes, 
			KnownDataTypes knownTypes) throws IllegalArgumentException {
        this.datasetName = datasetName;
		if ( (dataColumnTypes == null) || dataColumnTypes.isEmpty() )
			throw new IllegalArgumentException("no data column types given");
		if ( (knownTypes == null) || knownTypes.isEmpty() )
			throw new IllegalArgumentException("no known user data types given");
		numDataCols = dataColumnTypes.size();
		numSamples = 0;

		dataTypes = new DashDataType<?>[numDataCols+2];
		stdObjects = null;

		for (int k = 0; k < numDataCols; k++) {
			DataColumnType dataColType = dataColumnTypes.get(k);
			dataTypes[k] = knownTypes.getDataType(dataColType);
			if ( dataTypes[k] == null )
				throw new IllegalArgumentException("unknown data column type: " + 
						dataColType.getDisplayName());
			dataTypeMap.put(dataTypes[k].getVarName(), dataTypes[k]);
		}
		dataTypes[numDataCols] = DashboardServerUtils.SAMPLE_NUMBER;
		dataTypes[numDataCols+1] = DashboardServerUtils.WOCE_AUTOCHECK;
		numDataCols += 2;

		assignColumnIndicesOfInterest();
	}

	/**
	 * Create with the given data file data types for each column and the given 
	 * standardized data objects for each data column value (second index) in each 
	 * sample (first index).  The data types given must be known subclasses of 
	 * DashDataType valid for data files: {@link CharDashDataType}, 
	 * {@link IntDashDataType}, or {@link DoubleDashDataType}.
	 * 
	 * @param dataColumnTypes
	 * 		types for the data columns in each sample
	 * @param stdDataValues
	 * 		standard values; the value at stdDataValues[j][k] is the appropriate
	 * 		object for the value of the k-th data column in the j-th sample.
	 * 		Missing values correspond to null objects.
	 * @throws IllegalArgumentException
	 * 		if not data column types are given, 
	 * 		if a data column type is not a known subclass type,
	 * 		if no data values are given,
	 * 		if the number of data columns in the array of data values does 
	 * 			not match the number of data column types, or
	 * 		if a data value object is not an appropriate object 
	 * 			for the data column type
	 */
	public StdDataArray(DashDataType<?>[] dataColumnTypes, 
			Object[][] stdDataValues) throws IllegalArgumentException {
		if ( (dataColumnTypes == null) || (dataColumnTypes.length == 0) )
			throw new IllegalArgumentException("no data column types given");
		numDataCols = dataColumnTypes.length;
		if ( (stdDataValues == null) || (stdDataValues.length == 0) )
			throw new IllegalArgumentException("no standardized data values given");
		numSamples = stdDataValues.length;
		if ( stdDataValues[0].length != numDataCols )
			throw new IllegalArgumentException("Different number of data column values (" + 
					stdDataValues[0].length + ") and types (" +  numDataCols + ")");

		dataTypes = new DashDataType<?>[numDataCols];
		stdObjects = new Object[numSamples][numDataCols];

		for (int k = 0; k < numDataCols; k++) {
			DashDataType<?> dtype = dataColumnTypes[k];
			if ( dtype == null )
				throw new IllegalArgumentException(
						"no data type for column number" + Integer.toString(k+1));
			dataTypes[k] = dtype;

			// Catch invalid data column types and invalid data objects 
			// while assigning the standard data values
			if ( dtype instanceof CharDashDataType ) {
				for (int j = 0; j < numSamples; j++) {
					try {
						stdObjects[j][k] = (Character) stdDataValues[j][k];
					} catch ( Exception ex ) {
						throw new IllegalArgumentException("standard data object for sample number " + 
								Integer.toString(j+1) + ", column number " + Integer.toString(j+1) +
								" is invalid: " + ex.getMessage());
					}
				}
			}
			else if ( dtype instanceof IntDashDataType ) {
				for (int j = 0; j < numSamples; j++) {
					try {
						stdObjects[j][k] = (Integer) stdDataValues[j][k];
					} catch ( Exception ex ) {
						throw new IllegalArgumentException("standard data object for sample number " + 
								Integer.toString(j+1) + ", column number " + Integer.toString(j+1) +
								" is invalid: " + ex.getMessage());
					}
				}
			}
			else if ( dtype instanceof DoubleDashDataType ) {
				for (int j = 0; j < numSamples; j++) {
					try {
						stdObjects[j][k] = (Double) stdDataValues[j][k];
					} catch ( Exception ex ) {
						throw new IllegalArgumentException("standard data object for sample number " + 
								Integer.toString(j+1) + ", column number " + Integer.toString(j+1) +
								" is invalid: " + ex.getMessage());
					}
				}
			}
			else {
				throw new IllegalArgumentException("unknown data class type for " + 
						dtype.getDisplayName() + " (" + dtype.getDataClassName() + ")");
			}
		}

		assignColumnIndicesOfInterest();
	}

	/**
	 * Creates with the standardized data file types and values in the given 
	 * user standard data array.  The methods {@link #getSampleLongitudes()}, 
	 * {@link #getSampleLatitudes()}, {@link #getSampleDepths()}, and 
	 * {@link #getSampleTimes()} on the standardized user data must succeed 
	 * and return arrays with no null (missing) values.  No data column can be 
	 * the type {@link DashboardServerUtils#UNKNOWN}.  Only those data columns 
	 * matching one of the given known data files types is copied from the 
	 * standardized user data.  The following data columns will be added and 
	 * assigned if not already present:
	 * <ul>
	 *   <li>{@link DashboardServerUtils#YEAR}</li>
	 *   <li>{@link DashboardServerUtils#MONTH_OF_YEAR}</li>
	 *   <li>{@link DashboardServerUtils#DAY_OF_MONTH}</li>
	 *   <li>{@link DashboardServerUtils#HOUR_OF_DAY}</li>
	 *   <li>{@link DashboardServerUtils#MINUTE_OF_HOUR}</li>
	 *   <li>{@link DashboardServerUtils#SECOND_OF_MINUTE}</li>
	 *   <li>{@link DashboardServerUtils#TIME}</li>
	 * </ul>
	 * (TIME should always added and assigned since it is not a user provided type.)
	 * If the time to the seconds is not provided, the seconds values are all 
	 * set to zero and the added SECOND_OF_MINUTE column added will be all zeros.
	 * 
	 * @param userStdData
	 * 		standardized user data values
	 * @param dataFileTypes
	 * 		known data file column types
	 * @throws IllegalArgumentException
	 * 		if no standard user data values are given,
	 * 		if any of the user data types is {@link DashboardServerUtils#UNKNOWN}
	 * 		if any sample longitude, latitude, sample depth is missing, or
	 * 		if any sample time cannot be computed.
     * 
     *  ** THIS IS ONLY USED BY TEST CLASSES **
     *  
	 */
	public StdDataArray(StdDataArray userStdData, 
						KnownDataTypes dataFileTypes) throws IllegalArgumentException {
        this.datasetName = userStdData.datasetName;
		// StdUserDataArray has to have data columns, but could be missing the data values
		numSamples = userStdData.getNumSamples();
		if ( numSamples <= 0 )
			throw new IllegalArgumentException("no data values given");
		int numUserColumns = userStdData.getNumDataCols();

		// Check that sample longitude, latitude, depth, and time are present and all valid; 
		// hang onto the time values for adding to this standardized data
		Double[] timeVals;
		try {
			for ( Double value : userStdData.getSampleLongitudes() )
				if ( value == null )
					throw new IllegalArgumentException("a longitude value is missing");
			for ( Double value : userStdData.getSampleLatitudes() )
				if ( value == null )
					throw new IllegalArgumentException("a latitude value is missing");
			for ( Double value : userStdData.getSampleDepths() )
				if ( value == null )
					throw new IllegalArgumentException("a sample depth value is missing");
			timeVals = userStdData.getSampleTimes();
			for ( Double value : timeVals )
				if ( value == null )
					throw new IllegalArgumentException("a sample date/time value is missing");
		} catch ( IllegalStateException ex ) {
			throw new IllegalArgumentException(ex);
		}

		// Get the list of data file types present in the standardized user data
		ArrayList<DashDataType<?>> userDataTypes = new ArrayList<DashDataType<?>>(numUserColumns + 7);
		ArrayList<Integer> userColIndices = new ArrayList<Integer>(numUserColumns + 7);
		List <DashDataType<?>> userTypes = userStdData.getDataTypes();
		for (int k = 0; k < numUserColumns; k++) {
			DashDataType<?> dtype = userTypes.get(k);
			if ( DashboardServerUtils.UNKNOWN.typeNameEquals(dtype) )
				throw new IllegalArgumentException("user column number " + Integer.toString(k+1) + 
						" is type " + DashboardServerUtils.UNKNOWN.getDisplayName());
			if ( userStdData.isUsableIndex(k) && dataFileTypes.containsTypeName(dtype.getVarName()) ) {
				// OTHER and metadata column types are not added
				userColIndices.add(k);
				userDataTypes.add(dtype);
			}
		}
		// Add required data columns if not present
		if ( ! userStdData.hasYear() ) {
			userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
			userDataTypes.add(DashboardServerUtils.YEAR);
		}
		if ( ! userStdData.hasMonthOfYear() ) {
			userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
			userDataTypes.add(DashboardServerUtils.MONTH_OF_YEAR);
		}
		if ( ! userStdData.hasDayOfMonth() ) {
			userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
			userDataTypes.add(DashboardServerUtils.DAY_OF_MONTH);
		}
		if ( ! userStdData.hasHourOfDay() ) {
			userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
			userDataTypes.add(DashboardServerUtils.HOUR_OF_DAY);
		}
		if ( ! userStdData.hasMinuteOfHour() ) {
			userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
			userDataTypes.add(DashboardServerUtils.MINUTE_OF_HOUR);
		}
		if ( ! userStdData.hasSecondOfMinute() ) {
			userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
			userDataTypes.add(DashboardServerUtils.SECOND_OF_MINUTE);
		}
		if ( ! userDataTypes.contains(DashboardServerUtils.TIME) ) {
			userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
			userDataTypes.add(DashboardServerUtils.TIME);
		}

		numDataCols = userDataTypes.size();
		dataTypes = new DashDataType<?>[numDataCols];
		stdObjects = new Object[numSamples][numDataCols];
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.setLenient(false);
		for (int k = 0; k < numDataCols; k++) {
			dataTypes[k] = userDataTypes.get(k);
			int userIdx = userColIndices.get(k);
			if ( DashboardUtils.INT_MISSING_VALUE.equals(userIdx) ) {
				if ( DashboardServerUtils.YEAR.typeNameEquals(dataTypes[k]) ) {
					for (int j = 0; j < numSamples; j++) {
						cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
						stdObjects[j][k] = Integer.valueOf( cal.get(GregorianCalendar.YEAR) );
					}
				}
				else if ( DashboardServerUtils.MONTH_OF_YEAR.typeNameEquals(dataTypes[k]) ) {
					for (int j = 0; j < numSamples; j++) {
						cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
						stdObjects[j][k] = Integer.valueOf( cal.get(GregorianCalendar.MONTH) - GregorianCalendar.JANUARY + 1 );
					}
				}
				else if ( DashboardServerUtils.DAY_OF_MONTH.typeNameEquals(dataTypes[k]) ) {
					for (int j = 0; j < numSamples; j++) {
						cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
						stdObjects[j][k] = Integer.valueOf( cal.get(GregorianCalendar.DAY_OF_MONTH) );
					}
				}
				else if ( DashboardServerUtils.HOUR_OF_DAY.typeNameEquals(dataTypes[k]) ) {
					for (int j = 0; j < numSamples; j++) {
						cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
						stdObjects[j][k] = Integer.valueOf( cal.get(GregorianCalendar.HOUR_OF_DAY) );
					}
				}
				else if ( DashboardServerUtils.MINUTE_OF_HOUR.typeNameEquals(dataTypes[k]) ) {
					for (int j = 0; j < numSamples; j++) {
						cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
						stdObjects[j][k] = Integer.valueOf( cal.get(GregorianCalendar.MINUTE) );
					}
				}
				else if ( DashboardServerUtils.SECOND_OF_MINUTE.typeNameEquals(dataTypes[k]) ) {
					for (int j = 0; j < numSamples; j++) {
						cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
						Double second = ( 1000.0 * cal.get(GregorianCalendar.SECOND) + 
											cal.get(GregorianCalendar.MILLISECOND) ) / 1000.0;
						stdObjects[j][k] = second;
					}
				}
				else if ( DashboardServerUtils.TIME.typeNameEquals(dataTypes[k]) ) {
					for (int j = 0; j < numSamples; j++) {
						stdObjects[j][k] = timeVals[j];
					}
				}
				else {
					throw new IllegalArgumentException("Unexpected error: unknown data column type with missing index");
				}
			}
			else {
				for (int j = 0; j < numSamples; j++) {
					// Because isValidIndex was true, this should not throw any exceptions
					stdObjects[j][k] = userStdData.getStdVal(j, userIdx);
				}
			}
		}

		assignColumnIndicesOfInterest();
	}

	/**
	 * Assigns the data column indices of interest (longitude, latitude, sample 
	 * depth, and various time types) from the assigned types of the data columns.
	 */
	private void assignColumnIndicesOfInterest() {
		longitudeIndex = DashboardUtils.INT_MISSING_VALUE;
		latitudeIndex = DashboardUtils.INT_MISSING_VALUE;
		sampleDepthIndex = DashboardUtils.INT_MISSING_VALUE;
		samplePressureIndex = DashboardUtils.INT_MISSING_VALUE;
		datasetIdIndex = DashboardUtils.INT_MISSING_VALUE;
		stationIdIndex = DashboardUtils.INT_MISSING_VALUE;
		castIdIndex = DashboardUtils.INT_MISSING_VALUE;
		timestampIndex = DashboardUtils.INT_MISSING_VALUE;
		dateIndex = DashboardUtils.INT_MISSING_VALUE;
		yearIndex = DashboardUtils.INT_MISSING_VALUE;
		monthOfYearIndex = DashboardUtils.INT_MISSING_VALUE;
		dayOfMonthIndex = DashboardUtils.INT_MISSING_VALUE;
		timeOfDayIndex = DashboardUtils.INT_MISSING_VALUE;
		hourOfDayIndex = DashboardUtils.INT_MISSING_VALUE;
		minuteOfHourIndex = DashboardUtils.INT_MISSING_VALUE;
		secondOfMinuteIndex = DashboardUtils.INT_MISSING_VALUE;
		dayOfYearIndex = DashboardUtils.INT_MISSING_VALUE;
		secondOfDayIndex = DashboardUtils.INT_MISSING_VALUE;
		for (int k = 0; k < numDataCols; k++) {
			if ( DashboardServerUtils.LONGITUDE.typeNameEquals(dataTypes[k]) )
				longitudeIndex = k;
			else if ( DashboardServerUtils.LATITUDE.typeNameEquals(dataTypes[k]) )
				latitudeIndex = k;
			else if ( DashboardServerUtils.SAMPLE_DEPTH.typeNameEquals(dataTypes[k]) )
				sampleDepthIndex = k;
			else if ( DashboardServerUtils.WATER_PRESSURE.typeNameEquals(dataTypes[k]) )
				samplePressureIndex = k;
			else if ( DashboardServerUtils.DATASET_ID.typeNameEquals(dataTypes[k]))
				datasetIdIndex = k;
			else if ( DashboardServerUtils.STATION_ID.typeNameEquals(dataTypes[k]))
				stationIdIndex = k;
			else if ( DashboardServerUtils.CAST_ID.typeNameEquals(dataTypes[k]))
				castIdIndex = k;
			else if ( DashboardServerUtils.TIMESTAMP.typeNameEquals(dataTypes[k]) )
				timestampIndex = k;
			else if ( DashboardServerUtils.DATE.typeNameEquals(dataTypes[k]) )
				dateIndex = k;
			else if ( DashboardServerUtils.YEAR.typeNameEquals(dataTypes[k]) )
				yearIndex = k;
			else if ( DashboardServerUtils.MONTH_OF_YEAR.typeNameEquals(dataTypes[k]) )
				monthOfYearIndex = k;
			else if ( DashboardServerUtils.DAY_OF_MONTH.typeNameEquals(dataTypes[k]) )
				dayOfMonthIndex = k;
			else if ( DashboardServerUtils.TIME_OF_DAY.typeNameEquals(dataTypes[k]) )
				timeOfDayIndex = k;
			else if ( DashboardServerUtils.HOUR_OF_DAY.typeNameEquals(dataTypes[k]) )
				hourOfDayIndex = k;
			else if ( DashboardServerUtils.MINUTE_OF_HOUR.typeNameEquals(dataTypes[k]) )
				minuteOfHourIndex = k;
			else if ( DashboardServerUtils.SECOND_OF_MINUTE.typeNameEquals(dataTypes[k]) )
				secondOfMinuteIndex = k;
			else if ( DashboardServerUtils.DAY_OF_YEAR.typeNameEquals(dataTypes[k]) )
				dayOfYearIndex = k;
			else if ( DashboardServerUtils.SECOND_OF_DAY.typeNameEquals(dataTypes[k]) )
				secondOfDayIndex = k;
		}
	}

	/**
	 * @return 
	 * 		the number of samples (rows) in the current standardized data
	 */
	public int getNumSamples() {
		return numSamples;
	}

	/**
	 * @return 
	 * 		the number of data columns
	 */
	public int getNumDataCols() {
		return numDataCols;
	}

	/**
	 * Determines is this data column is an appropriate index.  This version 
	 * of the method just checks that the value is in the appropriate range.
	 * Subclasses should override this method if further validation is required.
	 * 
	 * @param idx
	 * 		index to test
	 * @return
	 * 		if the index is valid
	 */
	public boolean isUsableIndex(int idx) {
		if ( idx < 0 )
			return false;
		if ( idx >= numDataCols )
			return false;
		return true;
	}

	/**
	 * @return
	 * 		an array containing the standardized longitudes; 
	 * 		missing values are null
	 * @throws IllegalStateException
	 * 		if there are no standardized longitudes
	 */
	public Double[] getSampleLongitudes() throws IllegalStateException {
		if ( ! isUsableIndex(longitudeIndex) )
			throw new IllegalStateException("no valid longitude data column");
		Double[] sampleLongitudes = new Double[numSamples];
		for (int j = 0; j < numSamples; j++)
			sampleLongitudes[j] = (Double) stdObjects[j][longitudeIndex];
		return sampleLongitudes;
	}
	public int getLongitudeIndex() {
		return longitudeIndex;
	}
	public Double getSampleLongitude(int rowIdx) {
		if ( ! isUsableIndex(longitudeIndex) )
			return null;
		Double sampleLongitude = (Double) stdObjects[rowIdx][longitudeIndex];
		return sampleLongitude;
	}

	/**
	 * @return
	 * 		an array containing the standardized latitudes; 
	 * 		missing values are null
	 * @throws IllegalStateException
	 * 		if there are no standardized latitudes
	 */
	public Double[] getSampleLatitudes() throws IllegalStateException {
		if ( ! isUsableIndex(latitudeIndex) )
			throw new IllegalStateException("no valid latitude data column");
		Double[] sampleLatitudes = new Double[numSamples];
		for (int j = 0; j < numSamples; j++)
			sampleLatitudes[j] = (Double) stdObjects[j][latitudeIndex];
		return sampleLatitudes;
	}
	public int getLatitudeIndex() {
		return latitudeIndex;
	}
	public Double getSampleLatitude(int rowIdx) {
		if ( ! isUsableIndex(latitudeIndex) )
			return null;
		Double sampleLatitude = (Double) stdObjects[rowIdx][latitudeIndex];
		return sampleLatitude;
	}

	/**
	 * @return
	 * 		an array containing the standardized sample depths; 
	 * 		missing values are null
	 * @throws IllegalStateException
	 * 		if there are no standardized sample depths
	 */
	public Double[] getSampleDepths() throws IllegalStateException {
		if ( ! isUsableIndex(sampleDepthIndex) )
			throw new IllegalStateException("no valid sample depth data column");
		Double[] sampleDepths = new Double[numSamples];
		for (int j = 0; j < numSamples; j++)
			sampleDepths[j] = (Double) stdObjects[j][sampleDepthIndex];
		return sampleDepths;
	}
	public int getSampleDepthIndex() {
		return sampleDepthIndex;
	}

    private Double[] _sampleTimes = null;
	/**
	 * Computes the fully-specified time, in units of "seconds since 1970-01-01T00:00:00Z" 
	 * from the standardized date and time data values that can be found in the data.
	 * One of the following combinations of date/time columns must be given; if more than 
	 * one time specification is found, the first specification in this list is used.
	 * <ul>
	 *   <li>YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE</li>
	 *   <li>YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR</li>
	 *   <li>YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, TIME_OF_DAY</li>
	 *   <li>YEAR, DAY_OF_YEAR, SECOND_OF_DAY</li>
	 *   <li>TIMESTAMP</li>
	 *   <li>DATE, TIME_OF_DAY</li>
	 *   <li>DATE, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE</li>
	 *   <li>DATE, HOUR_OF_DAY, MINUTE_OF_HOUR</li>
	 *   <li>YEAR, DAY_OF_YEAR</li>
	 * </ul>
	 * In the formats without seconds, or TIME_OF_DAY values without seconds, the seconds
	 * are set to zero.  The logic in this ordering is the most likely mistake is with the 
	 * interpretation of a date string (year-month-day, day-month-year, month-day-year), 
	 * especially if the user gave years with only the last two digits.
	 * 
	 * @return
	 * 		an array containing the sample times; missing values are null
	 * @throws IllegalStateException
	 * 		if specification of the sample date and time is incomplete
	 */
	public synchronized Double[] getSampleTimes() throws IllegalStateException {
        if ( _sampleTimes == null ) {
            _sampleTimes = extractSampleTimes();
        }
        return _sampleTimes;
	}
	public Date getSampleTime(int rowIdx) throws IllegalStateException {
        if ( _sampleTimes == null || _sampleTimes.length == 0 ) { return null; }
        Double doubleTime = _sampleTimes[rowIdx];
        if ( doubleTime == null ) { return null; }
        Date time = null;
        time = new Date((long)(doubleTime.doubleValue()*1000));
        return time;
	}
	private Double[] extractSampleTimes() throws IllegalStateException {
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.setLenient(false);
		Double[] sampleTimes = new Double[numSamples];

		if ( isUsableIndex(yearIndex) && isUsableIndex(monthOfYearIndex) && 
			 isUsableIndex(dayOfMonthIndex) && isUsableIndex(hourOfDayIndex) && 
			 isUsableIndex(minuteOfHourIndex) ) {
			// Get time using year, month, day, hour, minute, and (if available) second
			boolean hasSec = isUsableIndex(secondOfMinuteIndex);
			for (int j = 0; j < numSamples; j++) {
				try {
					int year = ((Integer) stdObjects[j][yearIndex]).intValue();
					int month = ((Integer) stdObjects[j][monthOfYearIndex]).intValue();
					int day = ((Integer) stdObjects[j][dayOfMonthIndex]).intValue();
					int hour = ((Integer) stdObjects[j][hourOfDayIndex]).intValue();
					int min = ((Integer) stdObjects[j][minuteOfHourIndex]).intValue();
					int sec = 0;
					int millisec = 0;
					if ( hasSec ) {
						try {
							Double value = (Double) stdObjects[j][secondOfMinuteIndex];
							sec = value.intValue();
							value -= sec;
							value *= 1000.0;
							millisec = value.intValue();
						} catch ( Exception ex ) {
							sec = 0;
							millisec = 0;
						}
					}
					cal.set(year, GregorianCalendar.JANUARY+month-1, day, hour, min, sec);
					cal.set(GregorianCalendar.MILLISECOND, millisec);
					sampleTimes[j] = Double.valueOf( cal.getTimeInMillis() / 1000.0 );
				} catch ( Exception ex ) {
					sampleTimes[j] = null;
				}
			}
		}
		else if ( isUsableIndex(yearIndex) && isUsableIndex(monthOfYearIndex) && 
				  isUsableIndex(dayOfMonthIndex) && isUsableIndex(timeOfDayIndex) ) {
			// Use year, month, day, and time string
			// Standard format of time string is HH:mm:ss.SSS
			for (int j = 0; j < numSamples; j++) {
				try {
					int year = ((Integer) stdObjects[j][yearIndex]).intValue();
					int month = ((Integer) stdObjects[j][monthOfYearIndex]).intValue();
					int day = ((Integer) stdObjects[j][dayOfMonthIndex]).intValue();
					String[] hms = ((String) stdObjects[j][timeOfDayIndex]).split(":");
					if ( hms.length != 3 )
						throw new Exception();
					int hour = Integer.parseInt(hms[0]);
					int min = Integer.parseInt(hms[1]);
					Double value = Double.parseDouble(hms[2]);
					int sec = value.intValue();
					value -= sec;
					value *= 1000.0;
					int millisec = value.intValue();
					cal.set(year, GregorianCalendar.JANUARY+month-1, day, hour, min, sec);
					cal.set(GregorianCalendar.MILLISECOND, millisec);
					sampleTimes[j] = Double.valueOf( cal.getTimeInMillis() / 1000.0 );
				} catch ( Exception ex ) {
					sampleTimes[j] = null;
				}
			}
		}
		else if ( isUsableIndex(yearIndex) && isUsableIndex(dayOfYearIndex) && 
				  isUsableIndex(secondOfDayIndex) ) {
			// Use year, day of year (an integer), and second of day
			for (int j = 0; j < numSamples; j++) {
				try {
					int year = ((Integer) stdObjects[j][yearIndex]).intValue();
					Double value = (Double) stdObjects[j][dayOfYearIndex];
					int dayOfYear = value.intValue();
					if ( Math.abs(value - dayOfYear) > DashboardUtils.MAX_ABSOLUTE_ERROR )
						throw new Exception();
					value = ((Double) stdObjects[j][secondOfDayIndex]).doubleValue();
					value /= 3600.0;
					int hour = value.intValue();
					value -= hour;
					value *= 60.0;
					int minute = value.intValue();
					value -= minute;
					value *= 60.0;
					int sec = value.intValue();
					value -= sec;
					value *= 1000.0;
					int millisec = value.intValue();
					cal.clear(GregorianCalendar.MONTH);
					cal.clear(GregorianCalendar.DAY_OF_MONTH);
					cal.set(GregorianCalendar.YEAR, year);
					cal.set(GregorianCalendar.DAY_OF_YEAR, dayOfYear);
					cal.set(GregorianCalendar.HOUR_OF_DAY, hour);
					cal.set(GregorianCalendar.MINUTE, minute);
					cal.set(GregorianCalendar.SECOND, sec);
					cal.set(GregorianCalendar.MILLISECOND, millisec);
					sampleTimes[j] = Double.valueOf( cal.getTimeInMillis() / 1000.0 );
				} catch ( Exception ex ) {
					sampleTimes[j] = null;
				}
			}
		}
		else if ( isUsableIndex(timestampIndex) ) {
			// Use full timestamp
			// Standard format of the timestamp is yyyy-MM-dd HH:mm:sss.SSS
			for (int j = 0; j < numSamples; j++) {
				try {
					String[] dateTime = ((String) stdObjects[j][timestampIndex]).split(" ");
					if ( dateTime.length != 2 )
						throw new Exception();
					String[] ymd = dateTime[0].split("-");
					if ( ymd.length != 3 )
						throw new Exception();
					int year = Integer.parseInt(ymd[0]);
					int month = Integer.parseInt(ymd[1]);
					int day = Integer.parseInt(ymd[2]);
					String[] hms = dateTime[1].split(":");
					if ( hms.length != 3 )
						throw new Exception();
					int hour = Integer.parseInt(hms[0]);
					int min = Integer.parseInt(hms[1]);
					Double value = Double.parseDouble(hms[2]);
					int sec = value.intValue();
					value -= sec;
					value *= 1000.0;
					int millisec = value.intValue();
					cal.set(year, GregorianCalendar.JANUARY+month-1, day, hour, min, sec);
					cal.set(GregorianCalendar.MILLISECOND, millisec);
					sampleTimes[j] = Double.valueOf( cal.getTimeInMillis() / 1000.0 );
				} catch ( Exception ex ) {
					sampleTimes[j] = null;
				}
			}
		}
		else if ( isUsableIndex(dateIndex) && isUsableIndex(timeOfDayIndex) ) {
			// Use date string and time string
			// Standard format of the date is yyyy-MM-dd
			// Standard format of time string is HH:mm:ss.SSS
			for (int j = 0; j < numSamples; j++) {
//				try {
					String stdYMD = (String) stdObjects[j][dateIndex];
					if ( DashboardUtils.isEmptyNull(stdYMD)) {
						throw new IllegalStateException("Invalid (empty or null) date string (possibly due to wrong date format specified) at row: " + j);
					}
					String[] ymd = stdYMD.split("-");
					if ( ymd.length != 3 )
						throw new IllegalStateException("Invalid date string: "+ stdYMD);
					int year = Integer.parseInt(ymd[0]);
					int month = Integer.parseInt(ymd[1]);
					int day = Integer.parseInt(ymd[2]);
					String stdHMS = (String) stdObjects[j][timeOfDayIndex];
					if ( DashboardUtils.isEmptyNull(stdHMS)) {
						throw new IllegalStateException("Invalid (empty or null) time string (possibly due to wrong time format specified) at row: " + j);
					}
					String[] hms = stdHMS.split(":");
					if ( hms.length != 3 )
						throw new IllegalStateException("Invalid time string: "+ stdHMS);
					int hour = Integer.parseInt(hms[0]);
					int min = Integer.parseInt(hms[1]);
					Double value = Double.parseDouble(hms[2]);
					int sec = value.intValue();
                    if ( hour == 0 && min == 0 && sec == 0 
                         && this.getClass().isAssignableFrom(StdUserDataArray.class )) {
                        ADCMessage msg = new ADCMessage();
                        msg.setSeverity(Severity.WARNING);
                        msg.setRowIndex(j);
                        msg.setColIndex(timeOfDayIndex);
                        String comment = "Possible bad time value of 0";
                        msg.setGeneralComment(comment);
                        msg.setDetailedComment(comment);
                        ((StdUserDataArray)this).getStandardizationMessages().add(msg);
                    }
					value -= sec;
					value *= 1000.0;
					int millisec = value.intValue();
					cal.set(year, GregorianCalendar.JANUARY+month-1, day, hour, min, sec);
					cal.set(GregorianCalendar.MILLISECOND, millisec);
					sampleTimes[j] = Double.valueOf( cal.getTimeInMillis() / 1000.0 );
//				} catch ( Exception ex ) {
//					sampleTimes[j] = null;
//				}
			}
		}
		else if ( isUsableIndex(dateIndex) && isUsableIndex(hourOfDayIndex) && 
				  isUsableIndex(minuteOfHourIndex) ) {
			// Use date string, hour, minute, and (if available) second
			// Standard format of the date is yyyy-MM-dd
			boolean hasSec = isUsableIndex(secondOfMinuteIndex);
			for (int j = 0; j < numSamples; j++) {
				try {
					String[] ymd = ((String) stdObjects[j][dateIndex]).split("-");
					if ( ymd.length != 3 )
						throw new Exception();
					int year = Integer.parseInt(ymd[0]);
					int month = Integer.parseInt(ymd[1]);
					int day = Integer.parseInt(ymd[2]);
					int hour = ((Integer) stdObjects[j][hourOfDayIndex]).intValue();
					int min = ((Integer) stdObjects[j][minuteOfHourIndex]).intValue();
					int sec = 0;
					int millisec = 0;
					if ( hasSec ) {
						try {
							Double value = (Double) stdObjects[j][secondOfMinuteIndex];
							sec = value.intValue();
							value -= sec;
							value *= 1000.0;
							millisec = value.intValue();
						} catch ( Exception ex ) {
							sec = 0;
							millisec = 0;
						}
					}
					cal.set(year, GregorianCalendar.JANUARY+month-1, day, hour, min, sec);
					cal.set(GregorianCalendar.MILLISECOND, millisec);
					sampleTimes[j] = Double.valueOf( cal.getTimeInMillis() / 1000.0 );
				} catch ( Exception ex ) {
					sampleTimes[j] = null;
				}
			}
		}
		else if ( isUsableIndex(yearIndex) && isUsableIndex(dayOfYearIndex) ) {
			// Use year and day of year (floating-point)
			for (int j = 0; j < numSamples; j++) {
				try {
					int year = ((Integer) stdObjects[j][yearIndex]).intValue();
					Double value = (Double) stdObjects[j][dayOfYearIndex];
					int dayOfYear = value.intValue();
					value -= dayOfYear;
					value *= 24.0;
					int hour = value.intValue();
					value -= hour;
					value *= 60.0;
					int minute = value.intValue();
					value -= minute;
					value *= 60.0;
					int sec = value.intValue();
					value -= sec;
					value *= 1000.0;
					int millisec = value.intValue();
					cal.clear(GregorianCalendar.MONTH);
					cal.clear(GregorianCalendar.DAY_OF_MONTH);
					cal.set(GregorianCalendar.YEAR, year);
					cal.set(GregorianCalendar.DAY_OF_YEAR, dayOfYear);
					cal.set(GregorianCalendar.HOUR_OF_DAY, hour);
					cal.set(GregorianCalendar.MINUTE, minute);
					cal.set(GregorianCalendar.SECOND, sec);
					cal.set(GregorianCalendar.MILLISECOND, millisec);
					sampleTimes[j] = Double.valueOf( cal.getTimeInMillis() / 1000.0 );
				} catch ( Exception ex ) {
					sampleTimes[j] = null;
				}
			}
		}
		else
			throw new IllegalStateException("Incomplete specification of sample time");

		return sampleTimes;
	}

	/**
	 * @return
	 * 		if there is a valid {@link DashboardServerUtils#LONGITUDE} data column
	 */
	public boolean hasLongitude() {
		return isUsableIndex(longitudeIndex);
	}

	/**
	 * @return
	 * 		if there is a valid {@link DashboardServerUtils#LATITUDE} data column
	 */
	public boolean hasLatitude() {
		return isUsableIndex(latitudeIndex);
	}

	/**
	 * @return
	 * 		if there is a valid {@link DashboardServerUtils#SAMPLE_DEPTH} data column
	 */
	public boolean hasSampleDepth() {
		return isUsableIndex(sampleDepthIndex);
	}

    public boolean hasSamplePressure() {
        return isUsableIndex(samplePressureIndex);
    }

	public boolean hasDate() {
		return isUsableIndex(timestampIndex) || isUsableIndex(dateIndex) ||
			 ( isUsableIndex(yearIndex) && isUsableIndex(monthOfYearIndex) && isUsableIndex(dayOfMonthIndex));
	}
	
    public boolean hasTimeOfDay() {
		return isUsableIndex(timestampIndex) || isUsableIndex(timeOfDayIndex) ||
			 ( isUsableIndex(hourOfDayIndex) && isUsableIndex(minuteOfHourIndex));
    }
    
	public boolean hasSampleTime() {
		return isUsableIndex(timestampIndex) || 
		     ( hasDate() && hasTimeOfDay());
	}
	
	/**
	 * @return
	 * 		if there is a valid {@link DashboardServerUtils#YEAR} data column
	 */
	public boolean hasYear() {
		return isUsableIndex(yearIndex) || isUsableIndex(dateIndex) || isUsableIndex(timestampIndex);
	}

	/**
	 * @return
	 * 		if there is a valid {@link DashboardServerUtils#MONTH_OF_YEAR} data column
	 */
	public boolean hasMonthOfYear() {
		return isUsableIndex(monthOfYearIndex) || isUsableIndex(dateIndex) || isUsableIndex(timestampIndex);
	}

	/**
	 * @return
	 * 		if there is a valid {@link DashboardServerUtils#DAY_OF_MONTH} data column
	 */
	public boolean hasDayOfMonth() {
		return isUsableIndex(dayOfMonthIndex) || isUsableIndex(dateIndex) || isUsableIndex(timestampIndex);
	}

	/**
	 * @return
	 * 		if there is a valid {@link DashboardServerUtils#HOUR_OF_DAY} data column
	 */
	public boolean hasHourOfDay() {
		return isUsableIndex(hourOfDayIndex) || isUsableIndex(timeOfDayIndex) || isUsableIndex(timestampIndex);
	}

	/**
	 * @return
	 * 		if there is a valid {@link DashboardServerUtils#MINUTE_OF_HOUR} data column
	 */
	public boolean hasMinuteOfHour() {
		return isUsableIndex(minuteOfHourIndex) || isUsableIndex(timeOfDayIndex) || isUsableIndex(timestampIndex);
	}

	/**
	 * @return
	 * 		if there is a valid {@link DashboardServerUtils#SECOND_OF_MINUTE} data column
	 */
	public boolean hasSecondOfMinute() {
		return isUsableIndex(secondOfMinuteIndex) || isUsableIndex(timeOfDayIndex) || isUsableIndex(timestampIndex);
	}

	/**
	 * @return
	 * 		an unmodifiable list of types for the data columns.
	 */
	public List<DashDataType<?>> getDataTypes() {
		return Collections.unmodifiableList(Arrays.asList(dataTypes));
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
	 * 		if the sample index or the data column index is invalid
	 */
	public Object getStdVal(int sampleIdx, int columnIdx) throws IndexOutOfBoundsException{
		if ( (sampleIdx < 0) || (sampleIdx >= numSamples) )
			throw new IndexOutOfBoundsException("sample index is invalid: " + sampleIdx);
		if ( (columnIdx < 0) || (columnIdx >= numDataCols) )
			throw new IndexOutOfBoundsException("data column index is invalid: " + columnIdx);
		return stdObjects[sampleIdx][columnIdx];
	}

	/**
	 * Checks to see if the dataset has a defined column of the type specified.
	 * 
	 * @param dataColStdName The standard name for the column type.
	 * @return  true if the named column type exists in the dataset 
	 */
	public boolean hasDataColumn(String dataColStdName) {
		return lookForDataColumnIndex(dataColStdName) != null;
	}
		
	/**
	 * Looks to see if the named column type is defined for this dataset.
	 * @param dataColStdName The standard name for the column date to look for.
	 * @return The Integer value of the zero-based column index for the named column if found, or null if not found. 
	 */
	public Integer lookForDataColumnIndex(String dataColStdName) {
		Integer columnIdx = null;
		List<DashDataType<?>> colTypes = getDataTypes();
		int idx = 0;
		for (DashDataType<?> colType : colTypes) {
			if ( colType.typeNameEquals(dataColStdName)) {
				columnIdx = new Integer(idx);
				break;
			}
			idx += 1;
		}
		return columnIdx;
	}
	
	/**
	 * Get the station ID for the specified row.
	 * 
	 * @param row Zero-based row index
	 * @return The stationId for the specified row.
	 * @throws IllegalStateException If there is no station ID column defined.
	 */
	public String getStationId(int row) throws IllegalStateException {
		String stationId = null;
		int cidx = getStationIdIndex();
		stationId = String.valueOf((getStdVal(row, cidx)));
		return stationId;
	}
	
	private int getStationIdIndex() throws IllegalStateException {
		if ( stationIdIndex == DashboardUtils.INT_MISSING_VALUE.intValue() ) {
			Integer checkIdx = lookForDataColumnIndex(STATION_ID_VARNAME);
			if ( checkIdx == null ) {
				throw new IllegalStateException("No stationId column found.");
			}
			stationIdIndex = checkIdx.intValue();
		}
		return stationIdIndex;
	}

	/**
	 * Check to see if a stationId column has been defined.
	 * @return
	 */
	public boolean hasStationIdColumn() {
		try {
			getStationIdIndex();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

    public String getDatasetId(int row) {
        if (hasDatasetIdColumn()) {
            return String.valueOf(getStdVal(row, getDatasetIdIndex()));
        } else {
            return getDatasetName();
        }
    }

	public String getDatasetName() {
		return datasetName;
	}

    private int getDatasetIdIndex() throws IllegalStateException {
        if ( hasDatasetIdColumn()) {
            return datasetIdIndex;
        } else {
            throw new IllegalStateException("No datasetId column found.");
        }
    }
    public boolean hasDatasetIdColumn() {
        if ( datasetIdIndex != DashboardUtils.INT_MISSING_VALUE.intValue()) {
            return true;
        }
        Integer index = lookForDataColumnIndex(DATASET_IDENTIFIER_VARNAME);
        if ( index != null ) {
            datasetIdIndex = index.intValue();
            return true;
        }
        return false;
}
/**
 * Get the cast ID for the specified row.
	 * 
	 * @param row Zero-based row index
	 * @return The castId for the specified row.
	 * @throws IllegalStateException If there is no cast ID column defined.
	 */
	public String getDatasetStationCastIdentifier(int row) throws IllegalStateException {
        String stationCastId = getDatasetId(row);
        if ( hasStationIdColumn()) {
            int sidx = getStationIdIndex();
            stationCastId += ":" + String.valueOf((getStdVal(row, sidx)));
        }
        if ( hasCastIdColumn()) {
    		String castId = null;
    		int cidx = getCastIdIndex();
    		castId = String.valueOf((getStdVal(row, cidx)));
            if ( ! StringUtils.emptyOrNullOrNull(castId)) {
                stationCastId += ":" + castId;
            }
        }
		return stationCastId;
	}
	
	private int getCastIdIndex() {
		if ( castIdIndex == DashboardUtils.INT_MISSING_VALUE.intValue() ) {
			Integer checkIdx = lookForDataColumnIndex(CAST_ID_VARNAME);
			if ( checkIdx == null ) {
				throw new IllegalStateException("No castId column found.");
			}
			castIdIndex = checkIdx.intValue();
		}
		return castIdIndex;
	}

	/**
	 * Check to see if a castId column has been defined.
	 * @return
	 */
	public boolean hasCastIdColumn() {
		try {
			getCastIdIndex();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 37;
		int result = Arrays.deepHashCode(stdObjects);
		result = prime * result + secondOfDayIndex;
		result = prime * result + dayOfYearIndex;
		result = prime * result + secondOfMinuteIndex;
		result = prime * result + minuteOfHourIndex;
		result = prime * result + hourOfDayIndex;
		result = prime * result + timeOfDayIndex;
		result = prime * result + dayOfMonthIndex;
		result = prime * result + monthOfYearIndex;
		result = prime * result + yearIndex;
		result = prime * result + dateIndex;
		result = prime * result + timestampIndex;
		result = prime * result + sampleDepthIndex;
		result = prime * result + latitudeIndex;
		result = prime * result + longitudeIndex;
		result = prime * result + Arrays.hashCode(dataTypes);
		result = prime * result + numDataCols;
		result = prime * result + numSamples;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;

		if ( ! ( obj instanceof StdDataArray ) )
			return false;
		StdDataArray other = (StdDataArray) obj;

		if ( numDataCols != other.numDataCols )
			return false;
		if ( numSamples != other.numSamples )
			return false;

		if ( longitudeIndex != other.longitudeIndex )
			return false;
		if ( latitudeIndex != other.latitudeIndex )
			return false;
		if ( sampleDepthIndex != other.sampleDepthIndex )
			return false;
		if ( timestampIndex != other.timestampIndex )
			return false;
		if ( dateIndex != other.dateIndex )
			return false;
		if ( yearIndex != other.yearIndex )
			return false;
		if ( monthOfYearIndex != other.monthOfYearIndex )
			return false;
		if ( dayOfMonthIndex != other.dayOfMonthIndex )
			return false;
		if ( timeOfDayIndex != other.timeOfDayIndex )
			return false;
		if ( hourOfDayIndex != other.hourOfDayIndex )
			return false;
		if ( minuteOfHourIndex != other.minuteOfHourIndex )
			return false;
		if ( secondOfMinuteIndex != other.secondOfMinuteIndex )
			return false;
		if ( dayOfYearIndex != other.dayOfYearIndex )
			return false;
		if ( secondOfDayIndex != other.secondOfDayIndex )
			return false;

		if ( ! Arrays.equals(dataTypes, other.dataTypes) )
			return false;

		if ( ! Arrays.deepEquals(stdObjects, other.stdObjects) )
			return false;

		return true;
	}

	@Override
	public String toString() {
		String repr = "StdDataArray[numSamples=" + numSamples + ", numDataCols=" + numDataCols;
		repr += ",\n  longitudeIndex=" + longitudeIndex;
		repr += ", latitudeIndex=" + latitudeIndex;
		repr += ", sampleDepthIndex=" + sampleDepthIndex;
		repr += ", timestampIndex=" + timestampIndex;
		repr += ", dateIndex=" + dateIndex;
		repr += ", yearIndex=" + yearIndex;
		repr += ", monthOfYearIndex=" + monthOfYearIndex;
		repr += ", dayOfMonthIndex=" + dayOfMonthIndex;
		repr += ", timeOfDayIndex=" + timeOfDayIndex;
		repr += ", hourOfDayIndex=" + hourOfDayIndex;
		repr += ", minuteOfHourIndex=" + minuteOfHourIndex;
		repr += ", secondOfMinuteIndex=" + secondOfMinuteIndex;
		repr += ", dayOfYearIndex=" + dayOfYearIndex;
		repr += ", secondOfDayIndex=" + secondOfDayIndex;
		repr += ",\n  dataTypes=[";
		for (int k = 0; k < numDataCols; k++) {
			if ( k > 0 )
				repr += ",";
			repr += "\n    " + dataTypes[k].toString();
		}
		repr += "\n  ]";
		repr += ",\n  stdObjects=[";
		for (int j = 0; j < numSamples; j++) {
			if ( j > 0 )
				repr += ",";
			repr += "\n    " + Arrays.toString(stdObjects[j]);
		}
		repr += "\n  ]";
		repr += "\n]";
		return repr;
	}
}
