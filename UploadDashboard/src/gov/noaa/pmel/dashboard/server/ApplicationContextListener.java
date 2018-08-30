/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import gov.noaa.pmel.tws.util.ApplicationConfiguration;

/**
 * @author kamb
 *
 */
public class ApplicationContextListener implements ServletContextListener {

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        System.out.println("OA Context destroyed.");
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        System.out.println("OA Context initialized.");
        try {
            File contentDir = DashboardConfigStore.getAppContentDir();
            File configDir = new File(contentDir, "config");
            String module = "oap";
            ApplicationConfiguration.Initialize(configDir, module);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
