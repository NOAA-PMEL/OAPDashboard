/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.dao;

import java.sql.Driver;
import java.sql.DriverManager;

/**
 * @author kamb
 *
 */
public abstract class DaoBase {

    static {
        try {
            String driverClassName = "";
            DriverManager.registerDriver((Driver)Class.forName(driverClassName).newInstance());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
