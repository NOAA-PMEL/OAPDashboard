/**
 * 
 */
package gov.noaa.pmel.dashboard.test.util;

import java.lang.reflect.Method;

/**
 * @author kamb
 *
 */
public class ClassTestRunner {

	private static final String DEFAULT_CATALINA_BASE = "/local/tomcat/7";
	private static final String DEFAULT_SERVICE_NAME = "OAPUploadDashboard";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String classUnderTestName = "gov.noaa.pmel.dashboard.datatype.CastSet";
		try {
			if ( System.getProperty("CATALINA_BASE") == null ) {
				System.setProperty("CATALINA_BASE", DEFAULT_CATALINA_BASE);
			}
			if ( System.getProperty("UPLOAD_DASHBOARD_SERVER_NAME") == null ) {
				System.setProperty("UPLOAD_DASHBOARD_SERVER_NAME", DEFAULT_SERVICE_NAME);
			}
			
			System.out.println("Running class:"+classUnderTestName);
			Class<?> theClass = Class.forName(classUnderTestName);
			Method main = theClass.getMethod("main", new Class[]{String[].class});
			main.invoke(null, new Object[]{args});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
