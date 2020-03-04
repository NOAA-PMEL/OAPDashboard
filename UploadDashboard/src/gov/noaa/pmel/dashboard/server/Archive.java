/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao;
import gov.noaa.pmel.dashboard.server.submission.status.StatusRecord;
import gov.noaa.pmel.dashboard.server.submission.status.StatusState;
import static gov.noaa.pmel.dashboard.server.submission.status.StatusState.*;
import gov.noaa.pmel.dashboard.server.submission.status.SubmissionRecord;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * @author kamb
 *
 */
public class Archive {


    private static Logger logger = LogManager.getLogger(Archive.class);
    
    /**
     * @param p_sid
     * @return
     */
    public static boolean isRecordKey(String p_sid) {
        return p_sid.startsWith("SDIS");
    }

    public static List<SubmissionRecord> getAllVersionsForPackage(String pkgId) throws Exception {
        try {
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            List<SubmissionRecord> list = isRecordKey(pkgId) ? 
                                            sdao.getAllVersionsByKey(pkgId) :
                                            sdao.getAllVersionsForDataset(pkgId);
            return list;
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            throw ex;
        }
    }
        
    public static SubmissionRecord getCurrentSubmissionRecordForPackage(String p_sid) throws Exception {
        return getSubmissionRecordForVersion(p_sid, null);
    }
    
    public static SubmissionRecord getSubmissionRecordForVersion(String p_sid, String p_version) throws Exception {
        try {
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            SubmissionRecord srec;
            String s_version = StringUtils.emptyOrNull(p_version) ? 
                                "current" : 
                                p_version;
                                    
            switch (s_version) {
                case "current":
                case "latest":
                    srec = isRecordKey(p_sid) ? 
                            sdao.getLatestByKey(p_sid) :
                            sdao.getLatestForDataset(p_sid);
                    break;
                default:
                    int version = Integer.parseInt(s_version);
                    srec = isRecordKey(p_sid) ? 
                            sdao.getVersionByKey(p_sid, version) :
                            sdao.getVersionForDataset(p_sid, version);
                    break;
            }
            return srec;
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            throw ex;
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    /**
     * @param srec
     * @param sstate
     * @param message
     * @throws SQLException 
     */
     public static void updateStatus(SubmissionRecord srec, StatusState sstate, String message) throws SQLException {
         SubmissionsDao sdao = DaoFactory.SubmissionsDao();
         StatusRecord status = StatusRecord.builder().submissionId(srec.dbId())
                                    .status(sstate)
                                    .message(message)
                                    .build();
        sdao.updateSubmissionStatus(status);
    }

    /**
     * @param srec
     * @param ex
     * @throws SQLException 
     */
    public static void submissionFailed(SubmissionRecord srec, Exception ex) throws SQLException {
         String message = ERROR.displayMsg();
         if ( ex != null ) {
             message += " : " + ex.getMessage();
         }
         SubmissionsDao sdao = DaoFactory.SubmissionsDao();
         StatusRecord status = StatusRecord.builder().submissionId(srec.dbId())
                                    .status(ERROR)
                                    .message(message)
                                    .build();
        sdao.updateSubmissionStatus(status);
    }

    /**
     * @return
     */
    public static List<SubmissionRecord> getAllRecords() throws SQLException {
        try {
            SubmissionsDao sdao = DaoFactory.SubmissionsDao();
            List<SubmissionRecord> list = sdao.getAllRecords();
            return list;
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            throw ex;
        }
    }

}
