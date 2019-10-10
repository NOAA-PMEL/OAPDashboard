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

    public String getTransferCommand(String stdId, File transferFile) throws Exception;
}
