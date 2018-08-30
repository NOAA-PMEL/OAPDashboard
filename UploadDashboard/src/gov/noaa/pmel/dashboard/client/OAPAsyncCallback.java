/**
 * 
 */
package gov.noaa.pmel.dashboard.client;


import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;

/**
 * @author kamb
 *
 */
public abstract class OAPAsyncCallback<T> implements AsyncCallback<T> {

    public OAPAsyncCallback() {
        // TODO Auto-generated constructor stub
    }
    
    /* (non-Javadoc)
     * @see com.google.gwt.user.client.rpc.AsyncCallback#onFailure(java.lang.Throwable)
     */
    @Override
    public void onFailure(Throwable error) {
        UploadDashboard.showAutoCursor();
        String exMsg = error.getMessage();
        
        if (( exMsg.indexOf("SESSION HAS EXPIRED") >= 0 ) ||
              ( error instanceof StatusCodeException &&
              ((StatusCodeException)error).getStatusCode() == 401 )) {
            UploadDashboard.showLoginMessage();
        } else {
            customFailure(error);
        }
        
//        if ( exMsg.indexOf("SESSION HAS EXPIRED") >= 0 ) {
//            UploadDashboard.showMessage(exMsg);
//        } else if ( error instanceof StatusCodeException &&
//                  ((StatusCodeException)error).getStatusCode() == 401 ) {
//            UploadDashboard.showMessage("Your session has expired.<br/><br/>Please log in again.");
//        } else {
//            customFailure(error);
////            UploadDashboard.showFailureMessage(GET_COLUMN_SPECS_FAIL_MSG, ex);
//        }
//        if ( error instanceof StatusCodeException &&
//             ((StatusCodeException)error).getStatusCode() == 401 ) {
//            Window.alert("login required");
//        } else if {
//            customFailure(error);
//        }
    }

    public void customFailure(Throwable error) {
        // default no-op implementation
    }
}
