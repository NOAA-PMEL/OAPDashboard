/**
 * 
 */
package gov.noaa.pmel.dashboard.server.ws;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gov.noaa.pmel.dashboard.server.Users;


/**
 * @author kamb
 *
 */
@SuppressWarnings("serial")
public class PwResetService extends HttpServlet {

    /**
     * 
     */
    public PwResetService() {
    }

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println(request);
        System.out.println(request.getParameterMap());
        try {
            Users.resetPassword(request.getParameter("user_or_email"));
            respondSuccess(response);
        } catch (Exception ex) {
            respondFailure(response, ex);
        }
	}

    /**
     * @param response
     * @throws IOException 
     */
    private void respondSuccess(HttpServletResponse response) throws IOException {
        response.getOutputStream().write("A new password will be emailed to the email address on file.".getBytes());
    }
        
    /**
     * @param response
     * @param ex
     * @throws IOException 
     */
    private void respondFailure(HttpServletResponse response, Exception ex) throws IOException {
        response.getOutputStream().write(("We were unable to reset your password: " + ex.getMessage()).getBytes());
    }

}
