/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * An message embedded within a PopupPanel.
 * 
 * @author Karl Smith
 */
public class NotificationPopup extends Composite {

	private static final String DISMISS_TEXT = "Dismiss";

	interface DashboardInfoPopupUiBinder extends UiBinder<Widget, NotificationPopup> {
	}

	private static DashboardInfoPopupUiBinder uiBinder = 
			GWT.create(DashboardInfoPopupUiBinder.class);

    @UiField Image msgTypeIcon;
	@UiField HTML infoHtml;
	@UiField Button dismissButton;

    private InfoMsgType msgType;
    
    private OAPAsyncCallback<?> continuation;
    
	private PopupPanel parentPanel;

	/**
	 * Creates an empty message within a PopupPanel.
	 * The popup includes a dismiss button to hide it.  
	 * Use {@link #setInfoMessage(String)} to assign 
	 * the message to be displayed.  
	 * Use {@link #showAtPosition(int, int)} 
	 * or {@link #showInCenterOf(UIObject)} 
	 * to show the popup.
	 */
	NotificationPopup(InfoMsgType type) {
		initWidget(uiBinder.createAndBindUi(this));
        msgType = type;
		dismissButton.setText(DISMISS_TEXT);
		parentPanel = new PopupPanel(false, true);
		parentPanel.setWidget(this);
        msgTypeIcon.setUrl(type.iconSrc());
        msgTypeIcon.addStyleName(type.iconStyleClass());
	}

	NotificationPopup(String htmlMsg, InfoMsgType type) {
        this(htmlMsg, type, DISMISS_TEXT, null);
	}
    
	NotificationPopup(String htmlMsg, InfoMsgType type, OAPAsyncCallback<?> continuation) {
        this(htmlMsg, type, DISMISS_TEXT, continuation);
	}
    
	NotificationPopup(String htmlMsg, InfoMsgType type, String dismissBtnText, OAPAsyncCallback<?> continuation) {
        this(type);
        this.setInfoMessage(htmlMsg);
        this.dismissButton.setText(dismissBtnText);
        this.continuation = continuation;
	}
	/**
	 * @param htmlMessage
	 * 		the unchecked HTML message to display.
	 * 		For safety, use only known (static) HTML.
	 */
	void setInfoMessage(String htmlMessage) {
		infoHtml.setHTML(htmlMessage);
	}
    
    InfoMsgType msgType() {
        return msgType;
    }
	

	/**
	 * Show the popup relative to the given object.
	 * See {@link PopupPanel#showRelativeTo(UIObject)}.
	 * 
	 * @param obj
	 * 		show relative to this UI object
	 */
	void showRelativeTo(UIObject obj) {
		parentPanel.showRelativeTo(obj);
	}

	/**
	 * Show the popup centered in the browser window.
	 */
	void showCentered() {
		parentPanel.center();
	}

	@UiHandler("dismissButton")
	void onClick(ClickEvent e) {
		parentPanel.hide();
        if ( continuation != null ) {
            continuation.onSuccess(null);
        }
	}

    public void dismiss() {
		parentPanel.hide();
	}
}
