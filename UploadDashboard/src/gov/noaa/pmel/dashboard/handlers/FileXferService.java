/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.ApplicationConfiguration.PropertyNotFoundException;
import gov.noaa.pmel.tws.util.process.CommandRunner;

/**
 * @author kamb
 *
 */
public class FileXferService {

    private static Logger logger = LogManager.getLogger(FileXferService.class);
    
    private XFER_PROTOCOL _protocol;
    private FileTransferOp _transferOp;
    
    public static enum XFER_PROTOCOL {
        EMAIL,
        SFTP,
        SCP,
        CP;
        
        public String value() {
            return this.name().toLowerCase();
        }
        public static XFER_PROTOCOL from(String name) {
            return XFER_PROTOCOL.valueOf(name.toUpperCase());
        }
    }
        
    /**
     * @param stdId
     * @param archiveBundle
     * @param userRealName
     * @param userEmail
     * @throws Exception 
     */
    public static int putArchiveBundle(String stdId, File archiveBundle, String userRealName, String userEmail) throws Exception {
        System.out.println("Submitting " + stdId + " archive bundle " + archiveBundle + " to FTP site." );
        return new FileXferService().submitArchiveBundle(stdId, archiveBundle, userRealName, userEmail);
    }

    public FileXferService(XFER_PROTOCOL protocol) {
        _protocol = protocol;
        _transferOp = getFileTransferOp(_protocol);
    }
    private FileXferService() {
        _protocol = XFER_PROTOCOL.from(ApplicationConfiguration.getProperty("oap.archive.mode", XFER_PROTOCOL.SFTP.name()));
        _transferOp = getFileTransferOp(_protocol);
    }
    
    /**
     * @param forProtocol
     * @return
     */
    private static FileTransferOp getFileTransferOp(XFER_PROTOCOL forProtocol) {
        switch (forProtocol) {
            case SFTP:
                return new SftpTransfer();
            case SCP:
                return new ScpTransfer();
            case CP:
                return new CpTransfer();
            default:
                throw new IllegalStateException("Unknown protocol:" + forProtocol);
        }
    }

    public int submitArchiveBundle(String stdId, File archiveBundle, String userRealName, String userEmail) throws Exception {
//        try {
            String command = _transferOp.getTransferCommand(archiveBundle);
            logger.debug("xfer cmd: " + command);
            CommandRunner runner = new CommandRunner(command);
            int exitStatus = runner.runCommand();
            if ( exitStatus == 0 ) {
                exitStatus = submitHash(archiveBundle);
            }
            return exitStatus;
//        } catch (PropertyNotFoundException pex) {
//            pex.printStackTrace();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }
        
    /**
     * @param archiveBundle
     * @return
     */
    private int submitHash(File archiveBundle) throws Exception {
        String fname = archiveBundle.getName();
        String fbase = fname.substring(0, fname.lastIndexOf('.'));
        File hashFile = new File(archiveBundle.getParentFile(), fbase+"-sha256.txt");
        String command = _transferOp.getTransferCommand(hashFile);
        logger.debug("xfer cmd: " + command);
        CommandRunner runner = new CommandRunner(command);
        int exitStatus = runner.runCommand();
        return exitStatus;
    }

    public static void main(String[] args) {
        try {
            File archiveBundle = new File("/local/tomcat/oap_content/OAPUploadDashboard/MetadataDocs/NEMO/NEMOPRE052012/extracted_NEMOPRE052012_OADS.xml");
            new FileXferService(XFER_PROTOCOL.SFTP).submitArchiveBundle("datasetID", archiveBundle, "Real Name", "real.name@noaa.gov");
            new FileXferService(XFER_PROTOCOL.SCP).submitArchiveBundle("datasetID", archiveBundle, "Real Name", "real.name@noaa.gov");
            new FileXferService(XFER_PROTOCOL.CP).submitArchiveBundle("datasetID", archiveBundle, "Real Name", "real.name@noaa.gov");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
