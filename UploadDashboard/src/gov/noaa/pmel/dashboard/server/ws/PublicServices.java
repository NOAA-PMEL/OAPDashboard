/**
 * 
 */
package gov.noaa.pmel.dashboard.server.ws;

import java.io.IOException;
import java.util.Map;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static javax.servlet.http.HttpServletResponse.*;

import gov.noaa.pmel.dashboard.server.CommonServiceBase;
import gov.noaa.pmel.dashboard.server.DashboardException;
import gov.noaa.pmel.dashboard.server.Users;
import gov.noaa.pmel.dashboard.server.util.Notifications;


/**
 * @author kamb
 *
 */
@MultipartConfig
@SuppressWarnings("serial")
public class PublicServices extends CommonServiceBase {

    private static final String SERVICES_PATH = "tx";
    
    private static Logger logger = LogManager.getLogger(PublicServices.class);
    
    /**
     * 
     */
    public PublicServices() {
    }

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.warn("PublicServices root");
        response.sendError(SC_FORBIDDEN);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        logger.debug(httpRequest.getRequestURI());
        try {
            String path = httpRequest.getPathInfo();
            String requestedOp = path.substring(path.lastIndexOf('/')+1);
            String context = httpRequest.getRequestURI();
            context = context.substring(0, context.indexOf(SERVICES_PATH));
            Map<String, String[]> params = getFormParams(httpRequest);
            switch (requestedOp) {
                case "reset_password":
                    String[] user_email = params.get("user_or_email");
                    if ( user_email == null || user_email.length == 0 || user_email[0].trim().isEmpty() ) {
                        throw new DashboardException("No username or email provided.");
                    }
                    Users.resetPassword(user_email[0]);
                    String newPasswordPage = context + "passwordreset.html";
                    httpResponse.sendRedirect(newPasswordPage);
                    break;
                case "send_username":
                    user_email = params.get("user_or_email");
                    if ( user_email == null || user_email.length == 0 || user_email[0].trim().isEmpty() ) {
                        throw new DashboardException("No username or email provided.");
                    }
                    Users.sendUsername(user_email[0]);
                    String usernamePage = context + "username.html";
                    httpResponse.sendRedirect(usernamePage);
                    break;
                case "request_account":
                    Users.requestAccount(params);
                    String accountRequestPage = context + "accountrequested.html";
                    httpResponse.sendRedirect(accountRequestPage);
                    break;
                case "no_action":
                    respondFailure(httpResponse, SC_BAD_REQUEST, "Please select an action.");
                    break;
                default:
                    respondFailure(httpResponse, SC_BAD_REQUEST, "Unknown request.");
            }
        } catch (DashboardException ex) {
            respondFailure(httpResponse, SC_BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            logger.warn(ex,  ex);
            Notifications.Alert("SDIS PasswordServices Exception", ex);
            respondFailure(httpResponse, SC_INTERNAL_SERVER_ERROR, 
                           "There was an error handling your request. "
                           + "Please contact the system administrator.");
        }
	}

    /**
     * @param response
     * @throws IOException 
     */
    private static void respondSuccess(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(200);
        response.getOutputStream().write(msg.getBytes());
    }
        
    private static void respondFailure(HttpServletResponse response, int code, String msg) {
        sendErrResponse(response, code, msg);
    }

    private static void respondFailure(HttpServletResponse response, int code) throws IOException {
        response.sendError(code);
    }
}
