/**
 * 
 */
package gov.noaa.pmel.dashboard.server.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonProperty("submission_record")
    private String _submissionKey;
    
    @JsonProperty("submit_message")
    private String _submitMsg;
    
    @Setter(AccessLevel.PUBLIC)
    @JsonIgnore
    private String _archiveBag;
    
    @Setter(AccessLevel.PUBLIC)
    @JsonIgnore
    private String _pkgLocation;
    
    @Singular("addStatus")
    @JsonProperty("status_history")
    private List<StatusRecord> _statusHistory;
    
    public StatusRecord status() {
        return _statusHistory != null && ! _statusHistory.isEmpty() ?
                _statusHistory.get(0) : null;
    }
    
    public void updateStatus(StatusRecord newStatus) {
        getStatusHistory().add(0,newStatus);
    }
    
    /**
     * @return _statusHistory list, guaranteed not to be null;
     */
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
