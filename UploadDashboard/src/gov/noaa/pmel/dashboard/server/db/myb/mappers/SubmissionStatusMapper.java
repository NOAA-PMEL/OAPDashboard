/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.myb.mappers;

import java.sql.SQLException;
import java.util.List;

import gov.noaa.pmel.dashboard.server.model.SubmissionStatus;

/**
 * @author kamb
 *
 */
public interface SubmissionStatusMapper {
    
    public void insertStatus(SubmissionStatus status) throws SQLException;

    public SubmissionStatus getLatestForSubmission(Long submissionId) throws SQLException;
    public List<SubmissionStatus> getAllForSubmission(Long submissionId) throws SQLException;
}
