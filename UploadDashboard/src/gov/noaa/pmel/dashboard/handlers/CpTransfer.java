/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.File;
import java.io.IOException;

import gov.noaa.pmel.dashboard.handlers.FileXferService.XFER_PROTOCOL;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.ApplicationConfiguration.PropertyNotFoundException;

/**
 * @author kamb
 *
 */
public class CpTransfer extends BaseTransferAgent implements FileTransferOp {

    
    public CpTransfer() {
        super(XFER_PROTOCOL.CP);
    }
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.handlers.FileTransferOp#getTransferCommand()
     */
    @Override
    public String getTransferCommand(String stdId, File transferFile) throws Exception {
        String destDir = getTargetDestinationDir(stdId);
        return buildCommand(transferFile, destDir);
    }
    
    private String buildCommand(File transferFile, String destDir) throws IOException, PropertyNotFoundException {
        StringBuilder transferCmd = new StringBuilder();
        String command = "cp";
        transferCmd.append("([ -e ").append(destDir).append(" ] || mkdir -p ").append(destDir).append(" ) && ")
                   .append(command).append(SPACE)
                   .append(transferFile.getCanonicalPath()).append(SPACE)
                   .append(destDir);

        return transferCmd.toString();
    }

}
