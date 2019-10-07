/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

import lombok.Builder;
import lombok.Value;

/**
 * @author kamb
 *
 */
@Builder
public class DashboardServiceResponse implements Serializable, IsSerializable {

    private static final long serialVersionUID = 273980474568876388L;

    private boolean _wasSuccessful;
    public boolean wasSuccessful() {
        return _wasSuccessful;
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
