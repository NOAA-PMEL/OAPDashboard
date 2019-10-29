/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import gov.noaa.pmel.dashboard.handlers.FileXferService.XFER_PROTOCOL;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.ApplicationConfiguration.PropertyNotFoundException;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * @author kamb
 *
 */
public abstract class BaseTransferAgent {

    protected final String SPACE = " ";
    private String userid;
    private String host;
    private String idFileSpecifier;
    private String idFileLocation;
    private String targetDestination;
    private String protocolDestRoot;
    
    protected FileXferService.XFER_PROTOCOL _protocol;
    
    protected BaseTransferAgent(XFER_PROTOCOL protocol) {
        _protocol = protocol;
    }
    
    protected String getUserId() throws PropertyNotFoundException {
        if ( userid == null ) {
            userid = ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".username");
        }
        return userid;
    }
    protected String getHost() throws PropertyNotFoundException {
        if ( host == null ) {
            host = ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".hostname");
        }
        return host;
    }
    
    protected String getIdFileSpecifier() throws PropertyNotFoundException {
        if ( idFileSpecifier == null ) {
            String id_file_flag=ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".id_file_flag", "-o");
            String id_file = getIdFileLocation();
            idFileSpecifier = id_file_flag + ("-o".equals(id_file_flag) ? "IdentityFile=" : SPACE) + id_file;
        }
        return idFileSpecifier;
    }
    protected String getIdFileLocation() throws PropertyNotFoundException {
        if ( idFileLocation == null ) {
            idFileLocation = ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".id_file");
        }
        return idFileLocation;
    }
    protected String _getTargetDestinationDir(String stdId) throws PropertyNotFoundException {
        if ( targetDestination == null ) {
            targetDestination = ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".destination", "");
            if ( ! StringUtils.emptyOrNull(targetDestination) && !targetDestination.endsWith("/")) {
                targetDestination += "/";
            }
            targetDestination += stdId;
            if ( !targetDestination.endsWith("/")) {
                targetDestination += "/";
            }
        }
        return targetDestination;
    }
    protected String getProtocolDestinationRoot() throws PropertyNotFoundException {
        if ( protocolDestRoot == null ) {
            protocolDestRoot = ApplicationConfiguration.getProperty("oap.archive."+_protocol.value()+".destination", "");
            if ( ! StringUtils.emptyOrNull(protocolDestRoot) && !protocolDestRoot.endsWith("/")) {
                protocolDestRoot += "/";
            }
        }
        return protocolDestRoot;
    }
    protected String getTargetDestinationDir(String targetFilePath) throws PropertyNotFoundException {
        if ( targetDestination == null ) {
            // We'll leave how to handle the root up to the protocol-specific handlers.
//            targetDestination = getProtocolDestinationRoot();
//            if ( ! StringUtils.emptyOrNull(targetDestination) && !targetDestination.endsWith("/")) {
//                targetDestination += "/";
//            }
            String destDir = targetFilePath.indexOf("/", 1) > 0 ? targetFilePath.substring(0, targetFilePath.lastIndexOf('/')) : "";
            targetDestination = destDir;
            if ( !targetDestination.endsWith("/")) {
                targetDestination += "/";
            }
        }
        return targetDestination;
    }
    
}
