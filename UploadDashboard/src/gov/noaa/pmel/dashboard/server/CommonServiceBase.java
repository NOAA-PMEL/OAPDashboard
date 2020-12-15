/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;

/**
 * @author kamb
 *
 */
public abstract class CommonServiceBase extends HttpServlet {

    /** Generated */
    private static final long serialVersionUID = 2594830822093131075L;
    
    protected static final String SERVER_ERROR = "There was an error handling your request. "
                                               + "Please contact the system administrator.";

    protected static String getUsername(HttpServletRequest request) {
        try {
            String username = request.getUserPrincipal().getName();
            return username;
        } catch (Exception ex) {
            throw new IllegalStateException("No user found.");
        }
    }

    /**
     * 
     */
    public CommonServiceBase() {
        super();
    }

    protected Map<String, String[]> getFormParams(HttpServletRequest request) throws IOException, ServletException {
        Map<String, String[]> params;
        String contentType = request.getHeader("Content-Type");
        if ( ! contentType.startsWith("multipart/form-data")) {
            params = request.getParameterMap();
        } else {
            params = new HashMap<String, String[]>();
            // For Content-Type: multipart/form-data
            Collection<Part> parts = request.getParts();
            for (Part part : parts) {
                String pname = part.getName();
                String pcontent = IOUtils.toString(part.getInputStream(), StandardCharsets.UTF_8);
                params.put(pname, new String[] { pcontent });
            }
        }
        return params;
    }

    protected static void sendErrResponse(HttpServletResponse response, int responseStatus, String msgText) {
        response.setStatus(responseStatus);
        String jsonMsg = "{\"message\":\""+msgText+"\"}";
        try {
            response.getOutputStream().write(jsonMsg.getBytes("UTF8"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    protected void sendMsgResponse(HttpServletResponse response, int responseStatus, String msgText) throws Exception {
        String jsonMsg = "{\"message\":\""+msgText+"\"}";
        sendJsonResponse(response, responseStatus, jsonMsg);
    }

    protected void sendJsonResponse(HttpServletResponse response, int responseStatus, String json) throws Exception {
        response.setStatus(responseStatus);
        response.getOutputStream().write(json.getBytes("UTF8"));
    }

}