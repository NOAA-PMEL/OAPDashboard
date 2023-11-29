/**
 * 
 */
package gov.noaa.pmel.dashboard.test.datatype;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import gov.noaa.pmel.dashboard.server.util.DateUtil;
import gov.noaa.pmel.dashboard.server.util.DateUtil.FormatHint;

/**
 * @author kamb
 *
 */
public class TimeFormatTester {

    static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    
    static Date tryDateUtils(String dateString) {
        Date date = null;
        try {
            System.out.print("DateUtil: " + dateString + " : ");
            date = DateUtil.parse(dateString);
            System.out.print(sdf.format(date));
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return date;
    }
        
    static Date tryDateUtilsWithHint(String dateString, DateUtil.FormatHint hint) {
        Date date = null;
        try {
            System.out.print("DateUtilWithHint ("+hint+"): " + dateString + " : ");
            date = DateUtil.parseWithHint(dateString, hint);
            System.out.print(sdf.format(date));
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return date;
    }
        
    static void runAllTests(String dateString, Date correctDate, FormatHint hint) {
        Date parsedDate;
        System.out.println("--------- " + dateString);
        parsedDate = tryDateUtils(dateString);
        check(parsedDate, correctDate);
        parsedDate = tryDateUtilsWithHint(dateString, hint);
        check(parsedDate, correctDate);
    }
        
    /**
     * @param parsedDate
     * @param correctDate
     */
    private static void check(Date parsedDate, Date correctDate) {
        if ( parsedDate != null ) {
            if ( parsedDate.equals(correctDate)) {
                System.out.println("\t-- correct");
            } else {
                System.out.println("\t-- WRONG");
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
//        Calendar cal;
        Date sixtyEight, ninetyEight, testDate;
        try {
        sixtyEight = sdf.parse("1968-08-04");
        ninetyEight = sdf.parse("1998-08-04");
        testDate = sdf.parse("2017-08-04");
//        cal = Calendar.getInstance(TimeZone.getDefault());
//        cal.setTimeInMillis(0);
//        cal.set(Calendar.HOUR, 0);
//        cal.set(Calendar.MONTH, Calendar.JULY);
//        cal.set(Calendar.DAY_OF_MONTH, 4);
//        
//        cal.set(Calendar.YEAR, 1968);
//        Date sixtyEight = cal.getTime();
        String mmddyy_1960s1 = "080468";
        
//        cal.set(Calendar.YEAR, 1998);
//        Date ninetyEight = cal.getTime();
        String mmddyy_90s1 = "080498";
        String mmddyyyy_90s1 = "08041998";
        String mm_dd_yy_90s1 = "08-04-98";
        String mm_dd_yy_90s2 = "08/04/98";
        String mm_dd_yyyy_90s1 = "08-04-1998";
        String mm_dd_yyyy_90s2 = "08/04/1998";
        
//        cal.set(Calendar.YEAR, 2017);
//        Date testDate = cal.getTime();
        String mon_d_yyyy_1 = "Aug 08 2017";
        String mon_d_yyyy_2 = "Aug 08, 2017";
        String month_d_yyyy_1 = "August 08 2017";
        String month_d_yyyy_2 = "August 08, 2017";
        
        String ddmmyy = "040817";
        String ddmmyyyy = "04082017";
        String dd_mm_yy_1 = "04-08-17";
        String dd_mm_yy_2 = "04/08/17";
        String dd_mm_yyyy_1 = "04-08-2017";
        String dd_mm_yyyy_2 = "04/08/2017";
        
        String d_mon_yyyy_1 = "04 Aug 2017";
        String d_mon_yyyy_2 = "04 Aug, 2017";
        String d_month_yyyy_1 = "04 August 2017";
        String d_month_yyyy_2 = "04 August, 2017";
        
        String yyyymmdd = "20170804";
        String yyyy_mm_dd_1 = "2017-08-04";
        String yyyy_mm_dd_2 = "2017/08/04";
        String yyyy_mm_dd_3 = "2017 08 04";
        
        String[] dates = new String[] {
            "MONTH_DAY_YEAR",
            mmddyy_90s1,
            mmddyyyy_90s1,
            mm_dd_yy_90s1,
            mm_dd_yy_90s2,
            mm_dd_yyyy_90s1,
            mm_dd_yyyy_90s2,
        
            "2017",
            mon_d_yyyy_1,
            mon_d_yyyy_2,
            month_d_yyyy_1,
            month_d_yyyy_2,
        
            "DAY_MONTH_YEAR",
            ddmmyy,
            ddmmyyyy,
            dd_mm_yy_1,
            dd_mm_yy_2,
            dd_mm_yyyy_1,
            dd_mm_yyyy_2,
        
            d_mon_yyyy_1,
            d_mon_yyyy_2,
            d_month_yyyy_1,
            d_month_yyyy_2,
            
            "YEAR_MONTH_DAY",
            yyyymmdd,
            yyyy_mm_dd_1,
            yyyy_mm_dd_2,
            yyyy_mm_dd_3
        };
            
        FormatHint hint = FormatHint.MONTH_DAY_YEAR;
        Date checkDate = sixtyEight;
        runAllTests(mmddyy_1960s1, checkDate, hint);
        checkDate = ninetyEight;
        for (String dateString : dates) {
            if ( dateString.contains("MONTH")) {
                hint = FormatHint.valueOf(dateString);
                continue;
            } else if ( "2017".equals(dateString)) {
                checkDate = testDate;
                continue;
            }
            runAllTests(dateString, checkDate, hint);
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    }
}
