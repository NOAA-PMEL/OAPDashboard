/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.oads.OADSMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

/**
 * Service to receive the uploaded metadata file from the client
 * 
 * @author Linus Kamb
 */
public class NotificationService extends HttpServlet {

	private static final long serialVersionUID = -1458504704372812166L;

	public NotificationService() {
	}

	private static int ID_POS = 1;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        response.getOutputStream().write(("Sounds good: " + path + "!").getBytes());
        response.flushBuffer();
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        System.out.println("Post to: " + request.getRequestURL().toString());
        String path = request.getPathInfo(); 
        String location = request.getHeader("Location");
        String docId = location.substring(location.lastIndexOf('/')+1);
        String datasetId = path.substring(1);
        System.out.println(new Date() + "Notified of update to " + datasetId + " at location: " + location +", path: " + path);
//        setupRetrieveMetadataFile(location, datasetId);
        response.getOutputStream().write(("gotcha baby #"+docId+"@"+location).getBytes());
        response.flushBuffer();
        System.out.println("flushed");
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        retrieveMetadataFile(location, datasetId);
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
                    Thread.sleep(50);
                    retrieveMetadataFile(location, datasetId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        new Thread(retriever).start();
    }

    /**
     * @param datasetId
	 * @throws IOException 
	 * @throws  
     */
    private static void retrieveMetadataFile(String location, String datasetId) throws IOException {
        System.out.println(new Date() + " Retrieving " + datasetId + " from " + location);
        MetadataFileHandler metaHandler = DashboardConfigStore.get().getMetadataFileHandler();
        File metaFile = metaHandler.getMetadataFile(datasetId);
        HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(location);
        HttpResponse response = client.execute(get);
        if ( response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK ) {
            throw new IOException(response.getStatusLine().toString());
        }
        try ( InputStream is = response.getEntity().getContent();
              FileOutputStream fos = new FileOutputStream(metaFile); ) {
            byte[] buf = new byte[4096];
            int read;
            while (( read = is.read(buf)) > 0 ) {
                fos.write(buf, 0, read);
            }
        }
		String localTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm Z").format(new Date());
        DataFileHandler df = DashboardConfigStore.get().getDataFileHandler();
        DashboardDataset dataset = df.getDatasetFromInfoFile(datasetId);
        dataset.setMdTimestamp(localTimestamp);
        String msg = new Date() + " Updating metadata timestamp on user upload metadata file.";
        System.out.println(msg);
        df.saveDatasetInfoToFile(dataset, msg);
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
