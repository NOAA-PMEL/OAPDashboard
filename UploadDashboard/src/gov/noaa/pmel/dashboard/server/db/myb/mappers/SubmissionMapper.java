/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.myb.mappers;

import java.sql.SQLException;
import java.util.Map;

import gov.noaa.pmel.dashboard.server.model.SubmissionRecord;

/**
 * @author kamb
 *
 */
public interface SubmissionMapper { // extends SubmissionsDao {

    public long addDatasetSubmission(Map<String, Object>map) throws SQLException; // String datasetId, String packageLocation) throws SQLException;
    
    public void insertSubmission(SubmissionRecord submission) throws SQLException;
//    public void updateSubmission(SubmissionRecord submission) throws SQLException;
    
    public void initialSubmission(SubmissionRecord submission) throws SQLException;
    
    public SubmissionRecord getById(long dbId) throws SQLException;
//    public SubmissionRecord getFullById(long dbId) throws SQLException;
    public SubmissionRecord getLatestByKey(String submissionKey) throws SQLException;
    public SubmissionRecord getVersionByKey(String submissionKey, int version) throws SQLException;
    public SubmissionRecord getLatestForDatasetId(String datasetId) throws SQLException;
    public SubmissionRecord getVersionForDatasetId(String datasetId, int version) throws SQLException;
}
