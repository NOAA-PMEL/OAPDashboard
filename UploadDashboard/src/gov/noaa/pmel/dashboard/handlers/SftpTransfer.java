/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.File;
import java.io.IOException;

import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.ApplicationConfiguration.PropertyNotFoundException;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * @author kamb
 *
 */
public class SftpTransfer extends BaseTransferAgent implements FileTransferOp {

    
    public SftpTransfer() {
        super(FileXferService.XFER_PROTOCOL.SFTP);
    }
    
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.handlers.FileTransferOp#getTransferCommand()
     */
    @Override
    public String getTransferCommand(File transferFile) throws Exception {
        String dest = ApplicationConfiguration.getProperty("oap.archive.sftp.destination", "");
        return buildCommand(transferFile, dest);
    }
    
    private String buildCommand(File transferFile, String dest) throws IOException, PropertyNotFoundException {
        StringBuilder transferCmd = new StringBuilder();
        String command = ApplicationConfiguration.getProperty("oap.archive.sftp.command", _protocol.value());
        String user = getUserId()+"@"+getHost();
        String idFile = getIdFileLocation();
        transferCmd.append("echo \"put " + transferFile.getCanonicalPath() + " "+dest+"/\" | ")
                   .append(command)
                   .append(" -b - ");
        if ( ! StringUtils.emptyOrNull(idFile)) {
            transferCmd.append(getIdFileSpecifier())
                        .append(SPACE);
        }
        transferCmd.append(user);

        return transferCmd.toString();
    }
        
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }


}
