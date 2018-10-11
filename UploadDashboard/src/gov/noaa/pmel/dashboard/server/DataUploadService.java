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
            Set<String>successes = uploadProcessor.getSuccesses();
            sendResponseMsg(response, successes, messages);
        } catch (BadRequestException vex) { // baseRequestValidation
            logger.warn(vex);
            sendErrMsg(response, vex);
        } catch (FileUploadException fex) { // parseParameterMap
            logger.warn(fex);
            sendErrMsg(response, fex);
        } catch (IllegalStateException iex) { // no username
            logger.warn(iex);
            sendErrMsg(response, iex);
        } catch (NoSuchFieldException nsf) { // missing field
            logger.warn(nsf);
            sendErrMsg(response, nsf);
        } catch (IllegalArgumentException iex) { // no files
            logger.warn(iex);
            sendErrMsg(response, iex);
        } catch (Exception ex) {
            logger.warn(ex, ex);
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

    /**
     * @param response
     * @param messages
     * @throws IOException 
     */
    private void sendResponseMsg(HttpServletResponse response, Set<String> successes, List<String> messages) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
		try ( PrintWriter respWriter = response.getWriter(); ) {
            for (String msg :  messages ) {
                respWriter.println(msg);
    		}
    		response.flushBuffer();
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
