/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.myb;

import java.io.Reader;
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

    private static SqlSessionFactory dashboardSessionFactory;
    private static SqlSessionFactory flagsDbSessionFactory;
    
    private static String DEFAULT_CONFIG_FILE = "config/mybatis-config.xml";
 
    private static final String My_dashboardDb = "my-dashboard";
    private static final String My_flagsDb = "my-Flags";
    private static final String pg_dashboardDb = "oapdashboard";
    private static final String pg_flagsDb = "OAPFlags";
    
    private static final String dashboardEnv = My_dashboardDb;
    private static final String flagsEnv = My_flagsDb;
    static{
        String dbConfigFileName = ApplicationConfiguration.getProperty("tview.db.config.file", DEFAULT_CONFIG_FILE);
        try ( Reader r1 = Resources.getResourceAsReader(dbConfigFileName);
        	  Reader r2 = Resources.getResourceAsReader(dbConfigFileName); ) {
        	SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
            dashboardSessionFactory = builder.build(r1, dashboardEnv );
            flagsDbSessionFactory = builder.build(r2, flagsEnv );
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
 
    public static SqlSessionFactory getFlagsDbSessionFactory() {
    	return flagsDbSessionFactory;
    }
    public static SqlSessionFactory getDashboardDbSessionFactory(){
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
			ssf = getFlagsDbSessionFactory();
			System.out.println("ssf:"+ssf);
            try ( SqlSession sesh = ssf.openSession()) {
                System.out.println("sesh:"+sesh);
            }
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
