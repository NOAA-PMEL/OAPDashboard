/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.sun.nio.sctp.IllegalReceiveException;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.RawUploadFileHandler;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.upload.FileUploadProcessor;
import gov.noaa.pmel.dashboard.upload.FileUploadProcessorFactory;
import gov.noaa.pmel.dashboard.upload.StandardUploadFields;
import gov.noaa.pmel.dashboard.util.FormUtils;


/**
 * Service to receive the uploaded cruise file from the client
 * 
 * @author Karl Smith
 */
public class DataUploadService extends HttpServlet { 
	private static final long serialVersionUID = 1547524322159252520L;

    private static Logger logger = LogManager.getLogger(DataUploadService.class);
    
	private ServletFileUpload datafileUpload;

    public static class BadRequestException extends Exception {
        private static final long serialVersionUID = 529541444284268094L;
        public BadRequestException() { super(); }
        public BadRequestException(String message) { super(message); }
        public BadRequestException(Throwable cause) { super(cause); }
        public BadRequestException(String message, Throwable cause) { super(message, cause); }
    }
        
	public DataUploadService() {
		
		DiskFileItemFactory factory = new DiskFileItemFactory();
		try {
			File servletTmpDir = (File) getServletContext().getAttribute("javax.servlet.context.tempdir");
			factory.setRepository(servletTmpDir);
		} catch (Exception ex) {
            // factory will use default temp dir
            logger.debug(ex);
		}
		datafileUpload = new ServletFileUpload(factory);
	}

    @Override 
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug(request);
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    
    private static String getUploadField(String fieldName, Map<String,List<FileItem>> paramMap) {
        return FormUtils.getFormField(fieldName, paramMap, false);
    }
    
    private static String getUploadField(String fieldName, Map<String,List<FileItem>> paramMap, boolean allowMultipleValues) {
        return FormUtils.getFormField(fieldName, paramMap, allowMultipleValues);
    }
    
    private static String getRequiredField(String fieldName, Map<String,List<FileItem>> paramMap) throws NoSuchFieldException {
        return FormUtils.getRequiredFormField(fieldName, paramMap, false);
    }
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // log request
        logger.info(request);
	    
        StandardUploadFields stdFields;
		Map<String,List<FileItem>> paramMap = null;
        
