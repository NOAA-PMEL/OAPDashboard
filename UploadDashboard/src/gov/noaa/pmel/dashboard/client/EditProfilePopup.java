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
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.shared.UserInfo;

/**
 * An question embedded within a PopupPanel.
 * 
 * @author Linus Kamb
 */
public class EditProfilePopup extends Composite {

	interface EDitProfilePupupUiBinder extends UiBinder<Widget, EditProfilePopup> {
	}

	private static EDitProfilePupupUiBinder uiBinder = 
			GWT.create(EDitProfilePupupUiBinder.class);

    @UiField Label prologue;
    @UiField Label fnLbl;
    @UiField Label mnLbl;
    @UiField Label lnLbl;
    @UiField Label emailLbl;
    @UiField Label telLbl;
    @UiField Label extLbl;
    @UiField Label orgLbl;
    @UiField TextBox fnBox;
    @UiField TextBox mnBox;
    @UiField TextBox lnBox;
    @UiField TextBox emailBox;
    @UiField TextBox telBox;
    @UiField TextBox extBox;
    @UiField TextBox orgBox;
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
	public EditProfilePopup(final AsyncCallback<Boolean> callback) {
		initWidget(uiBinder.createAndBindUi(this));

//        FocusHandler fh = new FocusHandler() {
//            @Override
//            public void onFocus(FocusEvent focusEvent) {
//                ((TextBox)focusEvent.getSource()).setReadOnly(false);
//            }
//        };
//        cpBx.addFocusHandler(fh);
		parentPanel = new PopupPanel(false, true);
		parentPanel.setWidget(this);

		yesButton.setText("Update");
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

	void show(UserInfo userInfo) {
        setPrologue(userInfo.username());
        fnBox.setText(userInfo.firstName());
        mnBox.setText(userInfo.middle());
        lnBox.setText(userInfo.lastName());
        emailBox.setText(userInfo.email());
        telBox.setText(userInfo.telephone());
        extBox.setText(userInfo.telExtension());
        orgBox.setText(userInfo.organization());
//        Timer t = new Timer() {
//            @Override
//            public void run() {
//        		cpBx.setFocus(true);
//            }
//        };
//        t.schedule(500);
		parentPanel.center();
	}
    
	private void setPrologue(String username) {
	    prologue.setText("Update profile for user: " + username);
	}

	@UiHandler("yesButton")
	void yesOnClick(ClickEvent e) {
		sendIt = true;
		parentPanel.hide();
	}

    @UiHandler("noButton")
	void noOnClick(ClickEvent e) {
		sendIt = false;
		parentPanel.hide();
	}

    public void reset() {
        fnBox.setText("");
        mnBox.setText("");
        lnBox.setText("");
        emailBox.setText("");
        telBox.setText("");
        extBox.setText("");
        orgBox.setText("");
    }
    
    public String getFirstName() { return fnBox.getText().trim(); }
    public String getMiddle() { return mnBox.getText().trim(); }
    public String getLastName() { return lnBox.getText().trim(); }
    public String getEmail() { return emailBox.getText().trim(); }
    public String getTelephone() { return telBox.getText().trim(); }
    public String getExtension() { return extBox.getText().trim(); }
    public String getOrganization() { return orgBox.getText().trim(); }
}
