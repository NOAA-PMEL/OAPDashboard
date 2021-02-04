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
    NOT_SUBMITTED("Not submitted"),
    INITIAL("Initiated"),
    STAGED("Staged for delivery"),
    SUBMITTED("Submitted to archive"),
    RECEIVED("Received by archive"),
    INCOMPLETE("Required information is incomplete"),
    PENDING_INFO("Pending additional information"),
    VALIDATED("Submission validated"),
    ACCEPTED("Accepted by archive"),
    FAILED("Archive cannot process the submitted package"),
    REJECTED("Rejected by archive"),
    SUPERCEDED("Version has been superceded"),
//    RECALLED("Submission has been recalled"),
    ERROR("An error occurred processing the submission"),
    OTHER("Other: see message"),
    PROCESSING_ERROR("Error processing submission");
        
    private StatusState(String displayMsg) {
        _display = displayMsg;
    }
    private String _display;
        
    public String displayMsg() { return _display; }
    
    public static StatusState from(String part) throws IllegalArgumentException {
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
