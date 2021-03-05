/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;

/**
 * @author kamb
 *
 */
public class ApplicationHeaderTemplate extends Composite {

    private static Logger logger = Logger.getLogger(ApplicationHeaderTemplate.class.getName());
    
	public static final String LOGOUT_TEXT = "Logout";
	
    @UiField Label titleLabel;
    @UiField Label versionLabel;
    @UiField FlowPanel headerRightPanel;
    @UiField Label userInfoLabel;
    @UiField MenuBar menuBar;
    @UiField MenuItem sendFeedbackBtn;
    @UiField MyMenuBar preferencesMenuBar;
    @UiField MenuItem userInfoBtn;
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
        userInfoBtn.getElement().setId("userInfoBtn");
        changePasswordBtn.getElement().setId("changePasswordBtn");
        preferencesMenuBar.setParentMenu(menuBar);
		logoutBtn.setText(LOGOUT_TEXT);
		logoutBtn.setTitle(LOGOUT_TEXT);
        logoutBtn.setScheduledCommand(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                GWT.log("AHT execute logout command");
                doLogout();
            }
        });
        logoutSeparator.setEnabled(true);
        userInfoBtn.setScheduledCommand(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                showUserInfoPopup();
            }
        });
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

    public void setLogoutHandler(ScheduledCommand cmd) {
        logoutBtn.setScheduledCommand(cmd);
    }
    
    protected void setPageTitle(String title) {
        titleLabel.setText(title);
    }
    
    void doLogout() {
        logger.info("Logger Header logout");
        UploadDashboard.closePopups();
        try {
        UploadDashboard.getService().logoutUser(new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void nada) {
                UploadDashboard.logToConsole("Logout success");
                Cookies.removeCookie("JSESSIONID");
                UploadDashboard.stopHistoryHandling();
                UploadDashboard.showAutoCursor();
                Window.Location.assign("dashboardlogout.html");
            }
            @Override
            public void onFailure(Throwable ex) {
                UploadDashboard.logToConsole("Logout error:" + ex.toString());
//                Window.alert(String.valueOf(ex));
                Cookies.removeCookie("JSESSIONID");
                UploadDashboard.stopHistoryHandling();
                UploadDashboard.showAutoCursor();
                Window.Location.assign("dashboardlogout.html");
            }
        });
        } catch (Throwable t) {
            UploadDashboard.logToConsole("logout exception: "+ String.valueOf(t));
//            Window.alert(String.valueOf(t));
            Window.Location.assign("dashboardlogout.html");
        }
    }

    static void doSendFeedback() {
        GWT.log("GWT log Header sendFeedback");
        logger.info("Logger Header sendFeedback");
        UploadDashboard.showFeedbackPopup();
    }

    private static void showUserInfoPopup() {
        GWT.log("show user info popoup");
        UploadDashboard.showUserInfoPopup();
    }
    
    private static void showChangePasswordPopup() {
        GWT.log("show change password popoup");
        UploadDashboard.showChangePasswordPopup();
    }
    
    public void setDatasetIds(String datasetIds) {
        String currentText = titleLabel.getText();
        if ( currentText.indexOf(':') > 0 ) {
            currentText = currentText.substring(0, currentText.indexOf(':'));
        }
        String newText = currentText + ": " + datasetIds;
        titleLabel.setText(newText);
    }

    /**
     * @param cruises
     */
    public void addDatasetIds(DashboardDatasetList cruises) {
        String cruiseIds = extractCruiseIds(cruises);
        setDatasetIds(cruiseIds);
    }
    
    public void addDatasetIds(Collection<String> cruiseIds) {
        String datasetIds = extractCruiseIds(cruiseIds);
        setDatasetIds(datasetIds);
    }

    private static String extractCruiseIds(DashboardDatasetList cruises) {
        List<String> names = new ArrayList<>(cruises.values().size()); 
        for (DashboardDataset dd : cruises.values()) {
            String dsname = dd.getUserDatasetName();
            if ( dsname.equals(dd.getRecordId())) {
                dsname = dd.getUploadFilename();
            }
            names.add(dsname);
        }
        return extractCruiseIds(names);
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
