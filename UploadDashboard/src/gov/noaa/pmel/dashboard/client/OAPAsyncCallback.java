/**
 * 
 */
package gov.noaa.pmel.dashboard.client;


import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;

/**
 * @author kamb
 *
 */
public abstract class OAPAsyncCallback<T> implements AsyncCallback<T> {

    private static Logger logger = Logger.getLogger(OAPAsyncCallback.class.getName());
    
    public OAPAsyncCallback() {
        // TODO Auto-generated constructor stub
    }
    
    /* (non-Javadoc)
     * @see com.google.gwt.user.client.rpc.AsyncCallback#onFailure(java.lang.Throwable)
     */
    @Override
    public void onFailure(Throwable error) {
        UploadDashboard.showAutoCursor();
        logger.info("Async failure:"+error.toString());
        String exMsg = error.getMessage();
        logger.info("exception message:" + exMsg);
        
        if ( exMsg.indexOf("SESSION HAS EXPIRED") >= 0 ) {
        	UploadDashboard.logToConsole("SESSION EXPIRED msg");
            UploadDashboard.showLoginPopup();
        } else if ( error instanceof StatusCodeException &&
	              ((StatusCodeException)error).getStatusCode() == 401 ) {
        	UploadDashboard.logToConsole("StatusCodeException");
            UploadDashboard.showLoginPopup();
        } else {
            customFailure(error);
        }
    }

    public void customFailure(Throwable error) {
        logger.warning(String.valueOf(error));
    }
}
