/**
 * 
 */
package gov.noaa.pmel.dashboard.util;


/**
 * @author kamb
 *
 */
public class DbUtils {

	public static enum SortOrder {
		ASC,
		DESC
	}
	
	public static void close(AutoCloseable ... closers) {
		for (AutoCloseable c : closers) {
			try { if ( c != null ) { c.close(); }} 
			catch (Exception e) { e.printStackTrace(); }
		}
	}

}
