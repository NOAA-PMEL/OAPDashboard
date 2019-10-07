/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.SessionServicesInterface;

/**
 * @author kamb
 *
 */
public class SessionServices extends RemoteServiceServlet implements SessionServicesInterface {

    private static final long serialVersionUID = -215319085569201181L;

    static {
        try {
            DashboardConfigStore store = DashboardConfigStore.get(true);
            logger = LogManager.getLogger(DashboardServices.class);
        } catch (IOException iex) {
            iex.printStackTrace();
            System.exit(-42);
        }
    }
    
    // To get config debug set: -Dorg.apache.logging.log4j.simplelog.StatusLogger.level=TRACE
    private static Logger logger; //  = LogManager.getLogger(DashboardServices.class);
    
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.shared.SessionServices#ping(java.lang.String)
     */
	@Override
	public void ping(String sessionId) {
		HttpServletRequest request = getThreadLocalRequest();
		String username = null;
		try {
			username = DashboardUtils.cleanUsername(request.getUserPrincipal().getName().trim());
		} catch (Exception ex) {
            logger.info(ex);
		}
		logger.debug("ping from user: " + username);
	}

}
