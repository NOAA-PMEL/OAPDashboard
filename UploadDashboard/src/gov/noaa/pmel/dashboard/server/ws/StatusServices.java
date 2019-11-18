/**
 * 
 */
package gov.noaa.pmel.dashboard.server.ws;

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
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao;
import gov.noaa.pmel.dashboard.server.model.SubmissionRecord;
import gov.noaa.pmel.dashboard.server.util.Notifications;
import gov.noaa.pmel.dashboard.server.model.StatusRecord;
import gov.noaa.pmel.dashboard.server.model.StatusState;
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
    // @Path("{t_id:ds|sr}/{sid}")
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
        try {
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            SubmissionRecord srec;
            if ( StringUtils.emptyOrNull(p_version)) {
                if ( isRecordKey(p_sid)) {
                    srec = sdao.getLatestByKey(p_sid);
                } else {
                    srec = sdao.getLatestForDataset(p_sid);
                }
            } else {
                int version = Integer.parseInt(p_version);
                if ( isRecordKey(p_sid)) {
                    srec = sdao.getVersionByKey(p_sid, version);
                } else {
                    srec = sdao.getVersionForDataset(p_sid, version);
                }
                
            }
            if ( srec != null ) {
                response = Response.ok(srec.status()).build();
            } else {
                response = Response.status(HttpServletResponse.SC_NOT_FOUND)
                            .entity("No submission record found for id " + p_sid).build();
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            response = Response.serverError().entity("An error occurred on the server. Please try again later.").build();
        }
        return response;
    }
    
    @GET
    // @Path("{t_id:ds|sr}/{sid}")
    @Path("{sid}/history")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fullStatusForPkg(@PathParam("sid") String p_sid) {
        Response response = null;
        try {
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            SubmissionRecord srec;
            if ( isRecordKey(p_sid)) {
                srec = sdao.getLatestByKey(p_sid);
            } else {
                srec = sdao.getLatestForDataset(p_sid);
            }
            if ( srec != null ) {
                response = Response.ok(srec.getStatusHistory()).build();
            } else {
                response = Response.status(HttpServletResponse.SC_NOT_FOUND)
                            .entity("No submission record found for id " + p_sid).build();
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            response = Response.serverError().entity("An error occurred on the server. Please try again later.").build();
        }
        return response;
    }
    
    @GET
    // @Path("{t_id:ds|sr}/{sid}")
    @Path("{sid}/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response allSubmissionsForPkg(@PathParam("sid") String p_sid) {
        Response response = null;
        if ( true ) {
            return Response.ok("Not Implemented Yet").build();
        }
        try {
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            List<SubmissionRecord> srecs;
            if ( isRecordKey(p_sid)) {
                srecs = sdao.getAllVersionsByKey(p_sid);
            } else {
                srecs = sdao.getAllVersionsForDataset(p_sid);
            }
            if ( srecs != null ) {
                response = Response.ok(srecs).build();
            } else {
                response = Response.status(HttpServletResponse.SC_NOT_FOUND)
                            .entity("No submission record found for id " + p_sid).build();
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            response = Response.serverError().entity("An error occurred on the server. Please try again later.").build();
        }
        return response;
    }
    
    /**
     * @param p_sid
     * @return
     */
    private static boolean isRecordKey(String p_sid) {
        return p_sid.startsWith("SDIS");
    }

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
        try {
            logger.debug(httpRequest);
            String statusStr = fp_status_state != null ? fp_status_state : qp_status_state;
            StatusState sstate = getState(statusStr.toUpperCase());
            String message = fp_message != null ? fp_message : qp_message;
            String logMessage = "Status update for " + p_sid + " from " + httpRequest.getRemoteAddr() + " to " + sstate + ":" + message;
            logger.info(logMessage);
            Notifications.AdminEmail("Status update for " + p_sid, logMessage);
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            SubmissionRecord srec;
            if ( isRecordKey(p_sid)) {
                srec = sdao.getLatestByKey(p_sid);
            } else {
                srec = sdao.getLatestForDataset(p_sid);
            }
            if ( srec == null ) {
                response = Response.status(HttpServletResponse.SC_NOT_FOUND)
                            .entity("No submission record found for id " + p_sid).build();
            } else {
                StatusRecord status = StatusRecord.builder().submissionId(srec.dbId())
                                            .status(sstate)
                                            .message(message)
                                            .build();
                sdao.updateSubmissionStatus(status);
                response = Response.ok().build();
            }
            
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            Notifications.AdminEmail("Status update failed!", ex.getMessage());
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
