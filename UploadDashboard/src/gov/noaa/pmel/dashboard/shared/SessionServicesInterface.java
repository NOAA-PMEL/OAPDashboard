/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * @author kamb
 *
 */
@RemoteServiceRelativePath("SessionServices")
public interface SessionServicesInterface extends RemoteService {

    void ping(String sessionId);
}
