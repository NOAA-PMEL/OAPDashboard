/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * An message embedded within a PopupPanel.
 * 
 * @author Karl Smith
 */
public class FileDataPreviewPopup extends Composite {

	private static final String BUTTON_TEXT = "Submit";

	interface DashboardLoginPopupUiBinder extends UiBinder<Widget, FileDataPreviewPopup> {
	}

	private static DashboardLoginPopupUiBinder uiBinder = 
			GWT.create(DashboardLoginPopupUiBinder.class);

    @UiField HTML html;
    @UiField Button closeButton;
    
	private PopupPanel parentPanel;

	/**
	 * Creates an empty message within a PopupPanel.
	 * The popup includes a dismiss button to hide it.  
	 * Use {@link #setLoginMessage(String)} to assign 
	 * the message to be displayed.  
	 * Use {@link #showAtPosition(int, int)} 
	 * or {@link #showInCenterOf(UIObject)} 
	 * to show the popup.
	 */
	FileDataPreviewPopup() {
		initWidget(uiBinder.createAndBindUi(this));
		parentPanel = new PopupPanel(false);
		parentPanel.setWidget(this);
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

    public void setCloseButtonText(String text) {
        closeButton.setText(text);
    }
    
    public void setMessage(String msg) {
        html.setHTML(msg);
    }
    
    public void dismiss() {
        parentPanel.hide();
    }
    
    @UiHandler("closeButton")
    void closeButtonOnClick(ClickEvent e) {
        parentPanel.hide();
    }
}
