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
    NOT_SUBMITTED("Not submitted", "Package has not been submitted"),
    INITIAL("Initiated", "Submission process initiated"),
    STAGED("Submitted", "Staged for delivery"),
    RECEIVED("Received", "Received by archive"),
    INCOMPLETE("Incomplete", "Required information is incomplete"),
    PENDING_INFO("Pending", "Pending additional information"),
    VALIDATED("Validated", "Submission validated"),
    ACCEPTED("Accepted", "Submission accepted by archive"),
    PUBLISHED("Published", "Published by archive"),
    FAILED("Failed", "Archive cannot process the submitted package"),
    REJECTED("Rejected", "Rejected by archive"),
    SUPERSEDED("Superseded", "Version has been superseded"),
//    RECALLED("Submission has been recalled"),
    ERROR("Processing Error", "An error occurred processing the submission"),
    OTHER("Other: see message");
        
    private StatusState(String displayMsg) {
        this(displayMsg, displayMsg);
    }
    private StatusState(String displayMsg, String detailMsg) {
        _display = displayMsg;
        _detail = detailMsg;
    }
    private String _display;
    private String _detail;
        
    public String displayMsg() { return _display; }
    public String detailMsg() { return _detail; }
    
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
