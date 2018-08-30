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
    public String getTransferCommand(File transferFile) throws Exception {
        String dest = getTargetDestination();
        return buildCommand(transferFile, dest);
    }
    
    private String buildCommand(File transferFile, String dest) throws IOException, PropertyNotFoundException {
        StringBuilder transferCmd = new StringBuilder();
        String command = "cp";
        transferCmd.append(command).append(SPACE)
                   .append(transferFile.getCanonicalPath()).append(SPACE)
                   .append(dest);

        return transferCmd.toString();
    }

}
