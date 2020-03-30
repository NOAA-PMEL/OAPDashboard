/**
 * 
 */
package gov.noaa.pmel.dashboard.server.ws;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static javax.servlet.http.HttpServletResponse.*;

import gov.noaa.pmel.dashboard.server.Users;


/**
 * @author kamb
 *
 */
@SuppressWarnings("serial")
public class PublicServices extends HttpServlet {

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
//        response.getOutputStream().write("Good to go".getBytes());
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        System.out.println(httpRequest);
        System.out.println(httpRequest.getParameterMap());
        try {
            String path = httpRequest.getPathInfo();
            String requestedOp = path.substring(path.lastIndexOf('/')+1);
            String context = httpRequest.getRequestURI();
            context = context.substring(0, context.indexOf(SERVICES_PATH));
            switch (requestedOp) {
                case "reset_password":
                    Users.resetPassword(httpRequest.getParameter("user_or_email"));
                    String newPasswordPage = context + "passwordreset.html";
                    httpResponse.sendRedirect(newPasswordPage);
                    break;
                case "request_account":
                    Users.requestAccount(httpRequest.getParameterMap());
                    String accountRequestPage = context + "accountrequested.html";
                    httpResponse.sendRedirect(accountRequestPage);
                    break;
                default:
                    respondFailure(httpResponse, SC_BAD_REQUEST);
            }
        } catch (Exception ex) {
            logger.warn(ex,  ex);
            respondFailure(httpResponse, "There was an error handling your request. Please contact the system administrator.");
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
        
    /**
     * @param response
     * @param ex
     * @throws IOException 
     */
    private static void respondFailure(HttpServletResponse response, String msg) throws IOException {
        response.getOutputStream().write(msg.getBytes());
    }

    private static void respondFailure(HttpServletResponse response, int code) throws IOException {
        response.sendError(code);
    }
}
