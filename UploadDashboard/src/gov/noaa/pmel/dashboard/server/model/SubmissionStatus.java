/**
 * 
 */
package gov.noaa.pmel.dashboard.server.model;

import java.util.Date;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
@Builder(toBuilder=true)
public class SubmissionStatus {

    private transient Long _dbId;
    
    @NonNull
    private Long _submissionId;
    
    private Date _statusTime;
    
    private State _status;
    
    private String _message;
    
    public static enum State {
        INITIAL("Initiated"),
        STAGED("Staged for delivery"),
        RECEIVED("Received by archive"),
        PENDING_INFO("Pending additional information"),
        VALIDATED("Submission validated"),
        ACCEPTED("Accepted by archive"),
        REJECTED("Rejected by archive"),
        PROCESSING_ERROR("Error processing submission");
        
        private State(String displayMsg) {
            _display = displayMsg;
        }
        private String _display;
        
        public String displayMsg() { return _display; }
    }
    
    public static SubmissionStatus initialStatus(Long submissionId) {
        return SubmissionStatus.builder().submissionId(submissionId)
                    .status(State.INITIAL)
                    .message("Archive submission initiated.")
                    .build();
    }
}
