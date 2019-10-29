/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.File;

/**
 * @author kamb
 *
 */
public interface FileTransferOp {

    public String getTransferCommand(File transferFile, String targetFilePath) throws Exception;
}
