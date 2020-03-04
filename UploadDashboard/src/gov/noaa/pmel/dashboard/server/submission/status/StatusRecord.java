/**
 * 
 */
package gov.noaa.pmel.dashboard.server.submission.status;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import gov.noaa.pmel.tws.util.StringUtils;
import gov.noaa.pmel.tws.util.TimeUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
@Builder(toBuilder=true)
public class StatusRecord {

    private transient Long _dbId;
    
    @NonNull
    @JsonIgnore
    private Long _submissionId;
    
    @JsonProperty("status_update")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss Z")
    private Date _statusTime;
    
    @JsonProperty("status")
    private StatusState _status;
    
    @JsonProperty("message")
    private String _message;
    
    public static StatusRecord initialStatus(Long submissionId) {
        return StatusRecord.builder().submissionId(submissionId)
                    .status(StatusState.INITIAL)
                    .message("Archive submission initiated.")
                    .build();
    }
    
    public String displayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TimeUtils.formatUTC(_statusTime, TimeUtils.non_std_ISO_8601_nofrac_Z))
          .append(" : ")
          .append(_status.displayMsg());
        if ( ! StringUtils.emptyOrNull(_message)) {
            sb.append(" : ").append(_message);
        }
        return sb.toString();
    }
}
