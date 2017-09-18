/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;

/**
 * @author kamb
 *
 */
public abstract class SessionHandlingCallbackBase<T> implements AsyncCallback<T> {

	protected final String SESSION_EXPIRED_INDICATOR = "SESSION HAS EXPIRED";
	
	@Override
	public void onFailure(Throwable caught) {
		String exMsg = caught.getMessage();
		if ( caught instanceof InvocationException && exMsg.indexOf(SESSION_EXPIRED_INDICATOR) >= 0) {
			UploadDashboard.showMessage("Your session has expired.<br/><br/>Please log in again.");
		} else {
			handleFailure(caught);
		}

	}

	protected abstract void handleFailure(Throwable caught);
}
