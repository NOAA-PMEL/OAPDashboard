/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.Collection;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;

/**
 * @author kamb
 *
 */
public class ApplicationHeaderTemplate extends CompositeWithUsername {

    private static Logger logger = Logger.getLogger(ApplicationHeaderTemplate.class.getName());
    @UiField Label titleLabel;
    @UiField FlowPanel headerRightPanel;
    @UiField Label userInfoLabel;
    @UiField MenuBar menuBar;
    @UiField MenuItem sendFeedbackBtn;
    @UiField MenuItem changePasswordBtn;
    @UiField MenuItem logoutSeparator;
    @UiField MenuItem logoutBtn;
    boolean overMenu = false;
    
    interface ApplicationHeaderTemplateUiBinder extends UiBinder<Widget, ApplicationHeaderTemplate> {
        // nothing needed here.
    }

    private static ApplicationHeaderTemplateUiBinder uiBinder = GWT.create(ApplicationHeaderTemplateUiBinder.class);

    public ApplicationHeaderTemplate() {
        initWidget(uiBinder.createAndBindUi(this));
        menuBar.setAutoOpen(true);
		logoutBtn.setText(LOGOUT_TEXT);
		logoutBtn.setTitle(LOGOUT_TEXT);
        logoutBtn.setScheduledCommand(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                doLogout();
            }
        });
        logoutSeparator.setEnabled(false);
        changePasswordBtn.setScheduledCommand(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                showChangePasswordPopup();
            }
        });
        sendFeedbackBtn.setScheduledCommand(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                doSendFeedback();
            }
        });
    }

    protected void setPageTitle(String title) {
        titleLabel.setText(title);
    }
    
    static void doLogout() {
        GWT.log("GWT log Header logout");
        logger.info("Logger Header logout");
        DashboardLogoutPage.showPage();
    }

    static void doSendFeedback() {
        GWT.log("GWT log Header sendFeedback");
        logger.info("Logger Header sendFeedback");
        UploadDashboard.showFeedbackPopup();
    }

    private static void showChangePasswordPopup() {
        GWT.log("show change password popoupa");
        UploadDashboard.showChangePasswordPopup();
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
}
