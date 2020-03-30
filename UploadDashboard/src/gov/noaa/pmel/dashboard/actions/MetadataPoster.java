/**
 * 
 */
package gov.noaa.pmel.dashboard.actions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import gov.noaa.pmel.dashboard.oads.OADSMetadata;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.MetadataPreviewInfo;
import gov.noaa.pmel.dashboard.shared.NotFoundException;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.FileUtils;
import gov.noaa.pmel.tws.util.Logging;

/**
 * @author kamb
 *
 */
public class MetadataPoster {

    private static Logger logger = Logging.getLogger(MetadataPoster.class);
    
    /**
     * 
     */
    public MetadataPoster() {
        // TODO Auto-generated constructor stub
    }
    
    public static MetadataPreviewInfo postMetadata(HttpServletRequest httpRequest, String username, String datasetId) 
            throws NotFoundException {
        String docId = null;
        String metadataEditorPostEndpoint = null;
        try {
			File mdFile = OADSMetadata.getMetadataFile(datasetId);
			if ( !mdFile.exists()) {
				mdFile = OADSMetadata.getExtractedMetadataFile(datasetId);
			} 
			if ( !mdFile.exists()) { // dataset has either not been checked or critically failed check.
//                try {
//                    StdUserDataArray stdArray = 
//                    DashboardOADSMetadata mdata = OADSMetadata.extractOADSMetadata(stdArray);
//                    configStore.getMetadataFileHandler().saveAsOadsXmlDoc(mdata, 
//                                                                          DashboardUtils.autoExtractedMdFilename(datasetId), 
//                                                                          "Initial Auto-extraction");
//                }
                
                mdFile = OADSMetadata.createEmptyOADSMetadataFile(datasetId);
			}
            // XXX HttpClient and stuff coming (currently) from netcdfAll jar 
            @SuppressWarnings("resource")
            HttpClient client = HttpClients.createDefault();
            metadataEditorPostEndpoint = getMetadataPostPoint(httpRequest, datasetId);
            logger.debug("metadataPost: " + metadataEditorPostEndpoint);
            HttpPost post = new HttpPost(metadataEditorPostEndpoint);
            FileBody body = new FileBody(mdFile, ContentType.APPLICATION_XML);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addPart("xmlFile", body);
            String notifyUrl = getNotificationUrl(httpRequest.getRequestURL().toString(), datasetId);
            System.out.println("noticationUrl: " + notifyUrl);
            StringBody notificationUrl = new StringBody(notifyUrl, ContentType.MULTIPART_FORM_DATA);
            builder.addPart("notificationUrl", notificationUrl);
            HttpEntity postit = builder.build();
            post.setEntity(postit);
            HttpResponse response = client.execute(post);
            logger.debug("response: " + response);
            HttpEntity responseEntity = response.getEntity();
            String responseContent = readFully(responseEntity.getContent());
            StatusLine statLine = response.getStatusLine();
            int responseStatus = statLine.getStatusCode();
            if ( responseStatus != HttpServletResponse.SC_OK ) {
                logger.warn("ME response: " + statLine.getStatusCode() + ":" + statLine.getReasonPhrase());
                String msg = responseContent;
                logger.warn("ME response content: " + msg);
                throw new IllegalArgumentException(msg);
            }
            docId = responseContent;
            MetadataPreviewInfo mdInfo = OADSMetadata.getMetadataPreviewInfo(username, datasetId); 
//            ServletContext context = request.getSession().getServletContext();
            String mdDocId = getMetadataEditorPage(httpRequest, docId);
            mdInfo.setMdDocId(mdDocId);
            return mdInfo;
        } catch (HttpHostConnectException hcex) {
            logger.warn("Unable to connect to MetadataEditor at " + metadataEditorPostEndpoint + ": " + hcex);
            throw new NotFoundException("Unable to connect to MetadataEditor. <br/>Please contact your administrator.");
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    private static String getMetadataPostPoint(HttpServletRequest request, String datasetId) throws Exception {
        String requestUrl = request.getRequestURL().toString();
        System.out.println("get ME post point request: " + requestUrl);
        String context = request.getContextPath();
        System.out.println("get ME post point context: " + context);
        String meUrlProp = ApplicationConfiguration.getProperty(DashboardConfigStore.METADATA_EDITOR_POST_ENDPOINT);
        String url;
        if ( meUrlProp.toLowerCase().startsWith("http")) {
            url = meUrlProp;
        } else {
            String requestBase = requestUrl.substring(0, requestUrl.indexOf(context));
            url = requestBase + meUrlProp;
        }
        String slash = url.endsWith("/") ? "" : "/";
        url = url + slash + datasetId;
        System.out.println("Post point: " + url);
        return url;
    }
    
    // request URL is in the form: http://matisse:8080/OAPUploadDashboard/OAPUploadDashboard/DashboardServices
    // or http://dunkel.pmel.noaa.gov:5680/oa/Dashboard/OAPUploadDashboard/DashboardServices
	// And notification URL should be http://dunkel.pmel.noaa.gov:5680/oa/Dashboard/DashboardUpdateService/notify/<datasetId>
    private static String getNotificationUrl(String requestUrl, String datasetId) {
        System.out.println("get notify URL request: " + requestUrl);
        String notifyUrl = null;
        if ( requestUrl.indexOf("pmel") > 0 ) {
            notifyUrl = "https://www.pmel.noaa.gov/sdig" + requestUrl.substring(requestUrl.indexOf("/oa"));
            System.out.println("n1: " + notifyUrl);
            notifyUrl = notifyUrl.substring(0, notifyUrl.indexOf("OAPUploadDashboard"));
            System.out.println("n2: " + notifyUrl);
            notifyUrl = notifyUrl + "DashboardUpdateService/notify/"+datasetId;
        } else {
            notifyUrl = requestUrl.substring(0, requestUrl.lastIndexOf("OAPUploadDashboard"));
            notifyUrl = notifyUrl + "DashboardUpdateService/notify/"+datasetId;
        }
        System.out.println("nofity url: " + notifyUrl);
        return notifyUrl;
    }

    // inside url typically looks like <host>//<root context>/Dashboard
    private static String getMetadataEditorPage(HttpServletRequest request, String docId) throws Exception {
        String requestUrl = request.getRequestURL().toString();
        System.out.println("get ME page request: " + requestUrl);
        String context = request.getContextPath();
        System.out.println("get ME context: " + context);
        String meUrlProp = ApplicationConfiguration.getProperty(DashboardConfigStore.METADATA_EDITOR_URL);
        String url;
        if ( meUrlProp.toLowerCase().startsWith("http")) {
            url = meUrlProp;
        } else {
            String requestBase = requestUrl.substring(0, requestUrl.indexOf(context));
            url = requestBase + meUrlProp;
        }
        url = url + "?id="+docId;
        System.out.println("Editor page: " + url);
        return url;
    }
    
    /**
     * @param inStream
     * @return
     * @throws IOException 
     */
    private static String readFully(InputStream inStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
     
        buffer.flush();
        byte[] byteArray = buffer.toByteArray();
             
        String text = new String(byteArray, StandardCharsets.UTF_8);
        return text;
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
