/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.i18n.shared.DateTimeFormat;

import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;

/**
 * Static dashboard utility functions and constants
 * for use on both the client and server side.
 * 
 * @author Karl Smith
 */
public class DashboardUtils {

	// Cruise upload action strings
	public static final String PREVIEW_REQUEST_TAG = "PREVIEW REQUEST";
	public static final String NEW_DATASETS_REQUEST_TAG = "NEW DATASETS REQUEST";
//	public static final String APPEND_DATASETS_REQUEST_TAG = "APPEND DATASETS REQUEST";
	public static final String OVERWRITE_DATASETS_REQUEST_TAG = "OVERWRITE DATASETS REQUEST";

	// Recognized data formats
	public static final String COMMA_FORMAT_TAG = "COMMA-SEPARATED VALUES";
	public static final String SEMICOLON_FORMAT_TAG = "SEMICOLON-SEPARATED VALUES";
	public static final String TAB_FORMAT_TAG = "TAB-SEPARATED VALUES";
	public static final String UNSPECIFIED_DELIMITER_FORMAT_TAG = "UNSPECIFIED DELIMITER";

	// Cruise upload result strings
	public static final String FILE_PREVIEW_HEADER_TAG = "FILE PREVIEW HEADER TAG";
	public static final String INVALID_FILE_HEADER_TAG = "INVALID FILE HEADER TAG";
	public static final String DATASET_EXISTS_HEADER_TAG = "DATASET EXISTS HEADER TAG";
	public static final String UNEXPECTED_FAILURE_HEADER_TAG = "UNEXPECTED FAILURE HEADER TAG";
	public static final String END_OF_ERROR_MESSAGE_TAG = "END_OF_ERROR MESSAGE_TAG";
	public static final String SUCCESS_HEADER_TAG = "SUCCESS HEADER TAG";

	// Maximum number of rows shown in a page of a data grid (table)
	public static final int MAX_ROWS_PER_GRID_PAGE = 50;

	// Maximum number of error messages in an acceptable cruise
	public static final int MAX_ACCEPTABLE_ERRORS = 50;

	// Data check strings
	public static final String CHECK_STATUS_NOT_CHECKED = "";
	public static final String CHECK_STATUS_ACCEPTABLE = "No warnings";
	public static final String CHECK_STATUS_WARNINGS_PREFIX = "Warnings:";
	public static final String CHECK_STATUS_ERRORS_PREFIX = "Errors:";
	public static final String CHECK_STATUS_CRITICAL_ERRORS_PREFIX = "Critical Errors:";
	public static final String CHECK_STATUS_UNACCEPTABLE = "Unacceptable";
	public static final String GEOPOSITION_ERRORS_MSG = "(lat/lon/time errors!)";

	// Status strings - datasets that can be modified
	public static final String STATUS_NOT_SUBMITTED = "";
	public static final String STATUS_SUSPENDED = "Suspended";
	public static final String STATUS_EXCLUDED = "Excluded";
	// Status strings - datasets that cannot be modified
	public static final String STATUS_SUBMITTED = "Submitted";
	public static final String STATUS_ACCEPTED = "Accepted";
	public static final String STATUS_CONFLICT = "Conflict";
	public static final String STATUS_RENAMED = "Renamed";

	// Archival options
	public static final String ARCHIVE_STATUS_NOT_SUBMITTED = "";
	public static final String ARCHIVE_STATUS_WITH_NEXT_RELEASE = "With next release";
	public static final String ARCHIVE_STATUS_SENT_FOR_ARCHIVAL = "Sent for archival";
	public static final String ARCHIVE_STATUS_OWNER_TO_ARCHIVE = "Owner to archive";
	public static final String ARCHIVE_STATUS_ARCHIVED = "Archived";
	public static final String ARCHIVE_STATUS_FAILED = "Submission Failed";

	/**
	 *  Missing value for floating-point variables - not null or NaN
	 */
	public static final Double FP_MISSING_VALUE = -1.0E+34;

	/**
	 *  Missing value for integer variables - not null
	 */
	public static final Integer INT_MISSING_VALUE = -99;

	/**
	 * Missing value for String variables - not null
	 */
	public static final String STRING_MISSING_VALUE = "";

	/**
	 * Missing value for Character variables - not null
	 */
	public static final Character CHAR_MISSING_VALUE = ' ';

	/**
	 * Date used as a missing value - not null; 
	 * corresponds to Jan 2, 1800 00:00:00 UTC
	 */
	public static final Date DATE_MISSING_VALUE = new Date(-5364576000000L);

	/** 
	 * Maximum relative error between two floating point values 
	 * still considered the same value for practical purposes. 
	 * Typically used for rtol in {@link #closeTo(Double, Double, double, double)}
	 */
	public static final double MAX_RELATIVE_ERROR = 1.0E-6;

