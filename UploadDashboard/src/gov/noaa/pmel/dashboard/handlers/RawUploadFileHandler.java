/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;

/**
 * Handles saving the original uploaded files.
 * Files are saved under the configuration-specified directory in a directory that is:
 * username/year/timestamp/uploaded-filename
 * where timestamp is in the format %MMdd'T'hhmmss (month-day-T-hour-minute-second)
 * 
 * @author kamb
 *
 */
public class RawUploadFileHandler /* extends VersionedFileHandler */ {

    private static Logger logger = LogManager.getLogger(RawUploadFileHandler.class);
    
	/**
	 * @param filesDirName
	 * @param svnUsername
	 * @param svnPassword
	 * @throws IllegalArgumentException
	 */
    private File filesDir;
	public RawUploadFileHandler(String filesDirName, String svnUsername, String svnPassword) throws IllegalArgumentException {
//		super(filesDirName, svnUsername, svnPassword);
        filesDir = new File(filesDirName);
	}

    public File writeFileItem(FileItem item,  String username) throws Exception {
        File targetDir = createUploadTargetDir(username);
        File itemFile = writeItem(item, targetDir);
        return itemFile;
    }
	public File createUploadTargetDir(String username) {
		File rawFiles = filesDir;
		if ( !rawFiles.exists()) {
			boolean created = rawFiles.mkdirs();
			if ( !created ) {
				throw new IllegalStateException("Unable to create raw uploads base directory: " + rawFiles.getPath());
			} else {
				System.out.println("Created uploads directory " + rawFiles.getAbsolutePath());
			}
		}
		String format = "yyyy'"+File.separator+"'MMdd'T'hhmmss";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String timestamp = sdf.format(new Date());
		String filePath = username + File.separator + timestamp;
		File targetDir = new File(rawFiles, filePath);
		if ( !targetDir.exists()) {
			boolean created = targetDir.mkdirs();
			if ( !created ) {
				System.out.println("Failed to create user target dir " + targetDir.getAbsolutePath());
				throw new IllegalStateException("Unable to create raw uploads user target directory: " + targetDir.getPath());
			} else {
				System.out.println("Created user upload target directory " + targetDir.getAbsolutePath());
			}
		}
		return targetDir;
	}
	
	private static File getRawFileTarget(File targetDir, FileItem item) {
        String uploadFilename = item.getName();
        logger.debug("item filename" + uploadFilename);
        if ( uploadFilename.indexOf('\\') >= 0 ) {
            logger.debug("Trimming windows path of :" + uploadFilename);
            uploadFilename = uploadFilename.substring(uploadFilename.lastIndexOf('\\')+1);
        }

		File targetFile = new File(targetDir, uploadFilename);
		return targetFile;
	}

	public static File writeItem(FileItem item, File targetDir) throws Exception {
		if ( ! targetDir.exists()) {
			if ( ! targetDir.mkdirs()) {
				throw new IllegalStateException("Unable to create target directory " + targetDir.getAbsolutePath());
			}
		}
		File rawFile = getRawFileTarget(targetDir, item);
		item.write(rawFile);
        return rawFile;
//		commitVersion(rawFile, "Uploaded raw file: " + item.getName());
	}

}
