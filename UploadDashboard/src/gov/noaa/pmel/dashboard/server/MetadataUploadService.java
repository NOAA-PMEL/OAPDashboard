/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

/**
 * Service to receive the uploaded metadata file from the client
 * 
 * @author Karl Smith
 */
public class MetadataUploadService extends HttpServlet {

	private static final long serialVersionUID = -1458504704372812166L;

    private static final Logger logger = LogManager.getLogger(MetadataUploadService.class);
    
	private ServletFileUpload metadataUpload;

	public MetadataUploadService() {
		File servletTmpDir;
		try {
			// Get the temporary directory used by the servlet
			servletTmpDir = (File) getServletContext().getAttribute(
					"javax.servlet.context.tempdir");
		} catch (Exception ex) {
			// Just use the default system temp dir (less secure)
			servletTmpDir = null;
		}
		// Create a disk file item factory for processing requests
		DiskFileItemFactory factory = new DiskFileItemFactory();
		if ( servletTmpDir != null ) {
			// Use the temporary directory for the servlet for large files
			factory.setRepository(servletTmpDir);
		}
		// Create the file uploader using this factory
		metadataUpload = new ServletFileUpload(factory);
	}

	// XXX TODO: OME_FILENAME check
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// Verify the post has the correct encoding
		if ( ! ServletFileUpload.isMultipartContent(request) ) {
			sendErrMsg(response, "Invalid request contents format for this service.");
			return;
		}

		// Get the contents from the post request
		String username = null;
		try {
			username = DashboardUtils.cleanUsername(request.getUserPrincipal().getName().trim());
		} catch (Exception ex) {
			; // leave username null for error message later
		}
		// Get the contents from the post request
		String datasetIds = null;
		String uploadTimestamp = null;
		String isSupToken = null;
		FileItem metadataItem = null;
        File uploadFile = null;
		try {
			Map<String,List<FileItem>> paramMap = metadataUpload.parseParameterMap(request);
			try {
				List<FileItem> itemList;

				itemList = paramMap.get("datasetids");
				if ( (itemList != null) && (itemList.size() == 1) ) {
					datasetIds = itemList.get(0).getString();
				}
	
				itemList = paramMap.get("timestamp");
				if ( (itemList != null) && (itemList.size() == 1) ) {
					uploadTimestamp = itemList.get(0).getString();
				}
	
				itemList = paramMap.get("supplemental");
				if ( (itemList != null) && (itemList.size() == 1) ) {
					isSupToken = itemList.get(0).getString();
				}
				
				itemList = paramMap.get("metadataupload");
				if ( (itemList != null) && (itemList.size() == 1) ) {
					metadataItem = itemList.get(0);
				}
				
			} finally {
				// Delete everything except for the uploaded metadata file
				for ( List<FileItem> itemList : paramMap.values() ) {
					for ( FileItem item : itemList ) {
						if ( ! item.equals(metadataItem) ) {
							item.delete();
						}
					}
				}
			}
			// Verify page contents seem okay
			DashboardConfigStore configStore = DashboardConfigStore.get(true);
			if ( (username == null) || (datasetIds == null) || (uploadTimestamp == null) ||
				 (metadataItem == null) || ! Users.validateUser(username) ) {
				if ( metadataItem != null )
					metadataItem.delete();
				sendErrMsg(response, "Invalid request contents for this service.");
				return;
			}
			boolean isSupplemental = isSupToken != null ? Boolean.parseBoolean(isSupToken) : false;
			// TODO: validate parameters
			// save file
			ArrayList<String> idList = DashboardUtils.decodeStringArrayList(datasetIds);
			if ( idList.size() > 1 && ! isSupplemental ) {
				throw new IllegalArgumentException("Dataset metadata document can only be uploaded for one dataset.");
			}
			String version = configStore.getUploadVersion();
			String uploadFilename = DashboardUtils.baseName(metadataItem.getName());
            uploadFile = getUploadedFile(metadataItem);
			for (String datasetId : idList) {
                try ( InputStream is = new FileInputStream(uploadFile);) {
    				String filename = isSupplemental ? uploadFilename : MetadataFileHandler.metadataFilename(datasetId); 
    				MetadataFileHandler metadataHandler = configStore.getMetadataFileHandler();
    				// TODO: backup existing ?
                    
                    DashboardMetadata metadata = 
                                metadataHandler.saveMetadataFileItem(datasetId, username, uploadTimestamp, 
    			                                                     filename, version, is);
    				// TODO: validate metadata XML
    				// TODO: generate PDF
    				
    				if ( isSupplemental ) {
    					DataFileHandler cruiseHandler = configStore.getDataFileHandler();
    					cruiseHandler.addAddlDocTitleToDataset(datasetId, metadata);
    				} else {
    					DataFileHandler df = configStore.getDataFileHandler();
    					DashboardDataset dataset = df.getDatasetFromInfoFile(datasetId);
    					dataset.setMdTimestamp(uploadTimestamp);
    					df.saveDatasetInfoToFile(dataset, "Updating metadata timestamp on user upload metadata file.");
    				}
    			}
            }
            if ( uploadFile != null ) {
                uploadFile.delete();
                uploadFile = null;
            }
			
			// Send the success response
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/html;charset=UTF-8");
			try ( PrintWriter respWriter = response.getWriter(); ) {
    			respWriter.println(DashboardUtils.SUCCESS_HEADER_TAG);
    			response.flushBuffer();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			sendErrMsg(response, "Error processing the request\n" + ex.getMessage());
			return;
		} finally {
            if ( uploadFile != null ) {
                uploadFile.delete();
            }
		}
	}
	
	/**
     * @param metadataItem
     * @return
     */
    private static File getUploadedFile(FileItem metadataItem) throws Exception {
        if ( metadataItem instanceof DiskFileItem &&
             ! metadataItem.isInMemory() ) {
            return ((DiskFileItem)metadataItem).getStoreLocation();
        }
        File tmpFile = File.createTempFile("oadb_upload_"+metadataItem.getName(), ".tmp");
        try ( OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
            os.write(metadataItem.get());
            metadataItem.delete();
        }
        return tmpFile;
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
