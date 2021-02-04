/**
 * 
 */
package gov.noaa.pmel.dashboard.server.submission.status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.Archive;
import gov.noaa.pmel.dashboard.server.DashboardException;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.server.Users;
import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.server.util.Notifications;
import gov.noaa.pmel.dashboard.shared.NotFoundException;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;

/**
 * @author kamb
 *
 */
public class StatusUpdater {

    private static final String DSID = "-ds";
    private static final String KEY = "-k";
    private static final String VERSION = "-v";
    private static final String STATUS = "-s";
    private static final String MESSAGE = "-m";
    
    private static String _datasetId;
    private static String _submitKey;
    private static Integer _version;
    private static String _status;
    private static String _message;
    
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
    
    public static SubmissionRecord updateStatus(String datasetId, StatusState sstate, String message) 
            throws NotFoundException, DashboardException {
        try {
            SubmissionRecord srec;
            String notificationTitle = "Status update for " + datasetId;
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
                Archive.updateStatus(srec, sstate, message);
                logMessage = "The archive status for submission record " + datasetId +
                        " has been changed from " + currentState.status() + ":" + currentState.message() +
                        " to " + sstate + ":" + message;
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
            logger.warn(ex, ex);
            String exceptionMsg = DashboardServerUtils.exceptionToString(ex);
            Notifications.AdminEmail("Exception updating status for " + datasetId, exceptionMsg);
            throw new DashboardException("Exception updating archive status for dataset " + datasetId, ex);
        }
    }
    
    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case DSID:
                    _datasetId = args[++i];
                    break;
                case KEY:
                    _submitKey = args[++i];
                    break;
                case VERSION:
                    String vStr = args[++i];
                    _version = new Integer(vStr);
                    break;
                case STATUS:
                    _status = args[++i];
                    break;
                case MESSAGE:
                    _message = args[++i];
                    break;
            }
        }
    }
    private static void checkArgs() throws Exception {
        if ( _datasetId == null && _submitKey == null ) {
            System.out.println("No id or key");
            throw new IllegalArgumentException("You must specify either submission key or dataset ID.");
        }
        if ( _datasetId != null && _submitKey != null ) {
            System.out.println("Both id and key");
            throw new IllegalArgumentException("Cannot specify both submission key and dataset ID. Please use one or the other.");
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
    public static void main(String[] args) {
        try {
            dumpArgs(args);
            parseArgs(args);
            checkArgs();
            StatusState sst = getSubmissionState();
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            if ( _message == null ) {
                _message = sst.displayMsg();
            }
            SubmissionRecord srec = null;
            if ( _submitKey != null ) {
                srec = _version != null ? 
                        sdao.getVersionByKey(_submitKey, _version) :
                        sdao.getLatestByKey(_submitKey);
                if ( srec == null ) {
                    throw new IllegalArgumentException("Unable to find submission record using submisison key: " + _submitKey + " and version: " + _version);
                }
            } else {
                srec = _version != null ? 
                        sdao.getVersionForDataset(_datasetId, _version) :
                        sdao.getLatestForDataset(_datasetId);
                if ( srec == null ) {
                    System.err.println("Unable to find submission record using dataset id: " + _datasetId + " and version: " + _version);
                    System.exit(-1);
                }
            }
            StatusRecord ss = StatusRecord.builder()
                    .submissionId(srec.dbId())
                    .status(sst)
                    .message(_message)
                    .build();
            DaoFactory.SubmissionsDao().updateSubmissionStatus(ss);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            usage();
            System.exit(-2);
        } catch (Exception ex) {
            ex.printStackTrace();
            usage();
            System.exit(-3);
        }

    }

}
