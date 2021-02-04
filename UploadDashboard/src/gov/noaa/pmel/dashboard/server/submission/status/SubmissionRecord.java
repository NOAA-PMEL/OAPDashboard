/**
 * 
 */
package gov.noaa.pmel.dashboard.server.submission.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Singular;

/**
 * @author kamb
 *
 */
@Data
//@Setter(AccessLevel.NONE)
@Builder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"submit_time", "dataset_id", "record_id", "version", "submit_message", "package_location", "status_history" })
public class SubmissionRecord {

    private transient Long _dbId;
    @JsonProperty("submit_time")
    private Date _submissionTime;
    @JsonIgnore
    private Long _submitterId;

    @JsonProperty("dataset_id")
    private String _datasetId;
    
    @Default
    @JsonProperty("version")
    private Integer _version = 1;
    @JsonProperty("record_id")
    private String _submissionKey;
    
    @JsonProperty("submit_message")
    private String _submitMsg;
    
    @Setter(AccessLevel.PUBLIC)
    @JsonIgnore
    private String _archiveBag;
    
    @Setter(AccessLevel.PUBLIC)
    @JsonProperty("package_location")
    private String _pkgLocation;
    
    @Singular("addStatus")
    @JsonProperty("status_history")
    private List<StatusRecord> _statusHistory;
    
    public StatusRecord status() {
        return _statusHistory != null && ! _statusHistory.isEmpty() ?
                    _statusHistory.get(0) : 
                    StatusRecord.NOT_SUBMITTED;
    }
    
    public void updateStatus(StatusRecord newStatus) {
        getStatusHistory().add(0,newStatus);
    }
    
    /**
     * @return _statusHistory list, guaranteed not to be null;
     */
    @JsonIgnore
    public synchronized List<StatusRecord> getStatusHistory() {
        if ( _statusHistory == null ) {
            _statusHistory = new ArrayList<>();
        }
        return _statusHistory;
    }
    
    public SubmissionRecord newVersion(String submitMsg) {
        _version = new Integer(_version.intValue()+1);
        _statusHistory = new ArrayList<>(); // builder creates unmodifiable list.
        _submitMsg = submitMsg;
        _submissionTime = new Date();
        return this;
    }
    
    // required for Mybatis 
    public SubmissionRecord(Long dbId) {
        super();
        this._dbId = dbId;
    }
    
    // required for Mybatis 
    public SubmissionRecord(Long dbId, Date submissionTime, Long submitterId, String datasetId, Integer version,
            String submissionKey, String submitMsg, String archiveBag, String pkgLocation) {
        super();
        this._dbId = dbId;
        this._submissionTime = submissionTime;
        this._submitterId = submitterId;
        this._datasetId = datasetId;
        this._version = version;
        this._submissionKey = submissionKey;
        this._submitMsg = submitMsg;
        this._archiveBag = archiveBag;
        this._pkgLocation = pkgLocation;
    }
}
