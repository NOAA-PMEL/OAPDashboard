/**
 * 
 */
package gov.noaa.pmel.dashboard.server.util;

import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao;
import gov.noaa.pmel.dashboard.server.model.SubmissionRecord;
import gov.noaa.pmel.dashboard.server.model.SubmissionStatus;

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
    
    private static void usage() {
        System.out.println( "usage: ( -ds <datasetID> | -k <submissionKey> ) [ -v <versionNum> ] -s status* [ -m <status message> ]\n" +
                            "Valid status states are:\n" + validStatusStatesList());
    }
    /*
     * status States:
    INITIAL("Initiated"),
    STAGED("Staged for delivery"),
    RECEIVED("Received by archive"),
    PENDING_INFO("Pending additional information"),
    VALIDATED("Submission validated"),
    ACCEPTED("Accepted by archive"),
    REJECTED("Rejected by archive"),
    PROCESSING_ERROR("Error processing submission");
    */

    private static enum StatusState {
        initial("Initiated", SubmissionStatus.State.INITIAL),
        staged("Staged for delivery", SubmissionStatus.State.STAGED),
        received("Received by archive", SubmissionStatus.State.RECEIVED),
        pending("Pending additional information", SubmissionStatus.State.PENDING_INFO),
        valid("Submission validated", SubmissionStatus.State.VALIDATED),
        accepted("Accepted by archive", SubmissionStatus.State.ACCEPTED),
        rejected("Rejected by archive", SubmissionStatus.State.REJECTED),
        error("Error processing submission", SubmissionStatus.State.PROCESSING_ERROR);
        
        private String _explanation;
        private SubmissionStatus.State _ssState;
        private StatusState(String explanation, SubmissionStatus.State ssState) {
            _explanation = explanation;
            _ssState = ssState;
        }
        @Override
        public String toString() {
            return this.name() + " :\t" + _explanation;
        }
        public String explanation() {
            return _explanation;
        }
        public SubmissionStatus.State ssState() {
            return _ssState;
        }
    }
    
    private static StatusState getSubmissionState() {
        StatusState sst = StatusState.valueOf(_status.toLowerCase());
        return sst;
    }
    
//    private static final String INITIAL("Initiated"),
//    private static final String STAGED = "\"staged\"\t: Staged for delivery";
//    private static final String RECEIVED = "\"received\"\t: Received by archive";
//    private static final String PENDING_INFO = "\"pending\"\t: Pending additional information";
//    private static final String VALIDATED = "\"valid\"\t: Submission package validated";
//    private static final String ACCEPTED = "\"accepted\"\t: Accepted by archive";
//    private static final String REJECTED = "\"rejected\"\t: Submission rejected by archive";
//    private static final String PROCESSING_ERROR = "\"error\"\t: Error processing submission";
//    private static final String[] statesList = new String[] {
//            STAGED, RECEIVED, PENDING_INFO, VALIDATED, ACCEPTED, REJECTED, PROCESSING_ERROR
//    };
    
    private static String validStatusStatesList() {
        StringBuilder sb = new StringBuilder();
        for (StatusState state : StatusState.values() ) {
            sb.append(state).append("\n");
        }
        return sb.toString();
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
                _message = sst.explanation();
            }
            SubmissionRecord srec = null;
            if ( _submitKey != null ) {
                srec = _version != null ? 
                        sdao.getByVersionKey(_submitKey, _version) :
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
            SubmissionStatus ss = SubmissionStatus.builder()
                    .submissionId(srec.dbId())
                    .status(sst.ssState())
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