        try {
            String username = getUsername(request);
            
    	    // verify message
            checkRequestMime(request);
            
    	    // extract meta info
            paramMap = extractParameterMap(request);
            stdFields = extractStandardFields(paramMap);
            stdFields.username(username);
            
            // XXX should really move this to a different service
            if ( isPreviewRequest(getUploadField("dataaction", paramMap))) {
                sendPreview(stdFields, response);
                return;
            }
            
            
            
            FileUploadProcessor uploadProcessor = getUploadFileProcessor(stdFields);
            uploadProcessor.processUpload();
            List<String>messages = uploadProcessor.getMessages();
            
            sendOkMsg(response, uploadProcessor.getSuccesses());
        } catch (BadRequestException vex) { // baseRequestValidation
            sendErrMsg(response, vex);
        } catch (FileUploadException fex) { // parseParameterMap
            sendErrMsg(response, fex);
        } catch (IllegalStateException iex) { // no username
            sendErrMsg(response, iex);
        } catch (NoSuchFieldException nsf) { // missing field
            sendErrMsg(response, nsf);
        } catch (IllegalArgumentException iex) { // no files
            sendErrMsg(response, iex);
        } catch (Exception ex) {
            sendErrMsg(response, ex);
        } finally {
			for ( Entry<String,List<FileItem>> paramEntry : paramMap.entrySet() ) {
				for ( FileItem item : paramEntry.getValue() ) {
					item.delete();
				}
			}
        }
	}
    
    private static FileUploadProcessor getUploadFileProcessor(StandardUploadFields stdFields) {
        FileUploadProcessorFactory processorFactory = FileUploadProcessorFactory.getFactory(stdFields);
        FileUploadProcessor uploadProcessor = processorFactory.getProcessor();
        return uploadProcessor;
    }

    private Map<String, List<FileItem>> extractParameterMap(HttpServletRequest request) throws FileUploadException {
        Map<String,List<FileItem>> paramMap = datafileUpload.parseParameterMap(request);
        return paramMap;
    }
    private static StandardUploadFields extractStandardFields(Map<String,List<FileItem>> paramMap) 
            throws FileUploadException, NoSuchFieldException {
        StandardUploadFields sup = null;
        try {
            sup = StandardUploadFields.builder()
                    .parameterMap(paramMap)
                    .dataAction(getRequiredField("dataaction", paramMap))
                    .fileDataEncoding(getUploadField("dataencoding", paramMap))
                    .featureType(getFeatureType(paramMap))
                    .dataFiles(extractDataFiles(paramMap))
                    .build();
        } catch (NullPointerException nex) {
            nex.printStackTrace();
            throw new NoSuchFieldException(nex.getMessage());
        }
        return sup;
    }

    private static List<FileItem> extractDataFiles(Map<String, List<FileItem>> paramMap) throws IllegalArgumentException {
        List<FileItem> datafiles = paramMap.get("dataUpload");
        if ( datafiles == null || datafiles.isEmpty()) {
            throw new IllegalArgumentException("No upload files found.");
        }
        return datafiles;
    }

    private static String getUsername(HttpServletRequest request) {
        try {
            String username = request.getUserPrincipal().getName();
            return username;
        } catch (Exception ex) {
            logger.info(ex);
            throw new IllegalStateException("No user found.");
        }
    }

    private static FeatureType getFeatureType(Map<String, List<FileItem>> paramMap) throws NoSuchFieldException {
        FeatureType featureType;
        String featureTypeName = getRequiredField("featureType", paramMap);
        featureType = featureTypeName != null ? FeatureType.valueOf(featureTypeName) : FeatureType.UNSPECIFIED;
        return featureType;
    }

    private static void checkRequestMime(HttpServletRequest request) throws BadRequestException {
		if ( ! ServletFileUpload.isMultipartContent(request) ) {
            throw new BadRequestException("Invalid request contents format for this service.");
		}
    }