	/** 
	 * Maximum absolute error between two floating point values 
	 * still considered the same value for practical purposes. 
	 * Typically used for atol in {@link #closeTo(Double, Double, double, double)}
	 */
	public static final double MAX_ABSOLUTE_ERROR = 1.0E-6;

	/**
	 * The "upload filename" for all OME metadata files.
	 */
	public static final String _OME_FILENAME = "OME.xml";

	/**
	 * THe PDF version of the OME XML files.
	 */
	public static final String _OME_PDF_FILENAME = "OME.pdf";

	/**
	 * The "upload filename" for all PI-provided OME metadata files 
	 * that are not used for anything other than generating a supplemental 
	 * document.
	 * 
	 * The use of this name is just a temporary measure 
	 * until the CDIAC OME brought into the dashboard.
	 */
	public static final String _PI_OME_FILENAME = "PI_OME.xml";

	/**
	 * The PDF version of the PI OME XML file.
	 */
	public static final String _PI_OME_PDF_FILENAME = "PI_OME.pdf";


	/** For data without any specific units */
	public static final ArrayList<String> NO_UNITS = 
			new ArrayList<String>(Arrays.asList(""));

	/** Formats for date-time stamps */
	public static final ArrayList<String> TIMESTAMP_UNITS = 
			new ArrayList<String>(Arrays.asList(
					"yyyy-mm-dd hh:mm:ss", 
					"mm-dd-yyyy hh:mm:ss", 
					"dd-mm-yyyy hh:mm:ss", 
					"dd-mon-yyyy hh:mm:ss", 
					"mm-dd-yy hh:mm:ss", 
					"dd-mm-yy hh:mm:ss"));

	/** Formats for dates */
	public static final ArrayList<String> DATE_UNITS = 
			new ArrayList<String>(Arrays.asList(
					"yyyy-mm-dd", 
					"mm-dd-yyyy", 
					"dd-mm-yyyy", 
					"dd-mon-yyyy", 
					"mm-dd-yy", 
					"dd-mm-yy",
					"dd-mon-yy",
					"mon-dd-yyyy"
					));

	/** Formats for time-of-day */
	public static final ArrayList<String> TIME_OF_DAY_UNITS = 
			new ArrayList<String>(Arrays.asList("hh:mm:ss")); // ,"hh:mm"));

	/** Units for day-of-year (value of the first day of the year) */
	public static final ArrayList<String> DAY_OF_YEAR_UNITS = 
			new ArrayList<String>(Arrays.asList("Jan1=1.0", "Jan1=0.0"));

	/** Units for longitude */
	public static final ArrayList<String> LONGITUDE_UNITS = 
			new ArrayList<String>(Arrays.asList(
					"deg E", 
					"deg W", 
					"deg min E", 
					"deg min W", 
					"deg min sec E", 
					"deg min sec W"));

	/** Units of latitude */
	public static final ArrayList<String> LATITUDE_UNITS = 
			new ArrayList<String>(Arrays.asList(
					"deg N", 
					"deg S", 
					"deg min N", 
					"deg min S", 
					"deg min sec N", 
					"deg min sec S"));

	/** Unit of depth */
	public static final ArrayList<String> DEPTH_UNITS = 
			new ArrayList<String>(Arrays.asList("meters"));

	/** Unit of pressure */
	public static final ArrayList<String> PRESSURE_UNITS = 
			new ArrayList<String>(Arrays.asList("hPa", "kPa", "mmHg", "decibars"));

    /** Units of temperature */
	public static final ArrayList<String> TEMPERATURE_UNITS =
			new ArrayList<String>(Arrays.asList("degrees C"));
	/**
	 * UNASSIGNED needs to be respecified as one of the (other) data column types.
	 */
	public static final String UNKNOWN_VARNAME = "unknown";
	public static final DataColumnType UNKNOWN = new DataColumnType(UNKNOWN_VARNAME, 
			0.0, "(unknown)", "Unspecified data type", false, NO_UNITS);

	/**
	 * OTHER is for supplementary data in the user's original data file but 
	 * otherwise not used.  A description of each column with this type must 
	 * be part of the metadata, but the values are not validated or used. 
	 * Multiple columns may have this type.
	 */
	public static final String OTHER_VARNAME = "other";
	public static final DataColumnType OTHER = new DataColumnType(OTHER_VARNAME,
			1.0, "IGNORED", "Unused or unchecked supplementary data", false, NO_UNITS);

	/**
	 * User-provided name of the cruise/dataset
	 */
	public static final String DATASET_IDENTIFIER_VARNAME = "dataset_name";
	public static final DataColumnType DATASET_IDENTIFIER = new DataColumnType(DATASET_IDENTIFIER_VARNAME,
			100.0, "cruise/dataset name", "unique name for this dataset", true, NO_UNITS);

