/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
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
    @UiField PasswordTextBox currentPasswordBox;
    @UiField PasswordTextBox newPasswordBox;
    @UiField PasswordTextBox confirmPasswordBox;
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
		currentPasswordBox.setFocus(true);
		parentPanel.center();
	}
    
	private void setPrologue(String username) {
	    prologue.setText("Changing password for user: " + username);
	}

	@UiHandler("yesButton")
	void yesOnClick(ClickEvent e) {
        if ( ! newPasswordBox.getText().equals(confirmPasswordBox.getText())) {
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
        newPasswordBox.setText("");
        confirmPasswordBox.setText("");
    }

    @UiHandler("noButton")
	void noOnClick(ClickEvent e) {
		sendIt = false;
		parentPanel.hide();
	}

    public void reset() {
        currentPasswordBox.setText("");
        newPasswordBox.setText("");
        confirmPasswordBox.setText("");
    }
    public String getCurrentPassword() {
        return currentPasswordBox.getText();
    }
    public String getNewPassword() {
        return newPasswordBox.getText();
    }
}
