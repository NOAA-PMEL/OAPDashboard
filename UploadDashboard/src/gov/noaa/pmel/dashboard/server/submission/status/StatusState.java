/**
 * 
 */
package gov.noaa.pmel.dashboard.server.model;

/**
 * @author kamb
 *
 * ###### NOTE: This needs to be kept in sync with DasboardClient/g.n.p.d.client.status.StatusState ######
 */
public enum StatusState {
    INITIAL("Initiated"),
    STAGED("Staged for delivery"),
    SUBMITTED("Submitted"),
    RECEIVED("Received by archive"),
    PENDING_INFO("Pending additional information"),
    VALIDATED("Submission validated"),
    ACCEPTED("Accepted by archive"),
    REJECTED("Rejected by archive"),
    SUPERCEDED("Version has been superceded"),
    RECALLED("Submission has been recalled"),
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
}
