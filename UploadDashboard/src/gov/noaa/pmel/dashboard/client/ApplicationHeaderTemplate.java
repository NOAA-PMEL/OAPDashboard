/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.Collection;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;

/**
 * @author kamb
 *
 */
public class ApplicationHeaderTemplate extends CompositeWithUsername {

    private static Logger logger = Logger.getLogger(ApplicationHeaderTemplate.class.getName());
    @UiField
    Label titleLabel;
    @UiField
    Anchor sendFeedbackButton;
    @UiField
    InlineLabel userInfoLabel;
    @UiField
    Button logoutButton;
    @UiField Image askChangePasswordBtn;
    @UiField FlowPanel askPasswdChangePnl;
    @UiField Button changePasswordBtn;
    
    private HandlerRegistration logoutHandler;
    
    interface ApplicationHeaderTemplateUiBinder extends UiBinder<Widget, ApplicationHeaderTemplate> {
    }

    private static ApplicationHeaderTemplateUiBinder uiBinder = GWT.create(ApplicationHeaderTemplateUiBinder.class);

    public ApplicationHeaderTemplate() {
        initWidget(uiBinder.createAndBindUi(this));
        hideChangePasswordOption();
		logoutButton.setText(LOGOUT_TEXT);
		logoutButton.setTitle(LOGOUT_TEXT);
        askChangePasswordBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent evt) {
                GWT.log("Pressed ask change password");
                showChangePasswordOption();
            }
        });
        askChangePasswordBtn.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent arg0) {
                showChangePasswordOption();
            }
        });
        askChangePasswordBtn.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent arg0) {
                delayHideChangePasswordOption();
            }
        });
        changePasswordBtn.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent arg0) {
                showChangePasswordOption();
            }
        });
        changePasswordBtn.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent arg0) {
                delayHideChangePasswordOption();
            }
        });
        changePasswordBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent evt) {
                showChangePasswordPopup();
            }
        });
		handleLogout();
    }
    private void showChangePasswordPopup() {
        GWT.log("show change password popoupa");
        hideChangePasswordOption();
        UploadDashboard.showChangePasswordPopup();
    }
    private void showChangePasswordOption() {
        GWT.log("show Style:"+askPasswdChangePnl.getStyleName());
        cancelTimer();
        askPasswdChangePnl.removeStyleName("slideOut");
        askPasswdChangePnl.addStyleName("slideIn");
    }
    private void hideChangePasswordOption() {
        GWT.log("hide Style:"+askPasswdChangePnl.getStyleName());
        askPasswdChangePnl.removeStyleName("slideIn");
        askPasswdChangePnl.addStyleName("slideOut");
    }
    private void cancelTimer() {
        GWT.log("cancelTimer");
        if ( hideTimer != null ) {
            hideTimer.cancel();
        }
    }
    Timer hideTimer = null;
    private void delayHideChangePasswordOption() {
        cancelTimer();
        hideTimer = new Timer() {
            @Override
            public void run() {
                hideChangePasswordOption();
            }
        };
        hideTimer.schedule(300);
    }

    protected void setPageTitle(String title) {
        titleLabel.setText(title);
    }
    
    static void logoutOnClick(ClickEvent event) {
        GWT.log("GWT log Header logout");
        logger.info("Logger Header logout");
        DashboardLogoutPage.showPage();
    }

    @UiHandler("sendFeedbackButton")
    void sendFeedbackButtonOnClick(ClickEvent event) {
        GWT.log("GWT log Header sendFeedback");
        logger.info("Logger Header sendFeedback");
        UploadDashboard.showFeedbackPopup();
    }

    public void setDatasetIds(String datasetIds) {
        String currentText = titleLabel.getText();
        if ( currentText.indexOf(':') > 0 ) {
            currentText = currentText.substring(0, currentText.indexOf(':') -1);
        }
        String newText = currentText + ": " + datasetIds;
        titleLabel.setText(newText);
    }

    /**
     * @param cruises
     */
    public void addDatasetIds(DashboardDatasetList cruises) {
        String cruiseIds = extractCruiseIds(cruises.keySet());
        setDatasetIds(cruiseIds);
    }
    
    public void addDatasetIds(Collection<String> cruiseIds) {
        String datasetIds = extractCruiseIds(cruiseIds);
        setDatasetIds(datasetIds);
    }

    /**
     * @param cruises
     * @return
     */
    private static String extractCruiseIds(Collection<String> cruises) {
        StringBuilder ids = new StringBuilder();
        String comma = "";
        for (String id : cruises) {
            ids.append(comma).append(id);
            comma = ", ";
        }
        return ids.toString();
    }

    public void handleLogout() {
        if ( logoutHandler != null ) {
            logoutHandler.removeHandler();
        }
        logoutHandler = logoutButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                logoutOnClick(event);
            }
        });
    }
    /**
     * @param clickHandler
     */
    public void setClickHandler(ClickHandler clickHandler) {
        logoutHandler.removeHandler();
        logoutButton.addClickHandler(clickHandler);
    }

}
