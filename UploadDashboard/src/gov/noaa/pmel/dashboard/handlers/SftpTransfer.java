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
    public String getTransferCommand(String stdId, File transferFile) throws Exception {
        String dest = getTargetDestinationDir(stdId); // ApplicationConfiguration.getProperty("oap.archive.sftp.destination", "");
        return buildCommand(transferFile, dest);
    }
    
    private String buildCommand(File transferFile, String destDir) throws IOException, PropertyNotFoundException {
        StringBuilder transferCmd = new StringBuilder();
        String command = ApplicationConfiguration.getProperty("oap.archive.sftp.command", _protocol.value());
        String user = getUserId()+"@"+getHost();
        String idFile = getIdFileLocation();
        transferCmd.append("echo \"-mkdir ").append(destDir).append(" \n ")
                   .append("put ").append(transferFile.getCanonicalPath()).append(SPACE).append(destDir).append(" \" | ")
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
        try {
            File sendFile = new File("src/log4j2.properties");
            String xfer = new SftpTransfer().getTransferCommand("log4j", sendFile);
            System.out.println(xfer);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }


}
