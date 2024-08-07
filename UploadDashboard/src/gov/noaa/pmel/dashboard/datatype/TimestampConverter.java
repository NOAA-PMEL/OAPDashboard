/**
 * 
 */
package gov.noaa.pmel.dashboard.datatype;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.tws.util.StringUtils;

/**
 * For converting various timestamp, date, and time-of-day strings (various 
 * orderings of year, month, and day as well as various separators to standard 
 * formats of yyyy-MM-dd HH:mm:ss.SSS for timestamps, yyyy-MM-dd for dates and 
 * HH:mm:ss.SSS for times-of-day.
 * 
 * @author Karl Smith
 */
public class TimestampConverter extends ValueConverter<String> {

	private static final Logger logger = LogManager.getLogger(TimestampConverter.class);
	
	// TreeSet and TreeMap so can do case insensitive comparisons
	private static final TreeSet<String> SUPPORTED_FROM_UNITS;
	private static final TreeMap<String,Integer> MONTH_NAMES_MAP;
	static {
		SUPPORTED_FROM_UNITS = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		// timestamps
		SUPPORTED_FROM_UNITS.add("from \"yyyy-mm-dd hh:mm:ss\" to \"yyyy-mm-dd hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"mm-dd-yyyy hh:mm:ss\" to \"yyyy-mm-dd hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"dd-mm-yyyy hh:mm:ss\" to \"yyyy-mm-dd hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"mon-dd-yyyy hh:mm:ss\" to \"yyyy-mm-dd hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"dd-mon-yyyy hh:mm:ss\" to \"yyyy-mm-dd hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"mm-dd-yy hh:mm:ss\" to \"yyyy-mm-dd hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"dd-mm-yy hh:mm:ss\" to \"yyyy-mm-dd hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"mon-dd-yy hh:mm:ss\" to \"yyyy-mm-dd hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"dd-mon-yy hh:mm:ss\" to \"yyyy-mm-dd hh:mm:ss\"");
		// date only
		SUPPORTED_FROM_UNITS.add("from \"yyyy-mm-dd\" to \"yyyy-mm-dd\"");
		SUPPORTED_FROM_UNITS.add("from \"mm-dd-yyyy\" to \"yyyy-mm-dd\"");
		SUPPORTED_FROM_UNITS.add("from \"dd-mm-yyyy\" to \"yyyy-mm-dd\"");
		SUPPORTED_FROM_UNITS.add("from \"mm-dd-yy\" to \"yyyy-mm-dd\"");
		SUPPORTED_FROM_UNITS.add("from \"dd-mm-yy\" to \"yyyy-mm-dd\"");
		SUPPORTED_FROM_UNITS.add("from \"dd-mon-yyyy\" to \"yyyy-mm-dd\"");
		SUPPORTED_FROM_UNITS.add("from \"dd-mon-yy\" to \"yyyy-mm-dd\"");
		SUPPORTED_FROM_UNITS.add("from \"mon-dd-yyyy\" to \"yyyy-mm-dd\"");
		// time only
		SUPPORTED_FROM_UNITS.add("from \"hh:mm:ss\" to \"hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"hh:mm\" to \"hh:mm:ss\"");
		SUPPORTED_FROM_UNITS.add("from \"\" to \"hh:mm:ss\"");

		// recognized month names
		MONTH_NAMES_MAP = new TreeMap<String,Integer>(String.CASE_INSENSITIVE_ORDER);
		MONTH_NAMES_MAP.put("JAN", 1);
		MONTH_NAMES_MAP.put("JANUARY", 1);
		MONTH_NAMES_MAP.put("FEB", 2);
		MONTH_NAMES_MAP.put("FEBRUARY", 2);
		MONTH_NAMES_MAP.put("MAR", 3);
		MONTH_NAMES_MAP.put("MARCH", 3);
		MONTH_NAMES_MAP.put("APR", 4);
		MONTH_NAMES_MAP.put("APRIL", 4);
		MONTH_NAMES_MAP.put("MAY", 5);
		MONTH_NAMES_MAP.put("JUN", 6);
		MONTH_NAMES_MAP.put("JUNE", 6);
		MONTH_NAMES_MAP.put("JUL", 7);
		MONTH_NAMES_MAP.put("JULY", 7);
		MONTH_NAMES_MAP.put("AUG", 8);
		MONTH_NAMES_MAP.put("AUGUST", 8);
		MONTH_NAMES_MAP.put("SEP", 9);
		MONTH_NAMES_MAP.put("SEPT", 9);
		MONTH_NAMES_MAP.put("SEPTEMBER", 9);
		MONTH_NAMES_MAP.put("OCT", 10);
		MONTH_NAMES_MAP.put("OCTOBER", 10);
		MONTH_NAMES_MAP.put("NOV", 11);
		MONTH_NAMES_MAP.put("NOVEMBER", 11);
		MONTH_NAMES_MAP.put("DEC", 12);
		MONTH_NAMES_MAP.put("DECEMBER", 12);
	}

