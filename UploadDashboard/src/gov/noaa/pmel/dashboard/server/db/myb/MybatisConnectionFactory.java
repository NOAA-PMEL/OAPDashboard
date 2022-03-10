/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.myb;

import java.io.Reader;
import java.net.URL;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import gov.noaa.pmel.tws.util.ApplicationConfiguration;
	 
/**
 * @author kamb
 *
 */
public class MybatisConnectionFactory {

    /*
    ### Cause: org.apache.ibatis.transaction.TransactionException: Error configuring AutoCommit.  
    Your driver may not support getAutoCommit() or setAutoCommit(). Requested setting: false.  
    Cause: com.mysql.jdbc.exceptions.jdbc4.CommunicationsException: The last packet successfully received from the server was 290,432,177 milliseconds ago.  
    The last packet sent successfully to the server was 290,432,177 milliseconds ago. is longer than the server configured value of 'wait_timeout'. 
    You should consider either expiring and/or testing connection validity before use in your application, 
    increasing the server configured values for client timeouts, 
    or using the Connector/J connection property 'autoReconnect=true' to avoid this problem.
    */

    private static SqlSessionFactory dashboardSessionFactory;
    
    private static String DEFAULT_CONFIG_FILE = "config/mybatis-config.xml";
 
    private static final String DEFAULT_DB_ENVIRONMENT = "oapdashboard";

    public static final String DB_ENV_PROPERTY = "oap.db.environment";
    public static final String DB_CONFIG_FILE_PROPERTY = "oap.db.config.file";
    
    private static Object configureLock = new Object();
    private static boolean configured = false;
    
    public static void initialize(String dbEnvironmet) {
        synchronized (configureLock) {
            if ( !configured) {
                org.apache.ibatis.logging.LogFactory.useLog4J2Logging();
                String dbConfigFileName = ApplicationConfiguration.getProperty(DB_CONFIG_FILE_PROPERTY, DEFAULT_CONFIG_FILE);
                ApplicationConfiguration.console("Using dbConfigFile: "+ dbConfigFileName);
                ApplicationConfiguration.console("Using dashboard environemnt: "+ dbEnvironmet);
                try { 
                    URL url = MybatisConnectionFactory.class.getResource("/"+dbConfigFileName);
                    System.out.println("Db config file found at " + url + " as " + url.getFile());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try ( Reader r1 = Resources.getResourceAsReader(dbConfigFileName); ) {
                	SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
                    dashboardSessionFactory = builder.build(r1, dbEnvironmet );
                    System.out.println(dashboardSessionFactory);
                    configured = true;
                }catch(Exception e) {
                    System.err.println("Critical Database Configuration Error: " + e);
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }
    static void initialize() {
        String dbEnvironmet = ApplicationConfiguration.getProperty(DB_ENV_PROPERTY, DEFAULT_DB_ENVIRONMENT);
        initialize(dbEnvironmet);
    }
 
    public static SqlSessionFactory getDashboardDbSessionFactory(){
        if ( !configured ) { initialize(); }
        return dashboardSessionFactory;
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			SqlSessionFactory ssf = getDashboardDbSessionFactory();
			System.out.println("ssf:"+ssf);
            try ( SqlSession sesh = ssf.openSession()) {
                System.out.println("sesh:"+sesh);
            }
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
