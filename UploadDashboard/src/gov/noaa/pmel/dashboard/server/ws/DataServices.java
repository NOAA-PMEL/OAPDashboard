/**
 * 
 */
package gov.noaa.pmel.dashboard.server.ws;

import java.io.File;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.tws.util.StringUtils;

import org.apache.logging.log4j.LogManager;

/**
 * @author kamb
 *
 */
@Path("data")
public class DataServices extends ResourceBase {

    private static Logger logger = LogManager.getLogger(DataServices.class);
    
    @Context HttpServletRequest httpRequest;
    @Context HttpServletResponse httpResponse;
	@Context ServletContext servletContext;
	@Context SecurityContext securityContext;
    
    // "tomcat/content/OAPUploadDashboard/ArchiveBundles/bags/29AH20110128/29AH20110128_20200203T070959Z/29AH20110128_bagit.zip");
    private static File bagRoot = null; 
    
    /**
     * 
     */
    public DataServices() {
        bagRoot = getBagRoot();
    }

    @GET
    @Path("{pckg}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadDataPackage(@PathParam("pckg") String p_pckg) {
        Response response = null;
        logger.debug(fullDump(httpRequest));
        logger.debug("get data package for " + p_pckg +
                     " from " + getRemoteAddress(httpRequest));
        if ( StringUtils.emptyOrNull(p_pckg)) {
            return Response.status(Status.BAD_REQUEST).entity("Invalid package ID").build();
        }
        String ucPckg = p_pckg.toUpperCase();
        try {
            File pckgFile = getFileFor(ucPckg);
            if ( pckgFile != null && pckgFile.exists()) {
                ResponseBuilder rb = Response.ok(pckgFile);
                rb.header("Content-Disposition", "attachment;filename=" + pckgFile.getName());
                response = rb.build();
            } else {
                response = Response.status(Status.NOT_FOUND).entity("No data package found.  Perhaps the package hold time has expired.").build();
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            response = Response.serverError().entity("An error occurred on the server. Please try again later.").build();
        }
        return response;
        
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