//    @Override
    /*
	protected void _doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.info(request);
		// Verify the post has the correct encoding
		if ( ! ServletFileUpload.isMultipartContent(request) ) {
			sendErrMsg(response, "Invalid request contents format for this service.");
			return;
		}
        
		String username = null;
		try {
			username = DashboardUtils.cleanUsername(request.getUserPrincipal().getName().trim());
		} catch (Exception ex) {
			sendErrMsg(response, "Error processing upload request : No User Found.\n" + ex.getMessage());
            return;
		}

		// Get the contents from the post request
		String timestamp = null;
		String dataFormat = null;
		String encoding = null;
		String action = null;
		String datasetIdColName = null;
        String datasetId = null;
        FeatureType featureType;
		List<FileItem> datafiles = new ArrayList<>();
		try {
			Map<String,List<FileItem>> paramMap = datafileUpload.parseParameterMap(request);
			try {
                String featureTypeName = getUploadField("featureType", paramMap);
                featureType = featureTypeName != null ? FeatureType.valueOf(featureTypeName) : FeatureType.UNSPECIFIED;
                
                timestamp = getUploadField("timestamp", paramMap);
                action = getUploadField("dataaction", paramMap);
				encoding = getUploadField("dataencoding", paramMap); 
				dataFormat = getUploadField("dataformat", paramMap);
				datasetIdColName = getUploadField("datasetIdColName", paramMap);
                datasetId = getUploadField("datasetID", paramMap);
                
				datafiles = paramMap.get("dataUpload");

			} finally {
				// Delete everything except for the uploaded data files
				for ( Entry<String,List<FileItem>> paramEntry : paramMap.entrySet() ) {
					if ( ! "dataUpload".equals(paramEntry.getKey()) ) {
						for ( FileItem item : paramEntry.getValue() ) {
							item.delete();
						}
					}
				}
			}
		} catch (Exception ex) {
			// also delete the uploaded data files when an error occurs
			if ( datafiles != null ) {
				for ( FileItem item : datafiles )
					item.delete();
			}
			sendErrMsg(response, "Error processing upload request \n" + ex.getMessage());
			return;
		}

		// Verify contents seem okay
		if ( datafiles == null || datafiles.isEmpty() ) {
			sendErrMsg(response, "No upload files specified");
			return;
		}
		DashboardConfigStore configStore = DashboardConfigStore.get(true);
		if ( (username == null) || (dataFormat == null) || (encoding == null) || 
			 (action == null)   || (timestamp == null)  || ( ! configStore.validateUser(username) ) ||
			 ! ( action.equals(DashboardUtils.PREVIEW_REQUEST_TAG) ||
				 action.equals(DashboardUtils.NEW_DATASETS_REQUEST_TAG) ||
				 action.equals(DashboardUtils.APPEND_DATASETS_REQUEST_TAG) ||
				 action.equals(DashboardUtils.OVERWRITE_DATASETS_REQUEST_TAG) ) ) {
			for ( FileItem item : datafiles ) {
				item.delete();
			}
			sendErrMsg(response, "Invalid request contents for this service.");
			return;
		}

        if ( isPreviewRequest(action)) {
			FileItem firstItem = datafiles.get(0);
			String filename = firstItem.getName();
            try {
//                sendPreview(firstItem, encoding, response);
            } catch (Exception ex) {
    			sendErrMsg(response, "Error processing the uploaded file " + filename + "\n" + ex.getMessage());
            } finally {
        		for ( FileItem item : datafiles ) {
                    item.delete();
        		}
            }
            return;
        }

		DataFileHandler datasetHandler = configStore.getDataFileHandler();
		RawUploadFileHandler rawFileHandler = configStore.getRawUploadFileHandler();

		// List of all messages to be returned to the client
		ArrayList<String> messages = new ArrayList<String>(datafiles.size());

		// Set of IDs for successfully processed datasets
		TreeSet<String> successes = new TreeSet<String>();

		// create the directory to hold uploads now, in case there are multiple files in the upload.
		File rawUploadsDir = rawFileHandler.createUploadTargetDir(username);
		
		for ( FileItem item : datafiles ) {
			// Get the datasets from this file
			TreeMap<String,DashboardDatasetData> datasetsMap = null;
			String filename = item.getName();
            if ( ! featureType.equals(FeatureType.OPAQUE)) {
    			try ( BufferedReader cruiseReader = new BufferedReader( new InputStreamReader(item.getInputStream(), encoding)); ) {
    				datasetsMap = datasetHandler.createDatasetsFromInput(cruiseReader, dataFormat, username, filename, timestamp, datasetIdColName);
    			} catch (Exception ex) {
    				// Mark as a failed file, and go on to the next
    				messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
    				messages.add(ex.getMessage());
    				messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
    				item.delete();
    				continue;
    			}
            }

			try {
				rawFileHandler.writeItem(item, rawUploadsDir);
			} catch (Exception ex) {
				// TODO: log error, notify admin?
				ex.printStackTrace();
			}
			
			// done with the uploaded data file
			item.delete();

			// Process all the datasets created from this file
            if ( ! featureType.equals(FeatureType.OPAQUE)) {
    			for ( DashboardDatasetData datasetData : datasetsMap.values() ) {
    				// Check if the dataset already exists
    				datasetId = datasetData.getDatasetId();
    				boolean datasetExists = datasetHandler.dataFileExists(datasetId);
    				boolean appended = false;
    				if ( datasetExists ) {
    					String owner = "";
    					String status = "";
    					try {
    						// Read the original dataset info to get the current owner and submit status
    						DashboardDataset oldDataset = datasetHandler.getDatasetFromInfoFile(datasetId);
    						owner = oldDataset.getOwner();
    						status = oldDataset.getSubmitStatus();
    					} catch ( Exception ex ) {
    						// Some problem with the properties file
    						;
    					}
    					// If only create new datasets, add error message and skip the dataset
    					if ( DashboardUtils.NEW_DATASETS_REQUEST_TAG.equals(action) ) {
    						messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
    								filename + " ; " + datasetId + " ; " + owner + " ; " + status);
    						continue;
    					}
    					// Make sure this user has permission to modify this dataset
    					try {
    						datasetHandler.verifyOkayToDeleteDataset(datasetId, username);
    					} catch ( Exception ex ) {
    						messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
    								filename + " ; " + datasetId + " ; " + owner + " ; " + status);
    						continue;
    					}
    					if ( DashboardUtils.APPEND_DATASETS_REQUEST_TAG.equals(action) ) {
    						// Get all the data from the existing dataset
    						DashboardDatasetData oldDataset;
    						try {
    							oldDataset = datasetHandler.getDatasetDataFromFiles(datasetId, 0, -1);
    						} catch ( Exception ex ) {
    							messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
    									filename + " ; " + datasetId);
    							messages.add(ex.getMessage());
    							messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
    							continue;
    						}
    						// If append to dataset, at this time insist on the column names being the same
    						if ( ! datasetData.getUserColNames().equals(oldDataset.getUserColNames()) ) {
    							messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
    							messages.add("Data column names for existing dataset " + datasetId);
    							messages.add("    " + oldDataset.getUserColNames().toString());
    							messages.add("do not match those in uploaded file " + filename);
    							messages.add("    " + datasetData.getUserColNames());
    							messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
    							continue;
    						}
    						// Update information on the existing dataset to reflect updated data
    						// leave the original owner and any archive date
    						oldDataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_NOT_CHECKED);
    						oldDataset.setSubmitStatus(DashboardUtils.STATUS_NOT_SUBMITTED);
    						oldDataset.setArchiveStatus(DashboardUtils.ARCHIVE_STATUS_NOT_SUBMITTED);
    						oldDataset.setUploadFilename(filename);
    						oldDataset.setUploadTimestamp(timestamp);
    						oldDataset.setVersion(configStore.getUploadVersion());
    						// Add the add to the dataset
    						int rowNum = oldDataset.getNumDataRows();
    						for ( ArrayList<String> datavals : datasetData.getDataValues() ) {
    							rowNum++;
    							oldDataset.getDataValues().add(datavals);
    							oldDataset.getRowNums().add(rowNum);
    						}
    						oldDataset.setNumDataRows(rowNum);
    						// Replace the reference to the uploaded dataset with this appended dataset
    						datasetData = oldDataset;
    						appended = true;
    					}
    				}
    				// At this point, datasetData is the dataset to save, regardless of new, overwrite, or append
    
    //				// Create the OME XML stub file for this dataset
    //				try {
    //					OmeMetadata omeMData = new OmeMetadata(datasetId);
    //					DashboardOmeMetadata mdata = new DashboardOmeMetadata(omeMData,
    //							timestamp, username, datasetData.getVersion());
    //					String msg = "New OME XML document from data file for " + 
    //							datasetId + " uploaded by " + username;
    //					MetadataFileHandler mdataHandler = configStore.getMetadataFileHandler();
    //					mdataHandler.saveMetadataInfo(mdata, msg, false);
    //					mdataHandler.saveAsOmeXmlDoc(mdata, msg);
    //				} catch (Exception ex) {
    //					// should not happen
    //					messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
    //							filename + " ; " + datasetId);
    //					messages.add(ex.getMessage());
    //					messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
    //					continue;
    //				}
    
    				// Add any existing documents for this cruise
    				ArrayList<DashboardMetadata> mdataList = 
    						configStore.getMetadataFileHandler().getMetadataFiles(datasetId);
    				TreeSet<String> addlDocs = new TreeSet<String>();
    				for ( DashboardMetadata mdata : mdataList ) {
    					if ( DashboardUtils.autoExtractedMdFilename(datasetId).equals(mdata.getFilename())) {
    						// Ignore the auto-extracted XML stub file
    					}
    					else if ( DashboardUtils.metadataFilename(datasetId).equals(mdata.getFilename())) {
    						datasetData.setMdTimestamp(mdata.getUploadTimestamp());					
    					}
    					else {
    						addlDocs.add(mdata.getAddlDocsTitle());
    					}
    				}
    				datasetData.setAddlDocs(addlDocs);
    
    				// Save the cruise file and commit it to version control
    				try {
    					String commitMsg;
    					if ( appended )
    						commitMsg = "file for " + datasetId + " appended to by " + 
    								username + " from uploaded file " + filename;
    					else if ( datasetExists )
    						commitMsg = "file for " + datasetId + " updated by " + 
    								username + " from uploaded file " + filename;
    					else
    						commitMsg = "file for " + datasetId + " created by " + 
    								username + " from uploaded file " + filename;			
    					datasetHandler.saveDatasetInfoToFile(datasetData, "Dataset info " + commitMsg);
    					datasetHandler.saveDatasetDataToFile(datasetData, "Dataset data " + commitMsg);
    				} catch (IllegalArgumentException ex) {
    					messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
    							filename + " ; " + datasetId);
    					messages.add(ex.getMessage());
    					messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
    					continue;
    				}
    
    				// Success
    				messages.add(DashboardUtils.SUCCESS_HEADER_TAG + " " + datasetId);
    				successes.add(datasetId);
    			}
            }
		}

		// Update the list of cruises for the user
		try {
			configStore.getUserFileHandler().addDatasetsToListing(successes, username);
		} catch (IllegalArgumentException ex) {
			sendErrMsg(response, "Unexpected error updating list of datasets \n" + ex.getMessage());
			return;
		}

		// Send the success response
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter respWriter = response.getWriter();
		for ( String msg : messages )
			respWriter.println(msg);
		response.flushBuffer();
	}
    */

	private static boolean isPreviewRequest(String action) {
		return DashboardUtils.PREVIEW_REQUEST_TAG.equals(action);
    }

    private static void sendPreview(StandardUploadFields stdFields, HttpServletResponse response) throws IOException  {
        FileItem firstItem = stdFields.dataFiles().iterator().next();
        String filename = firstItem.getName();
        String encoding = stdFields.fileDataEncoding(); 
		// if preview, just return up to 50 lines 
		// of interpreted contents of the first uploaded file
		ArrayList<String> contentsList = new ArrayList<String>(50);
		BufferedReader cruiseReader = new BufferedReader(new InputStreamReader(firstItem.getInputStream(), encoding));
		try {
			for (int k = 0; k < 50; k++) {
				String dataline = cruiseReader.readLine();
				if ( dataline == null )
					break;
				contentsList.add(dataline);
			}
		} finally {
			cruiseReader.close();
		}

		// Respond with some info and the interpreted contents
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
        try ( PrintWriter respWriter = response.getWriter(); ) {
			respWriter.println(DashboardUtils.FILE_PREVIEW_HEADER_TAG);
			respWriter.println("----------------------------------------");
			respWriter.println("Filename: " + filename);
			respWriter.println("Encoding: " + encoding);
			respWriter.println("(Partial) Contents:");
			respWriter.println("----------------------------------------");
			for ( String dataline : contentsList )
				respWriter.println(dataline);
			response.flushBuffer();
			return;
        }
    }

	private static void sendOkMsg(HttpServletResponse response, Set<String> datasets) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
		try ( PrintWriter respWriter = response.getWriter(); ) {
            respWriter.write(DashboardUtils.SUCCESS_HEADER_TAG);
            String comma = "";
    		for ( String dsid : datasets ) {
    			respWriter.println(dsid + comma);
    			comma = ",";
    		}
    		response.flushBuffer();
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
	 */
	private static void sendErrMsg(HttpServletResponse response, String errMsg) {
        try {
    		response.setStatus(HttpServletResponse.SC_OK);
    		response.setContentType("text/html;charset=UTF-8");
            try ( PrintWriter respWriter = response.getWriter(); ) {
        		respWriter.println(errMsg);
        		response.flushBuffer();
            }
        } catch (IOException iox) {
            logger.warn(iox);
        }
	}

	private static void sendErrMsg(HttpServletResponse response, Exception ex) {
        try {
            ex.printStackTrace();
    		response.setStatus(HttpServletResponse.SC_OK);
    		response.setContentType("text/html;charset=UTF-8");
            try ( PrintWriter respWriter = response.getWriter(); ) {
        		respWriter.println(ex.getMessage());
        		response.flushBuffer();
            }
        } catch (IOException iox) {
            logger.warn(iox);
        }
	}

}
