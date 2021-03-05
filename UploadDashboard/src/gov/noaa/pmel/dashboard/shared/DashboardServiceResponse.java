/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author kamb
 *
 */
@Builder
@AllArgsConstructor
public class DashboardServiceResponse<R> implements Serializable, IsSerializable {

    private static final long serialVersionUID = 273980474568876388L;

    private R _response;
    private String _version;

    @SuppressWarnings("unused") // For GWT
    private DashboardServiceResponse() {
    }
    public DashboardServiceResponse(R response) {
        _response = response;
    }
    
    public R response() { return _response; }
    
    public String getVersion() {
        return _version;
    }
    public void setVersion(String version) {
        _version = version;
    }

    @Builder.Default
    private boolean _wasSuccessful = true;
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