	public static final String EXPO_CODE_VARNAME = "expocode";
	public static final DataColumnType EXPO_CODE = new DataColumnType(EXPO_CODE_VARNAME, 
			100.0, "expo code", "expocode for the cruise", false, NO_UNITS);

	public static final String PLATFORM_CODE_VARNAME = "platform_code";
	public static final DataColumnType PLATFORM_CODE = new DataColumnType(PLATFORM_CODE_VARNAME,
			101.0, "platform code", "platform code", false, NO_UNITS);

	public static final String PLATFORM_NAME_VARNAME = "platform_name";
	public static final DataColumnType PLATFORM_NAME = new DataColumnType(PLATFORM_NAME_VARNAME,
			101.5, "platform name", "platform name", false, NO_UNITS);

	public static final String PLATFORM_TYPE_VARNAME = "platform_type";
	public static final DataColumnType PLATFORM_TYPE = new DataColumnType(PLATFORM_TYPE_VARNAME,
			102.0, "platform type", "platform type", false, NO_UNITS);

	public static final String ORGANIZATION_NAME_VARNAME = "organization";
	public static final DataColumnType ORGANIZATION_NAME = new DataColumnType(ORGANIZATION_NAME_VARNAME,
			103.0, "organization", "organization", false, NO_UNITS);
	
	public static final String INVESTIGATOR_NAMES_VARNAME = "investigators";
	public static final DataColumnType INVESTIGATOR_NAMES = new DataColumnType(INVESTIGATOR_NAMES_VARNAME,
			104.0, "PI names", "investigators", false, NO_UNITS);

	/**
	 * User-provided station / cast identifier.
	 *
     * deprecated. And removed.
	public static final DataColumnType STATION_CAST = new DataColumnType("station", 
			200.0, "station/cast", "station", true, NO_UNITS);
	 */

	public static final String STATION_ID_VARNAME = "station";
	public static final DataColumnType STATION_ID = new DataColumnType(STATION_ID_VARNAME,
			200.0, "station ID", "station", true, NO_UNITS);
	public static final String CAST_ID_VARNAME = "cast";
	public static final DataColumnType CAST_ID = new DataColumnType(CAST_ID_VARNAME,
			201.0, "cast ID", "cast", true, NO_UNITS);
	public static final String NISKIN_VARNAME = "niskin";
	public static final DataColumnType NISKIN = new DataColumnType(NISKIN_VARNAME,
			202.0, "bottle ID", "niskin", false, NO_UNITS);

	/**
	 * User-provided unique ID for a sample in a dataset (user data type only). 
	 * Used when merging files of different data types measured for a sample.
	 */
	public static final String SAMPLE_ID_VARNAME = "sample_id";
	public static final DataColumnType SAMPLE_ID = new DataColumnType(SAMPLE_ID_VARNAME,
			300.0, "sample ID", "unique ID for this sample in the dataset", false, NO_UNITS);

	public static final String LONGITUDE_VARNAME = "longitude";
	public static final DataColumnType LONGITUDE = new DataColumnType(LONGITUDE_VARNAME,
			301.0, "longitude", "sample longitude", true, LONGITUDE_UNITS);

	public static final String LATITUDE_VARNAME = "latitude";
	public static final DataColumnType LATITUDE = new DataColumnType(LATITUDE_VARNAME,
			302.0, "latitude", "sample latitude", true, LATITUDE_UNITS);

	public static final String CTD_PRESSURE_VARNAME = "ctd_pressure";
	public static final DataColumnType CTD_PRESSURE = new DataColumnType(CTD_PRESSURE_VARNAME,
			303.0, "ctd pressure", "ctd pressure", true, PRESSURE_UNITS);

	public static final String SAMPLE_DEPTH_VARNAME = "sample_depth";
	public static final DataColumnType SAMPLE_DEPTH = new DataColumnType(SAMPLE_DEPTH_VARNAME,
			304.0, "depth - sample", "depth at which sample was taken", true, DEPTH_UNITS);

    public static final String TEMPERATURE_VARNAME = "temperature";
    public static final DataColumnType TEMPERATURE = new DataColumnType(TEMPERATURE_VARNAME, 
            305.0, "temperature", "temperature", false, TEMPERATURE_UNITS);
    public static final String SEA_SURFACE_TEMPERATURE_VARNAME = "sea_surface_temperature";
    public static final DataColumnType SEA_SURFACE_TEMP = new DataColumnType(SEA_SURFACE_TEMPERATURE_VARNAME, 
            306.0, "sea surface temp", "sea surface temperature", false, TEMPERATURE_UNITS);
	/**
	 * Date and time of the measurement
	 */
	public static final String TIMESTAMP_VARNAME = "date_time";
	public static final DataColumnType TIMESTAMP = new DataColumnType(TIMESTAMP_VARNAME,
			310.0, "date time", "sample date and time", false, TIMESTAMP_UNITS);

