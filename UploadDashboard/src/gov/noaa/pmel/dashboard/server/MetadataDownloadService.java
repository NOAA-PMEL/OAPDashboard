/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gov.noaa.pmel.dashboard.oads.OADSMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

/**
 * Service to receive the uploaded metadata file from the client
 * 
 * @author Karl Smith
 */
public class MetadataDownloadService extends HttpServlet {

	private static final long serialVersionUID = -1458504704372812166L;

	public MetadataDownloadService() {
	}

	private static int ID_POS = 1;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		// Get the contents from the post request
		String username = null;
		try {
			username = DashboardUtils.cleanUsername(request.getUserPrincipal().getName().trim());
		} catch (Exception ex) {
			; // leave username null for error message later
		}

		String requestPath = request.getPathInfo();
		String[] pathParts = requestPath.split("/");
		
		// Get the contents from the post request
		String datasetId = pathParts[ID_POS];
		
		try {
			File metadataFile = OADSMetadata.getMetadataFile(datasetId);
			
			if ( !metadataFile.exists()) {
				metadataFile = OADSMetadata.getExtractedMetadataFile(datasetId);
			}
			if ( !metadataFile.exists()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "OADS metadata not found for dataset : " + datasetId);
			} else {
				sendFile(metadataFile, response);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error transferring file: " + ex.getMessage());
			} catch (IOException e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	private static int BUFFER_SIZE = 16384;
	
	private void sendFile(File file, HttpServletResponse response) throws IOException {
		try ( OutputStream out = response.getOutputStream(); 
			  FileInputStream in = new FileInputStream(file); ) {
			String fname = file.getName();
			byte[] tbuf = new byte[BUFFER_SIZE];
			String mimeType = getServletContext().getMimeType(fname);
			if ( mimeType == null ) {
				mimeType = "applcation/octet-stream";
			}
			response.setContentType(mimeType);
			response.setContentLength((int)file.length());
			response.setHeader("Content-Disposition", "attachment; filename=\""+file.getName()+"\"");
			int read = 0;
			int loop = 0;
			while ((read = in.read(tbuf)) != -1) {
				out.write(tbuf, 0, read);
				if ( ++loop % 10 == 0 ) {
					out.flush();
				}
			}
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	/**
	 * Returns an error message in the given Response object.  
	 * The response number is still 200 (SC_OK) so the message 
	 * goes through cleanly.
	 * 
	 * @param response
	 * 		write the error message here
	 * @param errMsg
	 * 		error message to return
	 * @throws IOException 
	 * 		if writing to the response object throws one
	 */
	private void sendErrMsg(HttpServletResponse response, String errMsg) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter respWriter = response.getWriter();
		respWriter.println(errMsg);
		response.flushBuffer();
	}

}
