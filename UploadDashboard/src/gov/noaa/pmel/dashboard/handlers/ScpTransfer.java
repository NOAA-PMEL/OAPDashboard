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
    public String getTransferCommand(File transferFile, String targetFilePath) throws Exception {
        String destRoot = getProtocolDestinationRoot();
        String destDir = destRoot + getTargetDestinationDir(targetFilePath);
        return buildCommand(transferFile, destDir);
    }
    
    private String buildCommand(File transferFile, String destDir) throws IOException, PropertyNotFoundException {
        StringBuilder transferCmd = new StringBuilder();
        String user = getUserId()+"@"+getHost();
        StringBuilder mkCmd = new StringBuilder()
                .append("ssh").append(SPACE)
                   .append(getIdFileSpecifier()).append(SPACE)
                   .append(user).append(SPACE)
                   .append("\" [ -e ").append(destDir).append(" ] ||  mkdir -p ").append(destDir).append("\"");
        String scpCmd = ApplicationConfiguration.getProperty("oap.archive.scp.command", _protocol.value());
        transferCmd.append(scpCmd).append(SPACE)
                   .append(getIdFileSpecifier()).append(SPACE)
                   .append(transferFile.getCanonicalPath()).append(SPACE)
                   .append(user).append(":").append(destDir);

        StringBuilder fullCmd = new StringBuilder();
        fullCmd.append(mkCmd.toString()).append(" && ").append(transferCmd.toString());
        return fullCmd.toString();
    }

}