	/**
	 * Date of the measurement - no time.
	 */
	public static final String DATE_VARNAME = "date";
	public static final DataColumnType DATE = new DataColumnType(DATE_VARNAME,
			311.0, "date", "sample date", false, DATE_UNITS);

	public static final String YEAR_VARNAME = "year";
	public static final DataColumnType YEAR = new DataColumnType(YEAR_VARNAME,
			312.0, "year", "sample year", false, NO_UNITS);

	public static final String MONTH_OF_YEAR_VARNAME = "month";
	public static final DataColumnType MONTH_OF_YEAR = new DataColumnType(MONTH_OF_YEAR_VARNAME,
			313.0, "month of year", "sample month of year", false, NO_UNITS);
	
	public static final String DAY_OF_MONTH_VARNAME = "day";
	public static final DataColumnType DAY_OF_MONTH = new DataColumnType(DAY_OF_MONTH_VARNAME,
			314.0, "day of month", "sample day of month", false, NO_UNITS);

	public static final String TIME_OF_DAY_VARNAME = "time_of_day";
	public static final DataColumnType TIME_OF_DAY = new DataColumnType(TIME_OF_DAY_VARNAME,
			315.0, "time of day", "sample time of day", false, NO_UNITS); // TIME_OF_DAY_UNITS);

	public static final String HOUR_OF_DAY_VARNAME = "hour";
	public static final DataColumnType HOUR_OF_DAY = new DataColumnType(HOUR_OF_DAY_VARNAME,
			316.0, "hour of day", "sample hour of day", false, NO_UNITS);

	public static final String MINUTE_OF_HOUR_VARNAME = "minute";
	public static final DataColumnType MINUTE_OF_HOUR = new DataColumnType(MINUTE_OF_HOUR_VARNAME,
			317.0, "minute of hour", "sample minute of hour", false, NO_UNITS);

	public static final String SECOND_OF_MINUTE_VARNAME = "second";
	public static final DataColumnType SECOND_OF_MINUTE = new DataColumnType(SECOND_OF_MINUTE_VARNAME,
			318.0, "sec of minute", "sample second of minute", false, NO_UNITS);

	/**
	 * DAY_OF_YEAR, along with YEAR, and possibly SECOND_OF_DAY,
	 * may be used to specify the date and time of the measurement.
	 */
	public static final String DAY_OF_YEAR_VARNAME = "day_of_year";
	public static final DataColumnType DAY_OF_YEAR = new DataColumnType(DAY_OF_YEAR_VARNAME,
			320.0, "day of year", "sample day of year", false, DAY_OF_YEAR_UNITS);

	/**
	 * SECOND_OF_DAY, along with YEAR and DAY_OF_YEAR may
	 * be used to specify date and time of the measurement
	 */
	public static final String SECOND_OF_DAY_VARNAME = "sec_of_day";
	public static final DataColumnType SECOND_OF_DAY = new DataColumnType(SECOND_OF_DAY_VARNAME,
			321.0, "sec of day", "sample second of day", false, NO_UNITS);

	/**
	 * "Cleans" a username for use by substituting characters that are  
	 * problematic (such as space characters).  
	 * Also converts all alphabetic characters to lowercase.
	 * An empty string is returned if username is null.
	 * 
	 * @param username
	 * 		username to clean
	 * @return
	 * 		clean version of username
	 */
	public static String cleanUsername(String username) {
		if ( username == null )
			return "";
		return username.replace(' ', '_').toLowerCase();
	}

	/*-* This is not used anywhere
	 * Generate the encrypted password for a given plain-text username 
	 * and password.  This is intended to only be a first level of
	 * encryption.
	 * 
	 * @param username
	 * 		plain-text username to use
	 * @param password
	 * 		plain-text password to use 
	 * @return
	 * 		encrypted password, or an empty string if an error occurs 
	public static String passhashFromPlainText(String username, String password) {
		// This salt is just to make sure the keys are long enough
		String salt = "4z#Ni!q?F7b0m9nK(uDF[g%T3pD_";

		// Make sure something reasonable Strings are given
		if ( (username.length() < 7) || (password.length() < 7) ) {
			return "";
		}
		String name = cleanUsername(username);

		// Encrypt the password
		TripleDesCipher cipher = new TripleDesCipher();
		cipher.setKey((name.substring(0,6) + password + salt)
			  .substring(0,24).getBytes());
		String passhash;
		try {
			passhash = cipher.encrypt((password + salt).substring(0,32));
		} catch (Exception ex) {
			passhash = "";
		}

		return passhash;
	}
	 */