	private static final Pattern TIMESTAMP_SPLIT_PATTERN = Pattern.compile("[T ]");
	private static final Pattern DATE_SPLIT_PATTERN = Pattern.compile("[/, -]+");
	private static final Pattern TIME_SPLIT_PATTERN = Pattern.compile(":");

	private GregorianCalendar utcCalendar;
	private long millisNow;
	private int currYear;

	public TimestampConverter(String inputUnit, String outputUnit, String missingValue)
			throws IllegalArgumentException, IllegalStateException {
		super(inputUnit, outputUnit, missingValue);
		String key = "from \"" + fromUnit + "\" to \"" + toUnit + "\"";
		if ( ! SUPPORTED_FROM_UNITS.contains(key) )
			throw new IllegalArgumentException("conversion " + key + " not supported");
		utcCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		utcCalendar.setLenient(false);
		millisNow = System.currentTimeMillis();
		utcCalendar.setTimeInMillis(millisNow);
		currYear = utcCalendar.get(GregorianCalendar.YEAR);
	}

	@Override
	public String convertValueOf(String valueString, int recordNumber) throws IllegalArgumentException, IllegalStateException {
		// Deal with missing values
		if ( isMissingValue(valueString, true) )
			return null;
		String stdVal;
		if ( "yyyy-mm-dd hh:mm:ss".equalsIgnoreCase(toUnit) ) {
			String[] dateTime = splitTimestamp(valueString);
			String dateStdVal = standardizeDate(dateTime[0]);
			String timeStdVal = standardizeTime(dateTime[1]);
			stdVal = dateStdVal + " " + timeStdVal;
		}
		else if ( "yyyy-mm-dd".equalsIgnoreCase(toUnit) ) {
			stdVal = standardizeDate(valueString);
		}
		else if ( "hh:mm:ss".equalsIgnoreCase(toUnit) ) {
			stdVal = standardizeTime(valueString);
		}
		else {
			throw new IllegalArgumentException("conversion to \"" + toUnit + "\" is not supported");
		}
		return stdVal;
	}

