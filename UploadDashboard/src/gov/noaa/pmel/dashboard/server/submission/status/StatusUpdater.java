/**
 * 
 */
package gov.noaa.pmel.dashboard.server.submission.status;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.Archive;
import gov.noaa.pmel.dashboard.server.DashboardException;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.server.Users;
import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.server.submission.status.StatusState;
import gov.noaa.pmel.dashboard.server.util.Notifications;
import gov.noaa.pmel.dashboard.shared.NotFoundException;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;

import static gov.noaa.pmel.dashboard.server.submission.status.StatusMessageFlag.*;


/**
 * @author kamb
 *
 */
public class StatusUpdater {

    private static final String _DSID = "-i";
    private static final String _VERSION = "-v";
    private static final String _STATUS = "-s";
    private static final String _MESSAGE = "-m";
    private static final String _ACCESSION = "-a";
    
    private static String _datasetId;
    private static Integer _version;
    private static String _status;
    private static String _message;
    private static String _accession;
    
    private static Logger logger = LogManager.getLogger(StatusUpdater.class);
    
    private static void usage() {
        System.out.println( "usage: ( -ds <datasetID> | -k <submissionKey> ) [ -v <versionNum> ] -s status* [ -m <status message> ]\n" +
                            "Valid status states are:\n" + validStatusStatesList());
    }

    private static StatusState getSubmissionState() {
        StatusState sst = StatusState.from(_status);
        return sst;
    }
    
    private static String validStatusStatesList() {
        StringBuilder sb = new StringBuilder();
        for (StatusState state : StatusState.values() ) {
            sb.append(state).append("\n");
        }
        return sb.toString();
    }
    
    public static SubmissionRecord updateStatusRecord(String datasetId, 
                                                      StatusState sstage,
                                                      String message) {
        throw new RuntimeException("Deprecated: use updateStatusRecord(datasetId, sstate, updateParams"); // XXX TODO: remove
    }
    
    public static SubmissionRecord updateStatusRecord(String datasetId, 
                                                      StatusState sstate,
                                                      Map<String, String> updateParams)
            throws NotFoundException, DashboardException {
        try {
            SubmissionRecord srec;
            String notificationTitle = "Status update for " + datasetId;
            String updateMessage = updateParams.containsKey(MESSAGE.name()) ? 
                                    updateParams.get(MESSAGE.name()) :
                                    sstate.displayMsg();
            String logMessage = "";
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            if ( Archive.isRecordKey(datasetId)) {
                srec = sdao.getLatestByKey(datasetId);
            } else {
                srec = sdao.getLatestForDataset(datasetId);
            }
            if ( srec == null ) {
                notificationTitle = "FAILED: " + notificationTitle;
                logMessage = "No submission record found.\n";
                Notifications.AdminEmail(notificationTitle, logMessage);
                throw new NotFoundException("No submission record found for dataset ID: " + datasetId);
            } else {
                StatusRecord currentState = srec.status();
                Archive.updateStatus(srec, sstate, updateMessage);
                logMessage = "The archive status for submission record " + datasetId +
                        " has been changed from " + currentState.status() + ":" + currentState.message() +
                        " to " + sstate + ":" + updateMessage;
                logger.info(logMessage);
                if ( ApplicationConfiguration.getProperty("oap.archive.update.notify.user", false)) {
                    try {
                        User submitter = Users.getDataSubmitter(srec.datasetId());
                        Notifications.SendEmail("Status updated for " + datasetId, logMessage, submitter.email());
                    } catch (Exception ex) {
                        logger.warn("Exception sending status update to submitter: " + ex , ex);
                        StringBuilder exMsg = new StringBuilder(ex.toString()).append("\n");
                        exMsg.append(DashboardServerUtils.stackTraceToString(ex.getStackTrace()));
                        Notifications.AdminEmail("Exception sending status update message.", exMsg.toString());
                    }
                }
            }
            Notifications.AdminEmail(notificationTitle, logMessage);
            return srec;
        } catch (Exception ex) {
            logger.warn("Exception during update status process:"+ex, ex);
            String exceptionMsg = DashboardServerUtils.exceptionToString(ex);
            Notifications.AdminEmail("Exception updating status for " + datasetId, exceptionMsg);
            throw new DashboardException("Exception updating archive status for dataset " + datasetId, ex);
        }
    }
    
    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case _DSID:
                    _datasetId = args[++i];
                    break;
                case _VERSION:
                    String vStr = args[++i];
                    _version = new Integer(vStr);
                    break;
                case _STATUS:
                    _status = args[++i];
                    break;
                case _MESSAGE:
                    _message = args[++i];
                    break;
                case _ACCESSION:
                    _accession = args[++i];
                    break;
            }
        }
    }
    private static void checkArgs() throws Exception {
        if ( _datasetId == null ) {
            System.out.println("No record id");
            throw new IllegalArgumentException("You must specify dataset record ID.");
        }
        if ( _status == null ) {
            System.out.println("No status");
            throw new IllegalArgumentException("Please specifiy submission status from:\n" + validStatusStatesList());
        }
    }
    
    private static void dumpArgs(String[] args) {
        for (String arg : args) {
            System.out.println(arg);
        }
    }
    /**
     * @param args
     */
    // This is misleading, because it doesn't update everything.
//    public static void main(String[] args) {
//        try {
//            dumpArgs(args);
//            parseArgs(args);
//            checkArgs();
//            StatusState sst = getSubmissionState();
//            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
//            if ( _message == null ) {
//                _message = sst.displayMsg();
//            }
//            SubmissionRecord srec = null;
//            srec = _version != null ? 
//                    sdao.getVersionForDataset(_datasetId, _version) :
//                    sdao.getLatestForDataset(_datasetId);
//            if ( srec == null ) {
//                System.err.println("Unable to find submission record using dataset id: " + _datasetId + " and version: " + _version);
//                System.exit(-1);
//            }
//            StatusRecord ss = StatusRecord.builder()
//                    .submissionId(srec.dbId())
//                    .status(sst)
//                    .message(_message)
//                    .build();
//            DaoFactory.SubmissionsDao().updateSubmissionStatus(ss);
//        } catch (IllegalArgumentException ex) {
//            System.err.println(ex.getMessage());
//            usage();
//            System.exit(-2);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            usage();
//            System.exit(-3);
//        }
//    }

}