	/**
	 * Decodes a (JSON-like) encoded array of numbers into a byte array. 
	 * Numeric values are separated by a comma, which may have whitespace
	 * around it.
	 * 
	 * @param arrayStr
	 * 		JSON-encoded array of byte values to use
	 * @return
	 * 		a byte array represented arrayStr
	 * @throws NumberFormatException
	 * 		if arrayStr does not start with '[', does not end with ']', 
	 * 		or contains values inappropriate for the byte type
	 */
	public static byte[] decodeByteArray(String arrayStr) 
										throws NumberFormatException {
		if ( ! ( arrayStr.startsWith("[") && arrayStr.endsWith("]") ) )
			throw new NumberFormatException(
					"Encoded byte array not enclosed in brackets");
		String[] pieces = arrayStr.substring(1, arrayStr.length()-1)
								  .split("\\s*,\\s*", -1);
		if ( (pieces.length == 1) && pieces[0].trim().isEmpty() )
			return new byte[0];
		byte[] byteArray = new byte[pieces.length];
		for (int k = 0; k < pieces.length; k++)
			byteArray[k] = Byte.parseByte(pieces[k].trim());
		return byteArray;
	}

	/**
	 * Encodes an ArrayList of Integers suitable for decoding 
	 * with {@link #decodeIntegerArrayList(String)}
	 * 
	 * @param intList
	 * 		list of integer values to encode
	 * @return
	 * 		the encoded list of integer values
	 */
	public static String encodeIntegerArrayList(ArrayList<Integer> intList) {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		boolean firstValue = true;
		for ( Integer intVal : intList ) {
			if ( firstValue )
				firstValue = false;
			else
				sb.append(", ");
			sb.append(intVal.toString());
		}
		sb.append(" ]");
		return sb.toString();
	}

	/**
	 * Decodes a encoded array of numbers produced by 
	 * {@link #encodeIntegerArrayList(ArrayList)}
	 * into an ArrayList of Integers.
	 * 
	 * @param arrayStr
	 * 		encoded array of integer values to use
	 * @return
	 * 		the decoded ArrayList of Integers; never null but may be empty
	 * @throws NumberFormatException
	 * 		if arrayStr does not start with '[', does not end with ']', 
	 * 		or contains values inappropriate for the integer type
	 */
	public static ArrayList<Integer> decodeIntegerArrayList(String arrayStr) 
										throws NumberFormatException {
		if ( ! ( arrayStr.startsWith("[") && arrayStr.endsWith("]") ) )
			throw new NumberFormatException(
					"Encoded integer array not enclosed in brackets");
		String[] pieces = arrayStr.substring(1, arrayStr.length()-1)
								  .split("\\s*,\\s*", -1);
		if ( (pieces.length == 1) && pieces[0].trim().isEmpty() )
			return new ArrayList<Integer>(0);
		ArrayList<Integer> intList = new ArrayList<Integer>(pieces.length);
		for ( String strVal : pieces )
			intList.add(Integer.parseInt(strVal.trim()));
		return intList;
	}

	/**
	 * Encodes an Collection of Strings suitable for decoding using 
	 * {@link #decodeStringArrayList(String)}.  Characters within
	 * the strings are copied as-is, thus newline characters, or
	 * the character sequence double quote - comma - double quote, 
	 * within a string will likely cause problems when reading or 
	 * decoding the encoded string.
	 * 
	 * @param strList
	 * 		the ArrayList of strings to encode
	 * @return
	 * 		the encoded string array
	 */
	public static String encodeStringArrayList(Collection<String> strList) {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		boolean firstValue = true;
		for ( String strVal : strList ) {
			if ( firstValue )
				firstValue = false;
			else
				sb.append(", ");
			sb.append("\"");
			sb.append(strVal);
			sb.append("\"");
		}
		sb.append(" ]");
		return sb.toString();
	}

	/**
	 * Decodes an encoded string array produced by 
	 * {@link #encodeStringArrayList(ArrayList)}, into an 
	 * ArrayList of strings.  Each string must be enclosed in double 
	 * quotes; escaped characters within a string are not recognized 
	 * or modified.  Strings must be separated by commas.  Whitespace 
	 * around the comma is allowed.
	 * 
	 * @param arrayStr
	 * 		the encoded string array
	 * @return
	 * 		the decoded ArrayList of strings; never null, but may
	 * 		be empty (if the encoded string array contains no strings)
	 * @throws IllegalArgumentException
	 * 		if arrayStr does not start with '[', does not end with ']', 
	 * 		or contains strings not enclosed within double quotes.
	 */
	public static ArrayList<String> decodeStringArrayList(String arrayStr) 
									throws IllegalArgumentException {
		if ( ! ( arrayStr.startsWith("[") && arrayStr.endsWith("]") ) )
		{
			ArrayList<String> single = new ArrayList<>();
			String trimmed = arrayStr.trim();
			if ( trimmed.startsWith("\"") && arrayStr.endsWith("\"")) {
				single.add(trimmed.substring(1, trimmed.length()-1));
			} else {
				single.add(arrayStr);
			}
			return single;
		}
//			throw new IllegalArgumentException(
//					"Encoded string array not enclosed in brackets");
		String contents = arrayStr.substring(1, arrayStr.length() - 1);
		if ( contents.trim().isEmpty() )
			return new ArrayList<String>(0);
		int firstIndex = contents.indexOf("\"");
		int lastIndex = contents.lastIndexOf("\"");
		if ( (firstIndex < 0) || (lastIndex == firstIndex) ||
			 ( ! contents.substring(0, firstIndex).trim().isEmpty() ) ||
			 ( ! contents.substring(lastIndex+1).trim().isEmpty() ) )
			throw new IllegalArgumentException("Strings in encoded " +
					"string array are not enclosed in double quotes");
		String[] pieces = contents.substring(firstIndex+1, lastIndex)
								  .split("\"\\s*,\\s*\"", -1);
		return new ArrayList<String>(Arrays.asList(pieces));
	}

