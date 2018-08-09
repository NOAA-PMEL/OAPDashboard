/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.File;

/**
 * @author kamb
 *
 */
public interface ArchiveBundler {

	public File createArchiveFilesBundle(String stdId, File dataFile);
}
