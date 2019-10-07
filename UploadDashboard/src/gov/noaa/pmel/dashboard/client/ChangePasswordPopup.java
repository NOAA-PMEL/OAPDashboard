/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * An question embedded within a PopupPanel.
 * 
 * @author Karl Smith
 */
public class ChangePasswordPopup extends Composite {

	interface ChangePasswordPopupUiBinder extends UiBinder<Widget, ChangePasswordPopup> {
	}

	private static ChangePasswordPopupUiBinder uiBinder = 
			GWT.create(ChangePasswordPopupUiBinder.class);

    @UiField Label prologue;
    // attempts to defeat password auto-fill...
    @UiField Label cpLbl;
    @UiField Label npLbl;
    @UiField Label cnfLbl;
    @UiField PasswordTextBox cpBx;
    @UiField PasswordTextBox newPdBox;
    @UiField PasswordTextBox confirmPdBox;
	@UiField Button yesButton;
	@UiField Button noButton;

	private PopupPanel parentPanel;
	Boolean sendIt;
	HandlerRegistration handler;

	/**
	 * Widget for asking a question in a PopupPanel 
	 * that is modal and does not auto-hide.
	 * 
	 * @param callback
	 * 		calls the onSuccess method of this callback with the answer: 
	 * 		true for the yes button, false for the no button, or null if 
	 * 		the window was (somehow) closed without pressing either the
	 * 		yes or no button.  The onFailure method of this callback is
	 * 		never called.
	 */
	public ChangePasswordPopup(final AsyncCallback<Boolean> callback) {
		initWidget(uiBinder.createAndBindUi(this));

        cpBx.setReadOnly(true);
        newPdBox.setReadOnly(true);
        confirmPdBox.setReadOnly(true);
        FocusHandler fh = new FocusHandler() {
            @Override
            public void onFocus(FocusEvent focusEvent) {
                ((TextBox)focusEvent.getSource()).setReadOnly(false);
            }
        };
        cpBx.addFocusHandler(fh);
        newPdBox.addFocusHandler(fh);
        confirmPdBox.addFocusHandler(fh);
		parentPanel = new PopupPanel(false, true);
		parentPanel.setWidget(this);

		yesButton.setText("Change Password");
		noButton.setText("Cancel");
		sendIt = null;

		// Handler to make the callback on window closing 
		handler = parentPanel.addCloseHandler(
			new CloseHandler<PopupPanel>() {
    			@Override
    			public void onClose(CloseEvent<PopupPanel> event) {
    				// Make the appropriate call
    				callback.onSuccess(sendIt);
    			}
		});
	}

	void show(String username) {
        setPrologue(username);
        Timer t = new Timer() {
            @Override
            public void run() {
        		cpBx.setFocus(true);
            }
        };
        t.schedule(500);
		parentPanel.center();
	}
    
	private void setPrologue(String username) {
	    prologue.setText("Changing password for user: " + username);
	}

	@UiHandler("yesButton")
	void yesOnClick(ClickEvent e) {
        if ( ! newPdBox.getText().equals(confirmPdBox.getText())) {
            Window.alert("Passwords do not match!");
            clearNewPasswords();
            return;
        }
		sendIt = true;
		parentPanel.hide();
	}

	/**
     * 
     */
    private void clearNewPasswords() {
        newPdBox.setText("");
        confirmPdBox.setText("");
    }

    @UiHandler("noButton")
	void noOnClick(ClickEvent e) {
		sendIt = false;
		parentPanel.hide();
	}

    public void reset() {
        cpBx.setText("");
        newPdBox.setText("");
        confirmPdBox.setText("");
    }
    public String getCurrentPassword() {
        return cpBx.getText();
    }
    public String getNewPassword() {
        return newPdBox.getText();
    }
}
