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
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * An question embedded within a PopupPanel.
 * 
 * @author Karl Smith
 */
public class DashboardFeedbackPopup extends Composite {

	interface DashboardFeedbackPopupUiBinder extends UiBinder<Widget, DashboardFeedbackPopup> {
	}

	private static DashboardFeedbackPopupUiBinder uiBinder = 
			GWT.create(DashboardFeedbackPopupUiBinder.class);

    @UiField ListBox feedbackChoiceBox;
    @UiField TextArea feedbackTextBox;
	@UiField Button yesButton;
	@UiField Button noButton;

	private PopupPanel parentPanel;
	Boolean sendIt;
	HandlerRegistration handler;

    static enum FEEDBACK_TYPE {
        BUG("Bug Report"),
        FEATURE("Feature Request"),
        QUESTION("Question"),
        COMMENT("General Comment");
        
        private String text;
        private FEEDBACK_TYPE(String displayText) {
            text = displayText;
        }
        @Override 
        public String toString() { return text; }
    }
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
	public DashboardFeedbackPopup(final AsyncCallback<Boolean> callback) {
		initWidget(uiBinder.createAndBindUi(this));

		parentPanel = new PopupPanel(false, true);
		parentPanel.setWidget(this);

        feedbackChoiceBox.addItem("-- Please Select Feedback Category --");
        for (FEEDBACK_TYPE type : FEEDBACK_TYPE.values()) {
            feedbackChoiceBox.addItem(type.text, type.name());
        }
        feedbackChoiceBox.getElement().<SelectElement>cast().getOptions().getItem(0).setDisabled(true); // Please select...
		yesButton.setText("Send");
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

	void show() {
		feedbackChoiceBox.setFocus(true);
		parentPanel.center();
	}

	@UiHandler("yesButton")
	void yesOnClick(ClickEvent e) {
        if ( feedbackChoiceBox.getSelectedIndex() == 0 ) {
            Window.alert("Please choose a feedback type.");
            return;
        }
		sendIt = true;
		parentPanel.hide();
	}

	@UiHandler("noButton")
	void noOnClick(ClickEvent e) {
		sendIt = false;
		parentPanel.hide();
	}

    public void reset() {
        feedbackChoiceBox.setSelectedIndex(0);
        feedbackTextBox.setText("");
    }
    public String getMessage() {
        return feedbackTextBox.getText();
    }
    public String getFeedbackType() {
        return feedbackChoiceBox.getSelectedValue();
    }
}