	/**
	 * Encodes a set of QCFlag objects suitable for decoding with {@link #decodeQCFlagSet(String)}. 
	 * The comments in the QCFlag objects are ignored.
	 * 
	 * @param qcSet
	 * 		set of QCFlag values to encode
	 * @return
	 * 		the encoded list of QCFlag values
	 */
	public static String encodeQCFlagSet(TreeSet<QCFlag> qcSet) {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		boolean firstValue = true;
		for ( QCFlag flag : qcSet ) {
			if ( firstValue )
				firstValue = false;
			else
				sb.append(", ");
			sb.append("[ ");
			sb.append(flag.getRowIndex().toString());
			sb.append(", ");
			sb.append(flag.getColumnIndex().toString());
			sb.append(", \"");
			sb.append(flag.getSeverity().toString());
			sb.append("\", \"");
			sb.append(flag.getFlagValue());
			sb.append("\", \"");
			sb.append(flag.getFlagName());
			sb.append("\" ]");
		}
		sb.append(" ]");
		return sb.toString();
	}

	/**
	 * Decodes an encoded QCFlag set produced by {@link #encodeQCFlagSet(java.util.TreeSet)}, 
	 * into a TreeSet of QCFlags. 
	 * 
	 * @param qcFlagSetStr
	 * 		the encoded set of QCFlag objects
	 * @return
	 * 		the decoded TreeSet ofQCFlag objects; never null, but may
	 * 		be empty (if the encoded set does not specify any QCFlag objects)
	 * @throws IllegalArgumentException
	 * 		if qcFlagSetStr does not start with '[', does not end with ']', 
	 * 		or contains an invalid encoded QCFlag.
	 */
	public static TreeSet<QCFlag> decodeQCFlagSet(String qcFlagSetStr) {
		if ( ! ( qcFlagSetStr.startsWith("[") && qcFlagSetStr.endsWith("]") ) )
			throw new IllegalArgumentException("Encoded QCFlag set not enclosed in brackets");
		String contents = qcFlagSetStr.substring(1, qcFlagSetStr.length() - 1);
		if ( contents.trim().isEmpty() )
			return new TreeSet<QCFlag>();
		int firstIndex = contents.indexOf("[");
		int lastIndex = contents.lastIndexOf("]");
		if ( (firstIndex < 0) || (lastIndex < 0) || 
			 ( ! contents.substring(0, firstIndex).trim().isEmpty() ) ||
			 ( ! contents.substring(lastIndex+1).trim().isEmpty() ) )
			throw new IllegalArgumentException("A QCFlag encoding is not enclosed in brackets");
		String[] pieces = contents.substring(firstIndex+1, lastIndex)
								  .split("\\]\\s*,\\s*\\[", -1);
		TreeSet<QCFlag> flagSet = new TreeSet<QCFlag>();
		for ( String encFlag : pieces ) {
			String[] flagParts = encFlag.split(",", 5);
			try {
				if ( flagParts.length != 5 )
					throw new IllegalArgumentException("incomplete QCFlag description");

				Integer rowIndex = Integer.parseInt(flagParts[0].trim());

				Integer colIndex = Integer.parseInt(flagParts[1].trim());

				firstIndex = flagParts[2].indexOf("\"");
				lastIndex = flagParts[2].lastIndexOf("\"");
				if ( (firstIndex < 1) || (lastIndex == firstIndex) ||
					 ( ! flagParts[2].substring(0, firstIndex).trim().isEmpty() ) ||
					 ( ! flagParts[2].substring(lastIndex+1).trim().isEmpty() ) )
					throw new IllegalArgumentException("severity not enclosed in double quotes");
				Severity severity = Severity.valueOf(flagParts[2].substring(firstIndex+1, lastIndex));

				firstIndex = flagParts[3].indexOf("\"");
				lastIndex = flagParts[3].lastIndexOf("\"");
				if ( (firstIndex < 1) || (lastIndex == firstIndex) ||
					 ( ! flagParts[3].substring(0, firstIndex).trim().isEmpty() ) ||
					 ( ! flagParts[3].substring(lastIndex+1).trim().isEmpty() ) )
					throw new IllegalArgumentException("flag value not enclosed in double quotes");
				String flagValue = flagParts[3].substring(firstIndex+1, lastIndex);
				if ( flagValue.length() != 1 )
					throw new IllegalArgumentException("flag value is not a single character");

				firstIndex = flagParts[4].indexOf("\"");
				lastIndex = flagParts[4].lastIndexOf("\"");
				if ( (firstIndex < 1) || (lastIndex == firstIndex) ||
					 ( ! flagParts[4].substring(0, firstIndex).trim().isEmpty() ) ||
					 ( ! flagParts[4].substring(lastIndex+1).trim().isEmpty() ) )
					throw new IllegalArgumentException("flag name not enclosed in double quotes");
				String flagName = flagParts[4].substring(firstIndex+1, lastIndex);

				flagSet.add(new QCFlag(flagName, flagValue.charAt(0), severity, colIndex, rowIndex));
			} catch ( Exception ex ) {
				throw new IllegalArgumentException("Invalid encoding of a set of QCFlag objects: " + ex.getMessage(), ex);
			}
		}
		return flagSet;
	}

