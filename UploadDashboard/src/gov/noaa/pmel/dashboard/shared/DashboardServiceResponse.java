/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author kamb
 *
 */
@Data
@Builder
@AllArgsConstructor
public class DashboardServiceResponse<R> implements Serializable, IsSerializable {

    private static final long serialVersionUID = 273980474568876388L;

    private R _response;
    @Builder.Default
    private boolean _wasSuccessful = true;
    
    // XXX WARNING: Should set these global metadata fields once and for all...
    // Currently set in DashboardServices.getDatasetList();
    private String _version;
    @Builder.Default
    private long _maxUploadSize = -1;
    @Builder.Default
    private String _maxUploadSizeDisplayStr = "N/A";

    @SuppressWarnings("unused") // For GWT
    private DashboardServiceResponse() {
    }
    public DashboardServiceResponse(R response) {
        _response = response;
    }
    
    public R response() { return _response; }
    
    // Because I can't get lombok to work for client code...
    public String getVersion() {
        return _version;
    }
    public long getMaxUploadSize() {
        return _maxUploadSize;
    }
    public String getMaxUploadSizeDisplayStr() {
        return _maxUploadSizeDisplayStr;
    }

    public boolean wasSuccessful() {
        return _wasSuccessful;
    }
    public void setSuccessful(boolean successful) {
        _wasSuccessful = successful;
    }
    
    private String _message;
    public String message() {
        return _message;
    }
    
    private String _error;
    public String error() {
       return _error;   
    }
}
