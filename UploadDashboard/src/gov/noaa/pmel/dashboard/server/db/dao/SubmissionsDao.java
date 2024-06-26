/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.dao;

import java.sql.SQLException;
import java.util.List;

import gov.noaa.pmel.dashboard.server.submission.status.StatusRecord;
import gov.noaa.pmel.dashboard.server.submission.status.SubmissionRecord;

/**
 * @author kamb
 *
 */
public interface SubmissionsDao {
    
    public SubmissionRecord addDatasetSubmission(String datasetID, String pkgLocation) throws SQLException;
    
    public void insert(SubmissionRecord submission) throws SQLException;
    public SubmissionRecord initialSubmission(SubmissionRecord submission) throws SQLException;

    public void updateSubmission(SubmissionRecord submission) throws SQLException;
    
    /* Convenience method, uses SubmissionStatusMapper. */
    public void updateSubmissionStatus(StatusRecord status) throws SQLException;
    
    public SubmissionRecord getById(long id) throws SQLException;
//    public SubmissionRecord getFullById(long id) throws SQLException;

    public SubmissionRecord getVersionByKey(String key, int version) throws SQLException;
    public SubmissionRecord getLatestByKey(String key) throws SQLException;
    
    public SubmissionRecord getVersionForDataset(String datasetId, int version) throws SQLException;
    public SubmissionRecord getLatestForDataset(String datasetId) throws SQLException;
    
    public List<SubmissionRecord> getAllVersionsByKey(String key) throws SQLException;
    public List<SubmissionRecord> getAllVersionsForDataset(String datasetId) throws SQLException;

    public List<SubmissionRecord> getAllRecords() throws SQLException;
    
}
