/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.SubmissionsDao;
import gov.noaa.pmel.dashboard.server.model.SubmissionRecord;
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

    public static SubmissionRecord getArchiveStatusForPackage(String p_sid) throws Exception {
        return getArchiveStatusForVersion(p_sid, null);
    }
    
    public static SubmissionRecord getArchiveStatusForVersion(String p_sid, String p_version) throws Exception {
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

}
