/**
 * 
 */
package gov.noaa.pmel.dashboard.server.ws;

import java.io.File;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.Archive;
import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao;
import gov.noaa.pmel.dashboard.server.submission.status.StatusState;
import gov.noaa.pmel.dashboard.server.submission.status.SubmissionRecord;
import gov.noaa.pmel.dashboard.server.util.Notifications;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * @author kamb
 *
 */
@Path("status")
public class StatusServices extends ResourceBase {

    private static Logger logger = LogManager.getLogger(StatusServices.class);
    
    @Context HttpServletRequest httpRequest;
    @Context HttpServletResponse httpResponse;
	@Context ServletContext servletContext;
	@Context SecurityContext securityContext;
    
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response root() {
        System.out.println("status root");
        return Response.ok("status").build();
    }

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAll() {
        Response response = null;
        logger.debug(fullDump(httpRequest));
        logger.info("Status list all " +
                     " from " + getRemoteAddress(httpRequest));
        try {
            List<SubmissionRecord> list = Archive.getAllRecords();
            response = Response.ok().entity(list).build();
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            response = Response.serverError().entity("An error occurred on the server. Please try again later.").build();
        }
        return response;
    }

    @GET
    @Path("{sid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response currentStatusForPkg(@PathParam("sid") String p_sid) {
        return statusForVersion(p_sid, null);
    }
    @GET
    @Path("{sid}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response statusForVersion(@PathParam("sid") String p_sid,
                                     @PathParam("version") String p_version) {
        Response response = null;
        logger.debug(fullDump(httpRequest));
        logger.info("Status check for " + p_sid + 
                     ( p_version != null ? "." + p_version : "" ) + 
                     " from " + getRemoteAddress(httpRequest));
        try {
            SubmissionRecord srec = Archive.getSubmissionRecordForVersion(p_sid, p_version);
            if ( srec != null ) {
                response = Response.ok().entity(srec.status()).build();
            } else {
                response = Response.status(HttpServletResponse.SC_NOT_FOUND)
                            .entity("No submission record found for id " + p_sid + " of version " + p_version).build();
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            response = Response.serverError().entity("An error occurred on the server. Please try again later.").build();
        }
        return response;
    }
    
    @GET
    // @Path("{t_id:ds|sr}/{sid}")
    @Path("{sid}/{show:history|all}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fullStatusForPkg(@PathParam("sid") String p_sid,
                                     @PathParam("show") String p_show) {
        Response response = null;
        logger.debug(fullDump(httpRequest));
        logger.info("Status check for " + p_sid + 
                     " showing " + p_show + 
                     " from " + getRemoteAddress(httpRequest));
        try {
            String show = StringUtils.emptyOrNull(p_show) ? "history" : p_show;
            switch ( show ) {
                case "history":
                    SubmissionRecord srec = Archive.getCurrentSubmissionRecordForPackage(p_sid);
                    response = srec != null ? 
                                Response.ok(srec.getStatusHistory()).build() :
                                Response.status(Status.NOT_FOUND).entity("No archive record found for " + p_sid).build();
                    break;
                case "all":
                    List<SubmissionRecord> list = Archive.getAllVersionsForPackage(p_sid);
                    response = list != null && list.size() > 0 ? 
                                Response.ok(list).build() :
                                Response.status(Status.NOT_FOUND).entity("No archive record found for " + p_sid).build();
                    break;
                default:
                    String msg = "Invalid request: show status \""+show +"\"";
                    logger.warn(msg);
                    response = Response.status(Status.BAD_REQUEST).entity(msg).build();
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            response = Response.serverError().entity("An error occurred on the server. Please try again later.").build();
        }
        return response;
    }
    
//    @GET
//    @Path("list/{what:keys|datasets}")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response allSubmissionsForPkg(@PathParam("what") String p_what) {
//        Response response = null;
//        if ( true ) {
//            return Response.ok("Not Implemented Yet").build();
//        }
//        try {
//            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
//            List<SubmissionRecord> srecs;
//            srecs = sdao.getAll();
//            if ( srecs != null ) {
//                response = Response.ok(srecs).build();
//            } else {
//                response = Response.status(HttpServletResponse.SC_NOT_FOUND)
//                            .entity("No submission record found for id " + p_sid).build();
//            }
//        } catch (Exception ex) {
//            logger.warn(ex.getMessage(), ex);
//            response = Response.serverError().entity("An error occurred on the server. Please try again later.").build();
//        }
//        return response;
//    }
    
    @POST
    @Path("update/{sid}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateStatus(@PathParam("sid") String p_sid,
                                 @QueryParam("s") String qp_status_state,
                                 @QueryParam("m") String qp_message,
                                 @FormParam("status") String fp_status_state,
                                 @FormParam("message") String fp_message) {
        return updateVersionStatus(p_sid, null, qp_status_state, qp_message, fp_status_state, fp_message);
    }
    @POST
    @Path("update/{sid}/{version}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVersionStatus(@PathParam("sid") String p_sid,
                                        @PathParam("version") String p_version,
                                        @QueryParam("s") String qp_status_state,
                                        @QueryParam("m") String qp_message,
                                        @FormParam("status") String fp_status_state,
                                        @FormParam("message") String fp_message) {
        Response response = null;
        SubmissionRecord srec = null;
        try {
            logger.debug(fullDump(httpRequest));
            logger.info("Update " + p_sid + "." + p_version +
                        " as fp status " + fp_status_state + ":" + fp_message +
                        " or qp status " + qp_status_state + ":" + qp_message +
                         " from " + getRemoteAddress(httpRequest));
            String statusStr = fp_status_state != null ? fp_status_state : qp_status_state;
            StatusState sstate = getState(statusStr.toUpperCase());
            String message = fp_message != null ? fp_message : qp_message;
            String notificationTitle = "Status update for " + p_sid;
            String logMessage = notificationTitle + " from " + getRemoteAddress(httpRequest) + " to " + sstate + ":" + message;
            logger.info(logMessage);
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            if ( Archive.isRecordKey(p_sid)) {
                srec = sdao.getLatestByKey(p_sid);
            } else {
                srec = sdao.getLatestForDataset(p_sid);
            }
            if ( srec == null ) {
                response = Response.status(HttpServletResponse.SC_NOT_FOUND)
                            .entity("No submission record found for id " + p_sid).build();
                notificationTitle = "FAILED: " + notificationTitle;
                logMessage = "No record found.\n" + logMessage;
            } else {
                Archive.updateStatus(srec, sstate, message);
                response = Response.ok("Status updated for " + p_sid).build();
            }
            Notifications.AdminEmail(notificationTitle, logMessage);
        } catch (Exception ex) {
            logger.warn(ex, ex);
            Notifications.AdminEmail("Status update FAILED!", ex.toString());
            response = Response.serverError().entity("An error occurred on the server. Please try again later.").build();
        }
        return response;
    }

    /**
     * @param qp_status_state
     * @return
     */
    private StatusState getState(String qp_status_state) {
        return StatusState.from(qp_status_state);
    }

    public static void main(String[] args) {
    }

}
