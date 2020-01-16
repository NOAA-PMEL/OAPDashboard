/**
 * 
 */
package gov.noaa.pmel.dashboard.test.util;

import java.lang.reflect.Method;

/**
 * @author kamb
 *
 */
public class ClassRunner {

    /**
     * 
     */
    public ClassRunner() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String[] dargs = new String[]  { "-ds", "PRISM082008", "-v", "1", "-s", "pending", "-m", "we're looking for clues" };
        
        String className = "gov.noaa.pmel.dashboard.server.util.StatusUpdater";
        
        try {
            Class<?> theclass = Class.forName(className);
            Method main = theclass.getMethod("main", String[].class);
            main.invoke(null, new Object[] { dargs });
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
