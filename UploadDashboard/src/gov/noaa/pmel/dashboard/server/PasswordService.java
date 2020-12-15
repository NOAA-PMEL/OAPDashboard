/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.*;

import org.apache.log4j.Logger;

import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.server.ws.ResourceBase;
import gov.noaa.pmel.dashboard.util.PasswordUtils;
import gov.noaa.pmel.tws.util.Logging;

/**
 * @author kamb
 *
 */
@MultipartConfig
public class PasswordService extends CommonServiceBase {

	static final Logger logger = Logging.getLogger(NotificationService.class);
    
	private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement("/var/tmp/oap");
	
    /** generated */
    private static final long serialVersionUID = 2582946584035000552L;

    private static final String BASE_PAGE = "OAPUploadDashboard.html";
    
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.info("Post to: " + request.getRequestURL().toString());
        try {
            response.getOutputStream().write(request.toString().getBytes());
//            response.sendError(response.SC_FORBIDDEN);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.info("Post to: " + request.getRequestURL().toString());
        try {
            String path = request.getPathInfo(); 
            Cookie[] cookies = request.getCookies();
            Cookie sdis = null;
            for ( Cookie cookie : cookies ) {
                if (cookie.getName().equals("sdisuid")) {
                    sdis = cookie;
                }
            }
            if ( sdis == null ) {
                logger.warn("No SDIS cookie in set password request!");
                logger.warn(ResourceBase.fullDump(request));
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            Map<String, String[]> params = getFormParams(request);
            String username = getUsername(request);
            String p0 = params.get("pw0")[0];
            User user = Users.validateUser(username, p0);
            String pw1 = params.get("pw1")[0];
            String pw2 = params.get("pw2")[0];
            if ( !pw1.equals(pw2)) {
                sendMsgResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Passwords do not match.");
            } else if ( p0.equals(pw1)) {
                sendMsgResponse(response, HttpServletResponse.SC_BAD_REQUEST, "New password cannot be the same as the previous.");
            } else {
                PasswordUtils.validatePasswordStrength(pw1);
                Users._setUserPassword(user, pw1); // This re-does the pw check...
                String uri = request.getRequestURI();
                String location = uri.substring(0, uri.indexOf("pw")) + BASE_PAGE;
                sendPasswordChangedRedirect(response, location);
            }
        } catch (CredentialException cex) { // bad password complexity
            sendErrResponse(response, SC_BAD_REQUEST, "New password does not meet complexity requirements." );
        } catch (DashboardException dex) {
            request.getSession().invalidate();
            sendErrResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "There was an error changing your password.<br/>"
                            + "Please try again later.");
        } catch (LoginException lex) {
            sendErrResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Incorrect password.");
        } catch (IllegalStateException iex) { // username not found
            request.getSession().invalidate();
            logger.warn(iex,iex);
            sendErrResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                            "There was an error changing your password.<br/>"
                            + "Please try again later.");
        } catch (Throwable t) {
            response.sendError(SC_INTERNAL_SERVER_ERROR, SERVER_ERROR );
        }
    }
    
    /**
     * @param response
     * @throws IOException 
     */
    private void sendPasswordChangedRedirect(HttpServletResponse response, String location) throws Exception {
        String successMsg = buildSuccessMessage(response, location);
        sendJsonResponse(response, SC_OK, successMsg);
    }
    /**
     * @param response
     * @param location2 
     * @return
     */
    private static String buildSuccessMessage(HttpServletResponse response, String location) {
        StringBuilder msg = new StringBuilder("{")
            .append("\"").append("message").append("\":").append("\"Your password has been changed.\"").append(",")
            .append("\"").append("location").append("\":\"").append(location).append("\"}");
        return msg.toString();
    }
}