	/**
	 * Split a timestamp into the date and time parts of the timestamp
	 * 
	 * @param valueString
	 * 		timestamp to use
	 * @return
	 * 		array of two Strings, the first being the date part of the timestamp
	 * 		and the second being the time part of the timestamp
	 * @throws IllegalArgumentException
	 * 		if the fromUnit format is not recognized, or 
	 * 		if the value is not a valid timestamp (no space or 'T' divider)
	 */
	private String[] splitTimestamp(String valueString) throws IllegalArgumentException {
		String datePiece;
		String timePiece;
		if ( "yyyy-mm-dd hh:mm:ss".equalsIgnoreCase(fromUnit) || 
			 "mm-dd-yyyy hh:mm:ss".equalsIgnoreCase(fromUnit) || 
			 "dd-mm-yyyy hh:mm:ss".equalsIgnoreCase(fromUnit) || 
			 "mm-dd-yy hh:mm:ss".equalsIgnoreCase(fromUnit) || 
			 "dd-mm-yy hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			// Date could possibly have spaces; split and then concatenate everything 
			// except the last as the date piece, and the last is the time piece.
			try {
				String[] pieces = TIMESTAMP_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length < 2 )
					throw new Exception();
				datePiece = pieces[0];
				for (int k = 1; k < (pieces.length - 1); k++)
					datePiece += " " + pieces[k];
				timePiece = pieces[pieces.length-1];
			} catch ( Exception ex ) {
				throw new IllegalArgumentException("not a valid timestamp value");
			}
		}
		else if ( "mon-dd-yyyy hh:mm:ss".equalsIgnoreCase(fromUnit) || 
				  "dd-mon-yyyy hh:mm:ss".equalsIgnoreCase(fromUnit) || 
				  "mon-dd-yy hh:mm:ss".equalsIgnoreCase(fromUnit) || 
				  "dd-mon-yy hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			// Date could possibly have spaces, or the month be SEPT or OCT; change 
			// SEPT to Sept, OCT to Oct, split, and then concatenate everything 
			// except the last as the date piece, and the last is the time piece.
			try {
				String otherString = valueString.replace("SEPT", "Sept").replace("OCT", "Oct");
				String[] pieces = TIMESTAMP_SPLIT_PATTERN.split(otherString, 0);
				if ( pieces.length >= 2 ) {
					datePiece = pieces[0];
					for (int k = 1; k < (pieces.length - 1); k++)
						datePiece += " " + pieces[k];
					timePiece = pieces[pieces.length-1];
				}
				else {
					throw new Exception();
				}
			} catch ( Exception ex ) {
				throw new IllegalArgumentException("not a valid timestamp value");
			}
		}
		else {
			throw new IllegalArgumentException("conversion from \"" + fromUnit + "\" is not supported");
		}
		return new String[] { datePiece, timePiece };
	}

	/**
	 * Standardize a date value to yyyy-MM-dd
	 * 
	 * @param valueString
	 * 		date string in the format of fromUnit
	 * @return
	 * 		standardized date string
	 * @throws IllegalArgumentException
	 * 		if the fromUnit format is not recognized, or
	 * 		if the value is not a valid date string
	 */
	private String standardizeDate(String valueString) throws IllegalArgumentException {
		Integer year;
		Integer month;
		Integer day;
		if ( "yyyy-mm-dd".equalsIgnoreCase(fromUnit) || 
			 "yyyy-mm-dd hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			try {
				String[] pieces = DATE_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length != 3 ) {
					if ( valueString.length() == 8 ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, 4);
						pieces[1] = valueString.substring(4, 6);
						pieces[2] = valueString.substring(6);
					}
					else
						throw new IllegalArgumentException("Invalid date string " + valueString + " for given format: " + fromUnit);
				}
				year = Integer.valueOf(pieces[0]);
				month = Integer.valueOf(pieces[1]);
				day = Integer.valueOf(pieces[2]);
			} catch ( Exception ex ) {
				year = -1;
				month = -1;
				day = -1;
			}
		}
		else if ( "mm-dd-yyyy".equalsIgnoreCase(fromUnit) || 
				  "mm-dd-yyyy hh:mm:ss".equalsIgnoreCase(fromUnit)) {
			try {
				String[] pieces = DATE_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length != 3 ) {
					if ( valueString.length() == 8 ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, 2);
						pieces[1] = valueString.substring(2, 4);
						pieces[2] = valueString.substring(4);
					} else if ( valueString.length() == 7 ) { // assume single digit month
						pieces = new String[3];
						pieces[0] = valueString.substring(0, 1);
						pieces[1] = valueString.substring(1, 3);
						pieces[2] = valueString.substring(3);
					} else
						throw new IllegalArgumentException("Invalid date string " + valueString + " for given format: " + fromUnit);
				}
				month = Integer.valueOf(pieces[0]);
				day = Integer.valueOf(pieces[1]);
				year = Integer.valueOf(pieces[2]);
			} catch ( Exception ex ) {
				month = -1;
				day = -1;
				year = -1;
			}
		}
		else if ( "dd-mm-yyyy".equalsIgnoreCase(fromUnit) || 
				  "dd-mm-yyyy hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			try {
				String[] pieces = DATE_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length != 3 ) {
					if ( valueString.length() == 8 ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, 2);
						pieces[1] = valueString.substring(2, 4);
						pieces[2] = valueString.substring(4);
					}
					else
						throw new IllegalArgumentException("Invalid date string " + valueString + " for given format: " + fromUnit);
				}
				day = Integer.valueOf(pieces[0]);
				month = Integer.valueOf(pieces[1]);
				year = Integer.valueOf(pieces[2]);
			} catch ( Exception ex ) {
				day = -1;
				month = -1;
				year = -1;
			}
		}
		else if ( "dd-mon-yyyy".equalsIgnoreCase(fromUnit) || 
                  "dd-mon-yy".equalsIgnoreCase(fromUnit) || 
				  "dd-mon-yyyy hh:mm:ss".equalsIgnoreCase(fromUnit) ||
				  "dd-mon-yy hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			try {
				String[] pieces = DATE_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length != 3 ) {
					int monIdx = -1;
					int yearIdx = -1;
					for (int k = 0; k < valueString.length(); k++) {
						if ( Character.isLetter(valueString.charAt(k)) ) {
							monIdx = k;
							break;
						}
					}
					if ( monIdx > 0 ) {
						for (int k = monIdx; k < valueString.length(); k++) {
							if ( Character.isDigit(valueString.charAt(k)) ) {
								yearIdx = k;
								break;
							}
						}
					}
					if ( (monIdx > 0) && (yearIdx > monIdx) ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, monIdx);
						pieces[1] = valueString.substring(monIdx, yearIdx);
						pieces[2] = valueString.substring(yearIdx);
					}
					else
						throw new IllegalArgumentException("Invalid date string " + valueString + " for given format: " + fromUnit);
				}
				day = Integer.valueOf(pieces[0]);
				month = MONTH_NAMES_MAP.get(pieces[1]);
				year = Integer.valueOf(pieces[2]);
                if ( pieces[2].length() == 2) {
    				int century = currYear / 100;
    				year += century * 100;
    				if ( year > currYear ) {
    					year -= 100;
    				}
                }
			} catch ( Exception ex ) {
				day = -1;
				month = -1;
				year = -1;
			}
		}
		else if ( "mon-dd-yyyy".equalsIgnoreCase(fromUnit) || 
				  "mon-dd-yyyy hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			try {
				String[] pieces = DATE_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length != 3 ) {
					int dayIdx = -1;
					for (int k = 1; k < valueString.length(); k++) {
						if ( Character.isDigit(valueString.charAt(k)) ) {
							dayIdx = k;
							break;
						}
					}
					if ( (dayIdx > 0) && (valueString.length() == (dayIdx + 6)) ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, dayIdx);
						pieces[1] = valueString.substring(dayIdx, dayIdx+2);
						pieces[2] = valueString.substring(dayIdx+2);
					}
					else
						throw new IllegalArgumentException("Invalid date string " + valueString + " for given format: " + fromUnit);
				}
				month = MONTH_NAMES_MAP.get(pieces[0]);
				day = Integer.valueOf(pieces[1]);
				year = Integer.valueOf(pieces[2]);
			} catch ( Exception ex ) {
				month = -1;
				day = -1;
				year = -1;
			}
		}
		else if ( "mm-dd-yy".equalsIgnoreCase(fromUnit) || 
				  "mm-dd-yy hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			try {
				String[] pieces = DATE_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length != 3 ) {
					if ( valueString.length() == 6 ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, 2);
						pieces[1] = valueString.substring(2, 4);
						pieces[2] = valueString.substring(4);
					}
					else {
						throw new IllegalArgumentException("Invalid date string " + valueString + " for given format: " + fromUnit);
					}
				}
				month = Integer.valueOf(pieces[0]);
				day = Integer.valueOf(pieces[1]);
				year = Integer.valueOf(pieces[2]);
				int century = currYear / 100;
				year += century * 100;
				if ( year > currYear )
					year -= 100;
			} catch ( Exception ex ) {
				month = -1;
				day = -1;
				year = -1;
			}
		}
		else if ( "dd-mm-yy".equalsIgnoreCase(fromUnit) || 
				  "dd-mm-yy hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			try {
				String[] pieces = DATE_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length != 3 ) {
					if ( valueString.length() == 6 ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, 2);
						pieces[1] = valueString.substring(2, 4);
						pieces[2] = valueString.substring(4);
					}
					else {
						throw new IllegalArgumentException("Invalid date string " + valueString + " for given format: " + fromUnit);
					}
				}
				day = Integer.valueOf(pieces[0]);
				month = Integer.valueOf(pieces[1]);
				year = Integer.valueOf(pieces[2]);
				int century = currYear / 100;
				year += century * 100;
				if ( year > currYear )
					year -= 100;
			} catch ( Exception ex ) {
				day = -1;
				month = -1;
				year = -1;
			}
		}
		else if ( "dd-mon-yy".equalsIgnoreCase(fromUnit) || 
				  "dd-mon-yy hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			try {
				String[] pieces = DATE_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length != 3 ) {
					int monIdx = -1;
					int yearIdx = -1;
					for (int k = 0; k < valueString.length(); k++) {
						if ( Character.isLetter(valueString.charAt(k)) ) {
							monIdx = k;
							break;
						}
					}
					if ( monIdx > 0 ) {
						for (int k = monIdx; k < valueString.length(); k++) {
							if ( Character.isDigit(valueString.charAt(k)) ) {
								yearIdx = k;
								break;
							}
						}
					}
					if ( (monIdx > 0) && (yearIdx > monIdx) ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, monIdx);
						pieces[1] = valueString.substring(monIdx, yearIdx);
						pieces[2] = valueString.substring(yearIdx);
					}
					else
						throw new IllegalArgumentException("Invalid date string " + valueString + " for given format: " + fromUnit);
				}
				day = Integer.valueOf(pieces[0]);
				month = MONTH_NAMES_MAP.get(pieces[1]);
				year = Integer.valueOf(pieces[2]);
				int century = currYear / 100;
				year += century * 100;
				if ( year > currYear )
					year -= 100;
			} catch ( Exception ex ) {
				day = -1;
				month = -1;
				year = -1;
			}
		}
		else if ( "mon-dd-yy".equalsIgnoreCase(fromUnit) || 
				  "mon-dd-yy hh:mm:ss".equalsIgnoreCase(fromUnit) ) {
			try {
				String[] pieces = DATE_SPLIT_PATTERN.split(valueString, 0);
				if ( pieces.length != 3 ) {
					int dayIdx = -1;
					for (int k = 1; k < valueString.length(); k++) {
						if ( Character.isDigit(valueString.charAt(k)) ) {
							dayIdx = k;
							break;
						}
					}
					if ( (dayIdx > 0) && (valueString.length() == (dayIdx + 4)) ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, dayIdx);
						pieces[1] = valueString.substring(dayIdx, dayIdx+2);
						pieces[2] = valueString.substring(dayIdx+2);
					}
					else
						throw new IllegalArgumentException("Invalid date string " + valueString + " for given format: " + fromUnit);
				}
				month = MONTH_NAMES_MAP.get(pieces[0]);
				day = Integer.valueOf(pieces[1]);
				year = Integer.valueOf(pieces[2]);
				int century = currYear / 100;
				year += century * 100;
				if ( year > currYear )
					year -= 100;
			} catch ( Exception ex ) {
				month = -1;
				day = -1;
				year = -1;
			}
		}
		else {
			throw new IllegalArgumentException("conversion from \"" + fromUnit + "\" is not supported");
		}
		if ( (year == null) || (year < 1900) || (year > currYear) || 
			 (month == null) || (month < 1) || (month > 12) ||
			 (day == null) || (day < 1) || (day > 31) )
			throw new IllegalArgumentException("invalid date value " + valueString + " for format: " + fromUnit);
		try {
			// Check if the year-month-day is a valid combination
			utcCalendar.set(year, GregorianCalendar.JANUARY + month - 1, day, 0, 0, 0);
			utcCalendar.set(GregorianCalendar.MILLISECOND, 0);
			if ( utcCalendar.getTimeInMillis() > millisNow )
				throw new Exception();
		} catch ( Exception ex ) {
			throw new IllegalArgumentException("invalid date value: " + valueString);
		}
		String stdVal = String.format("%04d-%02d-%02d", year, month, day);		
		return stdVal;
	}

    
