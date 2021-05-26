/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
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

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
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
    @UiField MenuItem showHelpBtn;
    @UiField MyMenuBar preferencesMenuBar;
    @UiField MenuItem userInfoBtn;
    @UiField MenuItem changePasswordBtn;
    @UiField MenuItem logoutSeparator;
    @UiField MenuItem logoutBtn;
    
    static String _currentPage = "";
    
    boolean overMenu = false;
    
    interface ApplicationHeaderTemplateUiBinder extends UiBinder<Widget, ApplicationHeaderTemplate> {
        // nothing needed here.
    }

    private static ApplicationHeaderTemplateUiBinder uiBinder = GWT.create(ApplicationHeaderTemplateUiBinder.class);

    static JavaScriptObject helpWindow;
    
    public ApplicationHeaderTemplate() {
        initWidget(uiBinder.createAndBindUi(this));
        menuBar.setAutoOpen(false);
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
        showHelpBtn.setScheduledCommand(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                doShowHelp();
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
        UploadDashboard.logoutUser();
    }

    static void doSendFeedback() {
        GWT.log("GWT log Header sendFeedback");
        logger.info("Logger Header sendFeedback");
        UploadDashboard.showFeedbackPopup();
    }

    void doShowHelp() {
        GWT.log("GWT log Header ShowHelp");
        logger.info("Logger Header ShowHelp");
        JavaScriptObject theHelpWindow = helpWindow;
//        GWT.log("screenX:"+ screenX+", screenY:" + screenY);
//        GWT.log("btn left:"+showHelpBtn.getAbsoluteLeft() + ", width:" + showHelpBtn.getOffsetWidth());
//        GWT.log("btn top:"+showHelpBtn.getAbsoluteTop() + ", height:" + showHelpBtn.getOffsetHeight());
//        GWT.log("btn right:"+btnRight+", btn bottom:" + btnBottom);
//        GWT.log("client width: " + Window.getClientWidth() + ", height: "+ Window.getClientHeight());
//        GWT.log("help width: " + helpWidth + ", height: "+ helpHeight + ", offset: "  + offset);
//        GWT.log("help helpX: " + helpX + ", helpY: "+ helpY);
        String help_page = "sdis_help.html" + currentPageLink();
//        Window.open(help_page, "SDIS Help", features);
//        GWT.log("helpWindow:"+String.valueOf(helpWindow));
        int helpX;
        int helpY;
        int helpWidth = Window.getClientWidth() / 2;
        int helpHeight = Window.getClientHeight() / 4 * 3;
        if ( theHelpWindow != null ) {
            if ( MyWindow.isOpen(theHelpWindow) ) {
                GWT.log("theHelpWindow:"+theHelpWindow);
                helpX = MyWindow.screenX(theHelpWindow);
                helpY = MyWindow.screenY(theHelpWindow);
                if ( UploadDashboard.isFirefox()) {
                    MyWindow.close(theHelpWindow);
                }
                GWT.log("helpWin X:" + helpX + ", Y" + helpY);
            } else {
                helpX = MyWindow.lastX;
                helpY = MyWindow.lastY;
                GWT.log("LAST X:" + helpX + ", Y" + helpY);
            }
        } else {
            int screenX = MyWindow.screenX();
            int screenY = MyWindow.screenY();
            int btnRight = showHelpBtn.getAbsoluteLeft() + showHelpBtn.getOffsetWidth();
            int btnBottom = showHelpBtn.getAbsoluteTop() + showHelpBtn.getOffsetHeight();
            int offset = (2 * helpWidth / 3);
            helpX = screenX + btnRight - offset;
            helpY = screenY + btnBottom + 125;
            GWT.log("calculated X:" + helpX + ", Y" + helpY);
        }
        String features = "width="+helpWidth+",height="+helpHeight+","
                        + "screenX="+helpX+",screenY="+helpY+",resizable,scrollbars";
        helpWindow = MyWindow.open(help_page, "SDIS Help", features);
        GWT.log("helpWindow:"+helpWindow);
    }
    
    private String currentPageLink() {
        if ( ! PagesEnum.SHOW_DATASETS.name().equals(_currentPage)) {
            return "#"+_currentPage;
        }
        return "";
    }

    private static void showUserInfoPopup() {
        GWT.log("show user info popoup");
        UploadDashboard.showUserInfoPopup();
    }
    
    private static void showChangePasswordPopup() {
        GWT.log("show change password popoup");
        UploadDashboard.showChangePasswordPopup();
    }
    
    public void setPageInfo(PagesEnum page, Collection<DashboardDataset> cruises) {
        _currentPage = page.name();
        addDatasetIds(cruises);
    }
    
    /* private */ void setDatasetIds(Collection<String> datasetIds) { // XXX Use setPageInfo
        setDatasetIds(buildCruiseIdString(datasetIds));
    }
    
    /* private */ void setDatasetIds(String datasetIds) { // XXX Use setPageInfo
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
    /* private */ void addDatasetIds(DashboardDatasetList cruises) { // XXX Use setPageInfo
        String cruiseIds = extractCruiseIds(cruises);
        setDatasetIds(cruiseIds);
    }
    
    /* private */ void addDatasetIds(Collection<DashboardDataset> cruises) { // XXX Use setPageInfo
        String datasetIds = extractCruiseIds(cruises);
        setDatasetIds(datasetIds);
    }
    /* private void addDatasetIds(Collection<String> cruiseIds) { // XXX Use setPageInfo
        String datasetIds = buildCruiseIdString(cruiseIds);
        setDatasetIds(datasetIds);
    }*/ 

    private static String extractCruiseIds(DashboardDatasetList cruises) {
        return extractCruiseIds(cruises.values());
    }
    private static String extractCruiseIds(Collection<DashboardDataset> cruises) {
        List<String> names = new ArrayList<>(cruises.size()); 
        for (DashboardDataset dd : cruises) {
            String dsname = dd.getUserDatasetName();
            if ( dsname.equals(dd.getRecordId())) {
                dsname = dd.getUploadFilename();
            }
            names.add(dsname);
        }
        return buildCruiseIdString(names);
    }
    
    /**
     * @param cruises
     * @return
     */
    private static String buildCruiseIdString(Collection<String> cruises) {
        StringBuilder ids = new StringBuilder();
        String comma = "";
        for (String id : cruises) {
            ids.append(comma).append(id);
            comma = ", ";
        }
        return ids.toString();
    }
}
