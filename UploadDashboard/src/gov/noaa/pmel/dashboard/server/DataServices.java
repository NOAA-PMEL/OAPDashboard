/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;

import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.tws.util.StringUtils;

import org.apache.logging.log4j.LogManager;

/**
 * @author kamb
 *
 */
@Path("data")
public class DataServices extends HttpServlet {

    private static Logger logger = LogManager.getLogger(DataServices.class);
    
    // "tomcat/content/OAPUploadDashboard/ArchiveBundles/bags/29AH20110128/29AH20110128_20200203T070959Z/29AH20110128_bagit.zip");
    private static File bagRoot = null; 
    public DataServices() {
        bagRoot = getBagRoot();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug(request.getRemoteAddr() + " : " + request);
        System.out.println(request.getRemoteAddr() + " : " + request);
    
        String p_pckg = request.getPathInfo();
        logger.debug("get data package for " + p_pckg + " from " + getRemoteAddress(request));
        if ( StringUtils.emptyOrNull(p_pckg)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid package ID");
            return;
        }
        String ucPckg = p_pckg.toUpperCase();
        try {
            File pckgFile = getFileFor(ucPckg);
            if ( pckgFile != null && pckgFile.exists()) {
                String type = URLConnection.guessContentTypeFromName(pckgFile.getName());
                response.setContentType(type);
                response.setContentLengthLong(pckgFile.length());
                response.setHeader("Content-Disposition", "attachment;filename=" + pckgFile.getName());
                Files.copy(pckgFile.toPath(), response.getOutputStream());
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No data package found.  Perhaps the package hold time has expired.");
                return;
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred on the server. Please try again later.");
        }
    }

    protected static String getRemoteAddress(HttpServletRequest httpRequest) {
        String remoteAddr = httpRequest.getHeader("x-forwarded-for");
        if ( remoteAddr == null ) {
            remoteAddr = httpRequest.getRemoteAddr();
        }
        return remoteAddr;
    }
    
    static File getBagRoot() {
        File contentRoot = DashboardConfigStore.getAppContentDir();
        File baggy = new File(contentRoot, "ArchiveBundles/bags");
        return baggy;
    }
    
    /**
     * @param p_pckg
     * @return
     */
    private static File getFileFor(String pckg) {
        File packageFile = null;
        File datasetDir = new File(bagRoot, pckg);
        if ( datasetDir.exists()) {
            SortedSet<String> versions = new TreeSet<>(Arrays.asList(datasetDir.list()));
            File latest = new File(datasetDir, versions.last());
            packageFile = new File(latest, pckg+"_bagit.zip");
        }
        return packageFile;
    }
}