//    MM/dd/yy(yy)
//    yy(yy)/MM/dd
    
    static String datePatternString = "";
    static String timePatternString = "(\\d?\\d)?\\:?(\\d\\d)\\:?(\\d\\d)?(\\.\\d{1,3})?(\\d*)?\\s*([aApP][mM])?\\s*(.*)?";
    static String strictTimePatternString = "([01]?\\d|2[0-3])\\:?([0-5]\\d)\\:?([0-5]\\d)?(\\.\\d{1,3})?(\\d*)?\\s*([aApP][mM])?\\s*(.*)?";
    static Pattern pattern = Pattern.compile(timePatternString);
    
	private static String standardizeTime(String valueString) throws IllegalArgumentException {
        Matcher matcher = pattern.matcher(valueString);
        if ( matcher.matches()) {
            int gnum = 1;
            String hStr = matcher.group(gnum++);
            String mStr = matcher.group(gnum++);
            String sStr = matcher.group(gnum++);
            String msStr = matcher.group(gnum++);
            String xsMs = matcher.group(gnum++);
            String amPm = matcher.group(gnum++);
            String tz = matcher.group(gnum++);
            try {
                int hour = Integer.parseInt(hStr);
                int minute = Integer.parseInt(mStr);
                int sec = ! StringUtils.emptyOrNull(sStr) ? Integer.parseInt(sStr) : 0;
                int ms = ! StringUtils.emptyOrNull(msStr) && msStr.startsWith(".") ?
                            Integer.parseInt(msStr.substring(1)) : 0;
                if ( ! StringUtils.emptyOrNull(amPm)) {
                    hour = adjustByAmPm(hour, amPm.toUpperCase());            
                }
                if ( ! StringUtils.emptyOrNull(tz) && ! isUTC(tz)) {
                    logger.info("Ignoring time zone designation: " + tz);
                }
                if ( ! StringUtils.emptyOrNull(xsMs)) {
                    logger.info("Ignoring extra fractional section digits: " +xsMs);
                }
                Double second = new Double("" + sec + "." + ms);
        		if ( (hour < 0) || (hour >= 24) || 
        			 (minute < 0) || (minute >= 60) || 
        			 second.isNaN() || (second < 0.0) || (second >= 60.0) ) {
        			throw new IllegalArgumentException("invalid time value: " + valueString);
        		}
        		String stdVal = String.format("%02d:%02d:%05.3f", hour, minute, second);
        		return stdVal;
            } catch (Exception ex) {
                logger.info("Unable to parse time string: " + valueString + " : " + ex);
                throw new IllegalArgumentException("Unable to parse time string: " + valueString + " : " + ex);
            }
        } else {
            logger.info("Unable to match time string: " + valueString);
            throw new IllegalArgumentException("Unable to match time string: " + valueString);
        }
	}
	/**
     * @param tz
     * @return
     */
    private static boolean isUTC(String tz) {
        String tzU = tz.toUpperCase();
        boolean isUTC = tzU.equals("Z") || tzU.equals("UTC") || tzU.equals("GMT")
                            || tzU.matches("[+-]?0{1,2}\\:?00");
        if ( isUTC ) {
            logger.debug(tz + " is UTC");
        }
        return isUTC;
    }

    /**
     * @param hour
     * @param amPm
     * @return
     */
    private static int adjustByAmPm(int hour, String amPm) {
        int adjustedHour = hour;
        switch (amPm) {
            case "AM":
                if ( hour == 12 ) {
                    adjustedHour = 0;
                } else if ( hour > 12 ) {
                    throw new IllegalArgumentException("Invalid hour " + hour + " for AM");
                }
                break;
            case "PM":
                if ( hour < 12 ) {
                    adjustedHour += 12;
                } else if ( hour > 12 ) {
                    logger.warn( "Duplicative hour of " + hour + " and PM designation.");
                }
                break;
            default: // should not happen
                logger.warn("Illegal/Irregular AM/PM : " + amPm);
        }
        return adjustedHour;
    }

    private static void tryThisInstead(String timeStr, PrintStream out) {
        Matcher matcher = pattern.matcher(timeStr);
        if ( matcher.matches()) {
            for (int i = 0; i <= matcher.groupCount(); i++ ) {
                out.print(matcher.group(i) + " : ");
            }
        } else {
            out.print(timeStr + " : NOPE" );
        }
        out.println();
	}
    private static final String[] timeTests = new String[] {
            "2:42",
            "0842",
            "1842",
            "184227",
            "2:4:2",
            "02:42",
            "12:42",
            "02:62:38",
            "02:42:78",
            "12:42:38",
            "12:42:38 Z",
            "12:42:38-0000",
            "12:42:38.702 -08:00",
            "12:42:38.702465 AM -08:00",
            "02:42UTC",
            "02:42 UTC",
            "02:42 Z",
//            "02:42pst",
//            "02:42 pst",
            "02:42PST",
            "02:42 PST",
            "02:42-0800",
            "02:42 -0800",
            "12:42 -0800",
            "12:42 EDT",
            "02:42AM",
//            "02:42am",
            "02:42 PM",
            "12:42 PM",
//            "02:42 PM pst",
            "02:42 PM PST",
            "12:42 AM PST",
            "12:42 PM PST",
            "13:42 PM",
            "02:42 AM EDT",
            "12:42 AM EDT",
            "22:42 -800",
            "22:42 PST"
    };
