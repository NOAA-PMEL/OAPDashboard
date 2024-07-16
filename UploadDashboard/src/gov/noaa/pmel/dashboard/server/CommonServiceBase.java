/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.oads.util.StringUtils;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;

/**
 * @author kamb
 *
 */
public abstract class CommonServiceBase extends HttpServlet {

    /** Generated */
    private static final long serialVersionUID = 2594830822093131075L;
    
    protected static final String SERVER_ERROR = "There was an error handling your request. "
                                               + "Please contact the system administrator.";

    private static final Logger logger = LogManager.getLogger(CommonServiceBase.class);
    
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
    
    protected String getQuarantineLocation(String user) {
		Calendar c = Calendar.getInstance();
		String quarantine = ApplicationConfiguration.getProperty("oap.upload.quarantine", "");
		if ( StringUtils.emptyOrNull(quarantine)) { return null; }
		String dateTime = String.valueOf(c.get(Calendar.YEAR)) +
										 c.get(Calendar.MONTH) +
										 c.get(Calendar.DAY_OF_MONTH) + "_" +
										 c.get(Calendar.HOUR) +
										 c.get(Calendar.MINUTE);
		File quarantineDir = new File(quarantine);
		quarantineDir = new File(quarantineDir, user+"/"+dateTime);
		if ( !quarantineDir.exists()) {
			if ( !quarantineDir.mkdirs()) {
				logger.warn("Unable to create non-exising quarantine directory: " + quarantineDir);
				return null;
			}
		} else if ( !quarantineDir.isDirectory() || !quarantineDir.canWrite()) {
			logger.warn("Quarantine dir either unwritable or not a directory: " + quarantine);
			return null;
		}
    	return quarantineDir.getPath();
    }

}