	/**
	 * Returns the basename of a filename.  Does this by returning only the
	 * portion of the string after the last slash or backslash character 
	 * (either one if both present).
	 * 
	 * If null is given, or if the name ends in a slash or backslash, an empty 
	 * string is returned.  Whitespace is trimmed from the returned name.
	 */
	public static String baseName(String filename) {
		if ( filename == null )
			return "";

		String basename = filename;
		int idx = basename.lastIndexOf('/');
		if ( idx >= 0 ) {
			idx++;
			if ( basename.length() == idx )
				return "";
			else
				basename = basename.substring(idx);
		}
		idx = basename.lastIndexOf('\\');
		if ( idx >= 0 ) {
			idx++;
			if ( basename.length() == idx )
				return "";
			else
				basename = basename.substring(idx);
		}
		return basename.trim();
	}

	/**
	 * Determines if two longitudes are close to the same value
	 * modulo 360.0.  The absolute of the average value, absAver, 
	 * and the absolute value in the difference in values, absDiff,
	 * of first and second are determined.
	 *  
	 * The difference between is considered negligible if: 
	 *     absDiff < absAver * rtol + atol 
	 * 
	 * This comparison is made to the values as given as well as for
	 * each value with 360.0 added to it.  
	 * (So not a complete modulo 360 check.)
	 * 
	 * @param first 
	 * 		value to compare
	 * @param second 
	 * 		value to compare
	 * @param rtol
	 * 		relative tolerance of the difference
	 * @param atol
	 * 		absolute tolerance of the difference
	 * @return 
	 * 		true is first and second are both NaN, both Infinite
	 * 		(regardless of whether positive or negative), or 
	 * 		have values whose difference is "negligible".
	 */
	public static boolean longitudeCloseTo(Double first, Double second, 
										double rtol, double atol) {
		// Longitudes have modulo 360.0, so 359.999999 is close to 0.0
		if ( closeTo(first, second, rtol, atol) )
			return true;
		if ( closeTo(first + 360.0, second, rtol, atol) )
			return true;
		if ( closeTo(first, second + 360.0, rtol, atol) )
			return true;
		return false;
	}

	/**
	 * Determines if two Doubles are close to the same value.
	 * The absolute of the average value, absAver, and the 
	 * absolute value in the difference in values, absDiff,
	 * of first and second are determined.
	 *  
	 * The difference between is considered negligible if: 
	 *     absDiff < absAver * rtol + atol 
	 * 
	 * @param first 
	 * 		value to compare
	 * @param second 
	 * 		value to compare
	 * @param rtol
	 * 		relative tolerance of the difference
	 * @param atol
	 * 		absolute tolerance of the difference
	 * @return 
	 * 		true is first and second are both NaN, both Infinite
	 * 		(regardless of whether positive or negative), or 
	 * 		have values whose difference is "negligible".
	 */
	public static boolean closeTo(Double first, Double second, 
									double rtol, double atol) {

		if ( first == null || second == null ) { return false; }
		
		// NaN (only) matches NaN
		if ( first.isNaN() ) {
			return second.isNaN();
		}
		if ( second.isNaN() ) {
			return false;
		}

		// Positive or negative infinity (only) matches 
		// positive or negative infinity
		if ( first.isInfinite() ) {
			return second.isInfinite();
		}
		if ( second.isInfinite() ) {
			return false;
		}

		// Check if they are the same value
		if ( first.equals(second) )
			return true;

		// Check if values are close
		double absDiff = Math.abs(first - second);
		double absAver = Math.abs((first + second) * 0.5);
		return ( absDiff < absAver * rtol + atol );
	}
	
	public static boolean isEmptyNullOrNull(Object value) {
		return value == null || "null".equalsIgnoreCase(String.valueOf(value)) ||
				(( value instanceof String ) && ((String)value).trim().length() == 0);
	}
	