//    private static final String newTimestampFormat = "yyyy[-]MM[-]dd[[ ]['T']HH:mm[:ss][ ][XXX]]";   
    private static final String format1 = "H[:]mm[[:]ss][ ][z]";   
    private static final String format1b = "HH[:]mm[[:]ss][ ][z]";   
    private static final String format2 = "H[:]mm[[:]ss][ ][Z]";   
    private static final String format2b = "HH[:]mm[[:]ss][ ][Z]";   
    private static final String format3 = "H[:]mm[[:]ss][ ][X]";   
    private static final String format3b = "HH[:]mm[[:]ss][ ][X]";   
    private static final String format4 = "h[:]mm[[:]ss][ ][a]";   
    private static final String format5 = "h[:]mm[[:]ss][ ][a][ ][z]";   
    private static final String[] formats = new String[] {
        format1,
        format1b,
        format2,
        format2b,
        format3,
        format3b,
        format4,
        format5
    };
    public static void main(String[] args) {
        boolean fileOutput = false;
        try ( PrintStream out = fileOutput ? new PrintStream(new FileOutputStream("timeParsing.txt")) : System.out) { 
            for ( String s : timeTests ) {
                tryThisInstead(s, out);
                System.out.println(s + " > " + standardizeTime(s));
            }
            if ( true ) System.exit(1);
            DateTimeFormatter[] wtfs  = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern(format1),
                DateTimeFormatter.ofPattern(format1b),
                DateTimeFormatter.ofPattern(format2),
                DateTimeFormatter.ofPattern(format2b),
                DateTimeFormatter.ofPattern(format3),
                DateTimeFormatter.ofPattern(format3b),
                DateTimeFormatter.ofPattern(format4),
                DateTimeFormatter.ofPattern(format5)
            };
            for (String timeStr : timeTests) {
                out.print(timeStr + (timeStr.length() < ( fileOutput ? 4 : 8 ) ? "\t":" "));
                int n = 0;
                for ( DateTimeFormatter wtf : wtfs ) {
                    out.print( "\t" + formats[n++]+"\t" );
                    try {
                        TemporalAccessor time = wtf.parse(timeStr);
                        out.print(time + "\t: ");
                        try {
                            LocalTime lt = LocalTime.parse(timeStr, wtf);
                            out.println(lt);
                        } catch (Exception ex) {
                            out.println("N/A");
                        }
//                        break;
                    } catch (Exception ex) {
                        out.println(ex);
                    }
                    if ( n < formats.length ) out.print("\t");
//                    if ( timeStr.length() >= 7 && n < formats.length) out.print("\t");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private static void tryThis() {
     // parser/formatter for month/day/year
        DateTimeFormatter mdy = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        // parser for both patterns
        DateTimeFormatter parser = new DateTimeFormatterBuilder()
            // optional MM/dd/yyyy
                .optionalStart()
            .appendOptional(mdy)
            .optionalEnd()
            // optional yyyy-MM-dd'T'HH:mm:ss (use built-in formatter)
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            // create formatter
            .toFormatter();

        // parse MM/dd/yyyy
        LocalDate d1 = LocalDate.parse("10/16/2016", parser);
        System.out.println(mdy.format(d1));
        // parse yyyy-MM-dd'T'HH:mm:ss
        LocalDate d2 = LocalDate.parse("2016-10-16T10:20:30", parser);
        System.out.println(mdy.format(d2));
        LocalTime d3 = LocalTime.parse("2016-10-16T10:20:30", parser);
        System.out.println(mdy.format(d3));
        LocalTime d4 = LocalTime.parse("10:20:30", parser);
        System.out.println(mdy.format(d4));

        // use mdy to format

        // format object with time fields: using mdy formatter to avoid multiple pattern problem
        System.out.println(mdy.format(LocalDateTime.now()));
    }
	/**
	 * Standardized a time string to HH:mm:ss.SSS
	 * 
	 * @param valueString
	 * 		time string in the fromUnit format
	 * @return
	 * 		standardized time string
	 * @throws IllegalArgumentException
	 * 		if the fromUnit format is not recognized, or
	 * 		if the value is not a valid time string
	 */
	private String _standardizeTime(String valueString) throws IllegalArgumentException {
		Integer hour;
		Integer minute;
		Double second;
		if ( "hh:mm:ss".equalsIgnoreCase(fromUnit) || "hh:mm".equalsIgnoreCase(fromUnit)) {
			try {
				String[] pieces = TIME_SPLIT_PATTERN.split(valueString,0);
				if ( (pieces.length < 2) || (pieces.length > 3) ) {
					int idx = valueString.indexOf('.');
					if ( (idx == 6) || ((idx < 0) && (valueString.length() >= 6)) ) {
						pieces = new String[3];
						pieces[0] = valueString.substring(0, 2);
						pieces[1] = valueString.substring(2, 4);
						pieces[2] = valueString.substring(4);
					}
					else if ( (idx < 0) && (valueString.length() <= 4) ) {
						pieces = new String[2];
						int hend = valueString.length()-2;
						pieces[0] = hend > 0 ? valueString.substring(0, hend) : "0";
						pieces[1] = hend > 0 ? valueString.substring(hend, valueString.length()) : "0";
					} else
						throw new Exception("Invalid Time String: " + valueString + " - Does not match \"" + fromUnit + "\"" ) ; // \"hh:mm:ss\"");
				}
				hour = Integer.valueOf(pieces[0]);
				minute = Integer.valueOf(pieces[1]);
				if ( pieces.length == 3 ) {
					second = Double.valueOf(pieces[2]);
				} else {
					second = 0.0;
				}
			} catch ( Exception ex ) {
				hour = -1;
				minute = -1;
				second = -1.0;
			}
		} else {
			throw new IllegalArgumentException("conversion from \"" + fromUnit + "\" is not supported");
		}
		if ( (hour == null) || (hour < 0) || (hour >= 24) || 
			 (minute == null) || (minute < 0) || (minute >= 60) || 
			 (second == null) || second.isNaN() || (second < 0.0) || (second >= 60.0) ) {
			throw new IllegalArgumentException("invalid time value");
		}
		String stdVal = String.format("%02d:%02d:%05.3f", hour, minute, second);
		return stdVal;
	}

}
