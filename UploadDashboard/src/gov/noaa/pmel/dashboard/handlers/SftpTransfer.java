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
    
    protected static String getTargetFileName(String filePath) throws PropertyNotFoundException {
        String filename = filePath.indexOf("/") >= 0 ? filePath.substring(filePath.lastIndexOf('/')+1) : filePath;
        return filename;
    }
    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.handlers.FileTransferOp#getTransferCommand()
     */
    @Override
    public String getTransferCommand(File transferFile, String targetFilePath) throws Exception {
        String destDir = getTargetDestinationDir(targetFilePath); // ApplicationConfiguration.getProperty("oap.archive.sftp.destination", "");
        String fileName = getTargetFileName(targetFilePath);
        System.out.println("destDir:"+destDir);
        System.out.println("destName:"+fileName);
        return buildCommand(transferFile, destDir, fileName);
    }

    private String buildCommand(File transferFile, String destDir, String fileName) throws IOException, PropertyNotFoundException {
        StringBuilder transferCmd = new StringBuilder();
        transferCmd.append("echo \"")
                   .append("cd ").append(getProtocolDestinationRoot()).append(" \n ");
        String command = ApplicationConfiguration.getProperty("oap.archive.sftp.command", _protocol.value());
        String user = getUserId()+"@"+getHost();
        String idFile = getIdFileLocation();
        String[] destDirs = destDir.split("/");
        String curDir = "";
        if ( destDirs.length > 0 ) {
            for (String pathDir : destDirs) {
                curDir = curDir+pathDir+"/";
                transferCmd.append("-mkdir ").append(curDir).append(" \n ");
            }
        }
        transferCmd.append("put ").append(transferFile.getCanonicalPath()).append(SPACE).append(curDir+fileName).append(" \" | ")
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
//            File sendFile = new File("src/log4j2.properties");
//            String xfer = new SftpTransfer().getTransferCommand("log4j", sendFile);
//            System.out.println(xfer);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }
}
