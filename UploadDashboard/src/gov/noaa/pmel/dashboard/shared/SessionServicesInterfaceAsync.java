/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author kamb
 *
 */
public interface SessionServicesInterfaceAsync {

    void ping(String sessionId, AsyncCallback<Void> callback);
}