	public static boolean isEmptyOrNull(String value) {
		return value == null || value.trim().length() == 0;
	}
	
	public static boolean isEmptyNullOrNull(String value) {
		return isEmptyOrNull(value) || "null".equalsIgnoreCase(String.valueOf(value));
	}
	
    public static boolean isNullOrNull(Object value) {
        return value == null || "null".equalsIgnoreCase(String.valueOf(value));
    }

//	public static boolean isOrNullOrNull(Object value) {
//		return  || "null".equalsIgnoreCase(String.valueOf(value));
//	}
	public static final String DATE_ARCHIVE_FORMAT_NO_SEC = "yyyy-MM-dd HH:mm 'Z'";
	public static final String DATE_ARCHIVE_FORMAT = "yyyy-MM-dd HH:mm:ss 'Z'";
	public static final String DATE_FORMAT_TO_MINUTES_NO_TZ = "yyyy-MM-dd HH:mm";
	public static final String DATE_FORMAT_TO_SECONDS_NO_TZ = "yyyy-MM-dd HH:mm:ss";
	public static final String LOCALIZED_DATE_FORMAT_TO_MINUTES = "yyyy-MM-dd HH:mm Z";
	public static final String LOCALIZED_DATE_FORMAT_TO_SECONDS = "yyyy-MM-dd HH:mm:ss Z";
    private static String[] parseFormats = new String[] {
            DATE_ARCHIVE_FORMAT,
            DATE_ARCHIVE_FORMAT_NO_SEC,
            LOCALIZED_DATE_FORMAT_TO_MINUTES,
            LOCALIZED_DATE_FORMAT_TO_SECONDS
    };
    
    public static String formatClientSideDate(Date date, String format) {
        if ( date == null ) {
            return STRING_MISSING_VALUE;
        }
        return DateTimeFormat.getFormat(format).format(date);
    }
    
    /**
     * 
     * @param dateString
     * @param format
     * @return java.util.Date
     * @throws IllegalArgumentException if the entire dateString cannot be parsed into a date
     *         using the given format;
     */
    public static Date getClientSideDate(String dateString, String format) {
        if ( isEmptyOrNull(dateString)) {
            return DATE_MISSING_VALUE;
        }
//                uploadDate = DateTimeFormat.getFormat("yyyy-MM-dd hh:mm Z").parse(uploadTimestamp);
//                uploadDate = DateTimeFormat.getFormat("yyyy-MM-dd hh:mm Z").parse(uploadTimestamp);
        return DateTimeFormat.getFormat(format).parse(dateString);
    }
    
    public static Date parseDateOnClient(String dateString) {
        Date parsedDate = null;
        for (String format : parseFormats) {
            DateTimeFormat dtf = DateTimeFormat.getFormat(format);
            try {
                parsedDate = dtf.parseStrict(dateString);
                GWT.log("parsed " + dateString + " using " + format + " as " + parsedDate);
                return parsedDate;
            } catch (Exception ex) {
                GWT.log("Cannot parse " + dateString + " using " + format);
            }
        }
        return DATE_MISSING_VALUE;
    }
//    private static Date parseDateOnServer(String dateString) {
//        Date parsedDate = null;
//        for (String format : parseFormats) {
//            SimpleDateFormat dtf = new SimpleDateFormat(format);
//            try {
//                if ( dateString.contains("Z")) {
//                    dtf.setTimeZone(TimeZone.getTimeZone("UTC"));
//                }
//                parsedDate = dtf.parse(dateString);
//                return parsedDate;
//            } catch (Exception ex) {
//                System.err.println(ex + " with " + format);
//            }
//        }
//        return DATE_MISSING_VALUE;
//    }
    public static void main(String[] args) {
        System.out.println("Missing: " + DATE_MISSING_VALUE);
//        try {
//            String s1 = "2020-07-22 17:46:29 Z";
//            String s1b = "2020-07-22 17:46 Z";
//            String s2 = "2020-06-17 13:24:42 -0700";
//            String s3 = "2020-06-17 12:53 UTC";
//            String s4 = "2020-06-11 10:08 -0000";
//            String s5 = "2020-06-11 10:08 PST";
//            Date d = parseDateOnServer(s1);
//            System.out.println(s1 + " : " + d);
//            d = parseDateOnServer(s1b);
//            System.out.println(s1b + " : " + d);
//            d = parseDateOnServer(s2);
//            System.out.println(s2 + " : " + d);
//            d = parseDateOnServer(s3);
//            System.out.println(s3 + " : " + d);
//            d = parseDateOnServer(s4);
//            System.out.println(s4 + " : " + d);
//            d = parseDateOnServer(s5);
//            System.out.println(s5 + " : " + d);
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            // TODO: handle exception
//        }
    }
    
}
