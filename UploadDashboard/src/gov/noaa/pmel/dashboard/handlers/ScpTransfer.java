/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.File;
import java.io.IOException;

import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.ApplicationConfiguration.PropertyNotFoundException;

/**
 * @author kamb
 *
 */
public class ScpTransfer extends BaseTransferAgent implements FileTransferOp {

    
    public ScpTransfer() {
        super(FileXferService.XFER_PROTOCOL.SCP);
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
        String command = ApplicationConfiguration.getProperty("oap.archive.scp.command", _protocol.value());
        String user = getUserId()+"@"+getHost();
        transferCmd.append(command)
                   .append(" -i ")
                   .append(getIdFileLocation()).append(SPACE)
                   .append(transferFile.getCanonicalPath()).append(SPACE)
                   .append(user).append(":").append(dest);

        return transferCmd.toString();
    }

}
