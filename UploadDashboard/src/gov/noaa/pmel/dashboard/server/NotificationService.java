/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import gov.noaa.ncei.oads.xml.v_a0_2_2.BaseVariableType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.OadsMetadataDocumentType;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.oads.OADSMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.oads.util.TimeUtils;
import gov.noaa.pmel.oads.xml.a0_2_2.OadsXmlReader;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.Logging;

/**
 * Service to receive the uploaded metadata file from the client
 * 
 * @author Linus Kamb
 */
public class NotificationService extends HttpServlet {

	private static final long serialVersionUID = -1458504704372812166L;
    
	private static final Logger logger = Logging.getLogger(NotificationService.class);

	public NotificationService() {
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        response.getOutputStream().write(("Sounds good: " + path + "!").getBytes());
        response.flushBuffer();
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.info("Post to: " + request.getRequestURL().toString());
        String path = request.getPathInfo(); 
        String location = request.getHeader("Location");
        String docId = location.substring(location.lastIndexOf('/')+1);
        String datasetId = path.substring(1);
        logger.info(new Date() + "Notified of update to " + datasetId + " at location: " + location +", path: " + path);
        String contentType = request.getHeader("Content-Type");
        if ( contentType != null && "text/xml".equals(contentType)) {
            saveXml(datasetId, request);
        } else {
            setupRetrieveMetadataFile(location, datasetId);
        }
        response.getOutputStream().write(("gotcha baby #"+docId+"@"+location).getBytes());
        response.flushBuffer();
        logger.debug("flushed");
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
	}
    
	/**
     * @param location
     * @param datasetId
     */
    private static void setupRetrieveMetadataFile(final String location, final String datasetId) {
        Runnable retriever = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Retriver running.");
                    Thread.sleep(500);
                    retrieveMetadataFile(location, datasetId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        new Thread(retriever).start();
    }

	private static void saveXml(String datasetId, HttpServletRequest request) {
        try ( InputStream in = request.getInputStream(); ) {
            saveAndValidateMetadata(datasetId, in);
        } catch (Exception ex) {
            logger.warn(ex,ex);
        }
	}
    
	private static void saveAndValidateMetadata(String datasetId, InputStream in) throws Exception {
            OadsMetadataDocumentType metadata = saveXmlFromStream(datasetId, in);
            DashboardDataset info = DashboardConfigStore.get().getDataFileHandler().getDatasetFromInfoFile(datasetId);
            String validationMessage = ApplicationConfiguration.getProperty("oap.metadata.validate", true) ? 
                                        OADSMetadata.validateMetadata(info, metadata) :
                                        "Not checked.";
    		String timestamp = TimeUtils.formatUTC(new Date(), "yyyy-MM-dd HH:mm Z");
            DataFileHandler df = DashboardConfigStore.get().getDataFileHandler();
            DashboardDataset dataset = df.getDatasetFromInfoFile(datasetId);
            dataset.setMdTimestamp(timestamp);
            dataset.setMdStatus(validationMessage);
            String msg = new Date() + " Updating metadata timestamp on user upload metadata file.";
            logger.info(msg);
            df.saveDatasetInfoToFile(dataset, msg);
            checkMetadataDataColumns(dataset, metadata);
	}
	
	/**
     * @param dataset
     * @param metadata
     */
    private static void checkMetadataDataColumns(DashboardDataset dataset, OadsMetadataDocumentType metadata) {
        List<String> userColumns = (List<String>)dataset.getUserColNames().clone();
        for ( BaseVariableType var : metadata.getVariables()) {
            if ( ! userColumns.contains(var.getDatasetVarName())) {
                logger.warn(dataset.getRecordId() + ": Metadata has variable " + toString(var) + " which doesn't exist in dataset.");
            } else {
                userColumns.remove(var.getDatasetVarName());
            }
        }
        for ( String missingColumn : userColumns) {
            logger.info(dataset.getRecordId() + ": Metadata does not have variable for user data column " + missingColumn);
        }
    }

    private static String toString(BaseVariableType var) {
        return "["+var.getDatasetVarName()+"] " + var.getFullName();
    }
    private static OadsMetadataDocumentType saveXmlFromStream(String datasetId, InputStream in) throws Exception {
        MetadataFileHandler metaHandler = DashboardConfigStore.get().getMetadataFileHandler();
        File metaFile = metaHandler.getMetadataFile(datasetId);
        Path metaFilePath = metaFile.toPath();
        Files.copy(in, metaFilePath, StandardCopyOption.REPLACE_EXISTING);
        OadsMetadataDocumentType xml = OadsXmlReader.read(metaFile);
        return xml;
	}
	
    /**
     * @param datasetId
	 * @throws IOException 
	 * @throws  
     */
    @SuppressWarnings("resource")
    private static void retrieveMetadataFile(String location, String datasetId) throws Exception {
        logger.info(new Date() + " Retrieving " + datasetId + " from " + location);
        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(location);
        HttpResponse response = client.execute(get);
        if ( response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK ) {
            throw new IOException(response.getStatusLine().toString());
        }
        try ( InputStream is = response.getEntity().getContent(); ) {
            saveAndValidateMetadata(datasetId, is);
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
