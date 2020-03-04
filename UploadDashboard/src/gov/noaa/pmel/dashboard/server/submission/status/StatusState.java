/**
 * 
 */
package gov.noaa.pmel.dashboard.server.submission.status;

/**
 * @author kamb
 *
 * ###### NOTE: This needs to be kept in sync with DasboardClient/g.n.p.d.client.status.StatusState ######
 */
public enum StatusState {
    INITIAL("Initiated"),
    STAGED("Staged for delivery"),
    SUBMITTED("Submitted to archive"),
    RECEIVED("Received by archive"),
    PENDING_INFO("Pending additional information"),
    VALIDATED("Submission validated"),
    ACCEPTED("Accepted by archive"),
    REJECTED("Rejected by archive"),
    SUPERCEDED("Version has been superceded"),
//    RECALLED("Submission has been recalled"),
    ERROR("An error occurred processing the submission"),
    OTHER("Other: see message"),
    PROCESSING_ERROR("Error processing submission");
        
    public static StatusState from(String str) {
        StatusState ss = null;
        String ucStr = str.toUpperCase();
        if ( ucStr.startsWith("PENDING")) {
            ss = PENDING_INFO;
        } else if ( ucStr.contains("ERROR")) {
            ss = PROCESSING_ERROR;
        } else {
            try {
                ss = StatusState.valueOf(ucStr);
            } catch (Exception ex) {
                ss = OTHER; ss._display = str;
            }
        }
        return ss;
    }
    private StatusState(String displayMsg) {
        _display = displayMsg;
    }
    private String _display;
        
    public String displayMsg() { return _display; }
    
    public static StatusState lookup(String part) throws IllegalArgumentException {
        String lookup = part.toUpperCase();
        StatusState state = null;
        for ( StatusState checkState : values()) {
            if ( checkState.name().startsWith(lookup)) {
                if ( state != null ) {
                    throw new IllegalArgumentException("Cannot distinguish state partial name: " + part +
                                                       ". Found " + state + " and " + checkState);
                }
                state = checkState;
            }
        }
        if ( state == null ) {
            throw new IllegalArgumentException("No Status State found for " + part);
        }
        return state;
    }
}
