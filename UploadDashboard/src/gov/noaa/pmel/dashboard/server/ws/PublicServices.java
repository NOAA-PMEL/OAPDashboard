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

    static Logger logger = LogManager.getLogger(PublicServices.class);
    
    /**
     * 
     */
    public PublicServices() {
    }

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        response.sendError(SC_FORBIDDEN);
        System.out.println("PublicServices root");
        response.getOutputStream().write("Good to go".getBytes());
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        System.out.println(httpRequest);
        System.out.println(httpRequest.getParameterMap());
        try {
            String path = httpRequest.getPathInfo();
            String requestedOp = path.substring(path.lastIndexOf('/')+1);
            switch (requestedOp) {
                case "reset_password":
                    Users.resetPassword(httpRequest.getParameter("user_or_email"));
                    respondSuccess(httpResponse, "A new password will be emailed to the email address on file.");
                    break;
                case "request_account":
                    Users.requestAccount(httpRequest.getParameterMap());
                    respondSuccess(httpResponse, "Your request has been entered, and you will hear from the System Administrators shortly.");
                    break;
                default:
                    respondFailure(httpResponse, SC_BAD_REQUEST);
            }
        } catch (Exception ex) {
            respondFailure(httpResponse, ex);
        }
	}

    /**
     * @param response
     * @throws IOException 
     */
    private static void respondSuccess(HttpServletResponse response, String msg) throws IOException {
        response.getOutputStream().write(msg.getBytes());
    }
        
    /**
     * @param response
     * @param ex
     * @throws IOException 
     */
    private static void respondFailure(HttpServletResponse response, Exception ex) throws IOException {
        response.getOutputStream().write(("We were unable to reset your password: " + ex.getMessage()).getBytes());
    }

    private static void respondFailure(HttpServletResponse response, int code) throws IOException {
        response.sendError(code);
    }
}
