/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.myb.mappers;

import java.sql.SQLException;
import java.util.List;

import gov.noaa.pmel.dashboard.server.model.StatusRecord;

/**
 * @author kamb
 *
 */
public interface StatusRecordMapper {
    
    public void insertStatus(StatusRecord status) throws SQLException;

    public StatusRecord getLatestForSubmission(Long submissionId) throws SQLException;
    public List<StatusRecord> getAllForSubmission(Long submissionId) throws SQLException;
}
