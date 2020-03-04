/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.File;

import gov.noaa.pmel.dashboard.server.submission.status.SubmissionRecord;

/**
 * @author kamb
 *
 */
public interface ArchiveBundler {

	public File createArchiveFilesBundle(SubmissionRecord archiveRecordId, String stdId, File dataFile) throws Exception;
}
