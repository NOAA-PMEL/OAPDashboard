package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.logging.client.ConsoleLogHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;

import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardServiceResponse;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.PreviewPlotImage;
import gov.noaa.pmel.dashboard.shared.SessionServicesInterface;
import gov.noaa.pmel.dashboard.shared.SessionServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.UserInfo;

public class UploadDashboard implements EntryPoint, ValueChangeHandler<String> {

    private static Logger logger = Logger.getLogger("UploadDashboard");
    static {
        logger.addHandler(new ConsoleLogHandler());
        logger.setLevel(Level.ALL);
    }
	private static DashboardServicesInterfaceAsync service = 
			GWT.create(DashboardServicesInterface.class);
    private static SessionServicesInterfaceAsync sesh =
            GWT.create(SessionServicesInterface.class);
    
	static final boolean DO_PING = true;
    
	/**
	 * Enumerated type to specify pages for browser history.
	 */
	public enum PagesEnum {
		/** History tag for DatasetListPage */
		SHOW_DATASETS,
		/** History tag for DataUploadPage */
		UPLOAD_DATA,
		/** History tag for DataColumnSpecsPage */
		IDENTIFY_COLUMNS,
		/** History tag for DataMessagesPage */
		SHOW_DATA_MESSAGES,
		/** History tag for MetadataManagerPage */
		EDIT_METADATA,
		/** History tag for AddlDocsManagerPage */
		MANAGE_DOCUMENTS,
		/** History tag for DatasetPreviewPage */
		PREVIEW_DATASET,
		/** History tag for SubmitForQCPage */
		SUBMIT_FOR_QC,
		/** History tag for SubmitToArchivePage */
		SUBMIT_TO_ARCHIVE
	}

	// Column widths in em's
	static final double CHECKBOX_COLUMN_WIDTH = 2; //.95;
	static final double NARROWER_COLUMN_WIDTH = 5;
	static final double NARROW_COLUMN_WIDTH = 6.;
	static final double MIDDLING_COLUMN_WIDTH = 7.;
	static final double SELECT_COLUMN_WIDTH = CHECKBOX_COLUMN_WIDTH; // 6.2;
	static final double NORMAL_COLUMN_WIDTH = 10.;
	static final double FILENAME_COLUMN_WIDTH = 16.0;

	// Data background colors
    // lightish yellow: #fffed4
	// lightish red: #ff6d70
	static final String CHECKER_LIGHT_WARNING_COLOR = "#FFFBD6";
	static final String CHECKER_WARNING_COLOR = "#FFDD54"; // "#FFCC33";
	static final String CHECKER_ERROR_COLOR = "#FF6666";
	static final String CHECKER_LIGHT_ERROR_COLOR = "#FFB8AF";
	static final String USER_WARNING_COLOR = "#FFEE99";
	static final String USER_ERROR_COLOR = "#FFCCCC";
	
	static final String CHECKER_WARNING_CELL_COLOR = "#FEDD53"; // "#FFFED4";
	static final String CHECKER_ERROR_CELL_COLOR = "#FF6D70";
	static final String CHECKER_CRITICAL_CELL_COLOR = CHECKER_ERROR_COLOR;

	// Color for row numbers
	static final String ROW_NUMBER_COLOR = "#666666";
	private static final String UPLOAD_DASHBOARD_SERVER_NAME = "OAPUploadDashboard";

    private static final String IE_NOT_SUPPORTED = 
            "Internet Explorer is not supported.<br/>"
          + "We apologize for any invconvenience.<br/>"
          + "Please use another browser."
          + "<br/><br/>Please see the <a href='sdis_help.html#SUPPORTED_BROWSERS'>Help Documentation</a>"
          + " for supported browsers.";
    
	// Singleton instance of this object
	private static UploadDashboard singleton = null;
    private static UploadDashboard getSingleton() {
        if ( singleton == null ) {
            singleton = new UploadDashboard();
        }
        return singleton;
    }

	// History change handler registration
	private HandlerRegistration historyHandlerReg = null;

	// Keep a record of the currently displayed page
	private CompositeWithUsername currentPage;

	// PopupPanel for displaying messages 
	private DashboardInfoPopup infoMsgPopup;
    
    // popup used when a continuation (follow-on action) is required.
	private DashboardInfoPopup continueToPopup;
    
	private DashboardBlankPagePopup blankMsgPopup;
	private FileDataPreviewPopup dataPreviewPopup;
    private DataUpdatePopup dataUpdatePopup;

    private DashboardFeedbackPopup feedbackPopup;
    private ChangePasswordPopup changePasswordPopup;
    private EditProfilePopup userInfoPopup;
    
    private static String buildVersion = "n/a";
    private static String maxUploadSizeStr = "n/a";
    private static long maxUploadSize = -1;
    
	/**
	 * Create the manager for the UploadDashboard pages.
	 * Do not use this constructor; instead use the static
	 * methods provided to display pages and messages.
	 */
	UploadDashboard() {
		// Just in case this gets called more than once, 
		// remove any recorded page in the previous instantiation
		if ( (singleton != null) && (singleton.currentPage != null) ) {
			RootLayoutPanel.get().remove(singleton.currentPage);
			singleton.currentPage = null;
		}
        
		currentPage = null;
		infoMsgPopup = null;
		// Make sure singleton is assign to this instance since 
		// this constructor is probably called from GWT.
		singleton = this;
	}

    private static boolean DEBUG = true;
    public static void debugLog(String msg) {
        if ( DEBUG ) {
            logToConsole(msg);
        }
    }
    public static native void hardReload() /*-{
	    $wnd.location.reload(true);
	  }-*/;
    static native void logToConsole(String msg) /*-{
        console.log(msg);
    }-*/;

 
	/**
	 * Shows the message in a popup panel centered on the page.
	 * 
	 * @param htmlMsg
	 * 		unchecked HTML message to show.
	 */
	public static void showMessage(String htmlMsg) {
        getSingleton();
		if ( singleton.infoMsgPopup == null )
			singleton.infoMsgPopup = new DashboardInfoPopup();
		singleton.infoMsgPopup.setInfoMessage(htmlMsg);
		singleton.infoMsgPopup.showCentered();
	}

    public static void showNotification(String htmlMsg, InfoMsgType msgType) {
        getSingleton();
        NotificationPopup notificationMsgPopup = new NotificationPopup(htmlMsg, msgType);
        notificationMsgPopup.showCentered();
    }
    
    public static void showNotification(String htmlMsg, InfoMsgType msgType, OAPAsyncCallback<?> continuation) {
        getSingleton();
        NotificationPopup notificationMsgPopup = new NotificationPopup(htmlMsg, msgType, continuation);
        notificationMsgPopup.setInfoMessage(htmlMsg);
        notificationMsgPopup.showCentered();
    }
    
    public static void showNotification(String htmlMsg, InfoMsgType msgType, 
                                        String dismissBtnText, OAPAsyncCallback<?> continuation) {
        getSingleton();
        NotificationPopup notificationMsgPopup = new NotificationPopup(htmlMsg, msgType, dismissBtnText, continuation);
        notificationMsgPopup.setInfoMessage(htmlMsg);
        notificationMsgPopup.showCentered();
    }
    
	/**
	 * Shows a message in a popup panel centered on the page and executes the continuation on dismissal.
	 * 
	 * @param htmlMsg
	 * 		unchecked HTML message to show.
	 */
	public static void showMessageWithContinuation(String htmlMsg, OAPAsyncCallback<?> continuation) {
        if ( getSingleton().continueToPopup != null && getSingleton().continueToPopup.isVisible() ) {
            getSingleton().continueToPopup.setVisible(false);
        }
        getSingleton().continueToPopup = new DashboardInfoPopup(htmlMsg, continuation);
		getSingleton().continueToPopup.showCentered();
	}

    public static void theresAproblem(String msg, String yesText, String noText, AsyncCallback<Boolean> callback) {
        theresAproblem(InfoMsgType.WARNING, msg, yesText, noText, callback);
    }
    public static void theresAproblem(InfoMsgType type, String msg, String yesText, String noText, AsyncCallback<Boolean> callback) {
        DashboardAskPopup dap = new DashboardAskPopup(yesText, noText, type, callback);
        dap.askQuestion(msg);
    }
    
	private static Map<CompositeWithUsername, List<WindowBox>> pagePopups = new HashMap<>();
	private static void addPagePopup(CompositeWithUsername page, WindowBox popup) {
		if ( !pagePopups.containsKey(page)) {
			pagePopups.put(page, new ArrayList<WindowBox>());
		}
		pagePopups.get(page).add(popup);
	}
    public static DashboardServicesInterfaceAsync getService() {
        return service;
    }
    public static void logoutUser() {
        logoutUser("dashboardlogout.html");
    }
    public static void logoutUser(String redirectTo) {
        logger.info("UploadDashboard logoutUser");
        UploadDashboard.closePopups();
        try {
        UploadDashboard.getService().logoutUser(new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void nada) {
                UploadDashboard.logToConsole("Logout success");
                Cookies.removeCookie("JSESSIONID");
                UploadDashboard.stopHistoryHandling();
                UploadDashboard.showAutoCursor();
                Window.Location.assign(redirectTo);
            }
            @Override
            public void onFailure(Throwable ex) {
                UploadDashboard.logToConsole("Logout error:" + ex.toString());
//                Window.alert(String.valueOf(ex));
                Cookies.removeCookie("JSESSIONID");
                UploadDashboard.stopHistoryHandling();
                UploadDashboard.showAutoCursor();
                Window.Location.assign(redirectTo);
            }
        });
        } catch (Throwable t) {
            UploadDashboard.logToConsole("logout exception: "+ String.valueOf(t));
//            Window.alert(String.valueOf(t));
            Window.Location.assign("dashboardlogout.html");
        }
    }

    public static void closePopups() {
        closePreviews();
        if ( singleton.infoMsgPopup != null && singleton.infoMsgPopup.isVisible()) {
            singleton.infoMsgPopup.dismiss();
        }
        if ( singleton.blankMsgPopup != null && singleton.blankMsgPopup.isVisible()) {
            singleton.blankMsgPopup.dismiss();
        }
        if ( singleton.continueToPopup != null ) {
            singleton.continueToPopup.dismiss();
            singleton.continueToPopup = null;
        }
    }

    private static void closePreviews() {
		for (List<WindowBox> popups : pagePopups.values()) {
    		for(WindowBox popup : popups) {
    			if ( popup.isVisible()) {
    				popup.hide();
    			}
    		}
    		popups.clear();
		}
        pagePopups.clear();
    }

    public static void closePreviews(CompositeWithUsername page) {
		List<WindowBox> popups = pagePopups.get(page);
		if ( popups == null ) { return; }
		for(WindowBox popup : popups) {
			if ( popup.isVisible()) {
				popup.hide();
			}
		}
		popups.clear();
	}
	public static void showPreviewImage(final CompositeWithUsername page, PreviewPlotImage imgInfo, String imgUrl) {
		final WindowBox dd = new WindowBox(true,  true);
		dd.addStyleName("popupPreviewDialogBox");
		dd.setText(imgInfo.imageTitle);
		HTML html = new HTML(buildImagePopupHtml(imgUrl));
		VerticalPanel mainPanel = new VerticalPanel();
		mainPanel.add(html);
		mainPanel.addStyleName("popupDialogPanel");
		Button dismissButton = new Button("DISMISS");
		dismissButton.addStyleName("popupDialogButton");
		dismissButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				dd.hide();
			}
		});
		mainPanel.add(dismissButton);
		dd.setWidget(mainPanel);
		dd.addDomHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				putThisOnTop(page, dd);
			}

		}, ClickEvent.getType());
		addPagePopup(page, dd);
		putThisOnTop(page, dd);
		dd.center();
	}

	private static void putThisOnTop(CompositeWithUsername page, WindowBox dd) {
		List<WindowBox>popups = pagePopups.get(page);
		for (WindowBox popup : popups) {
			Style pstyle = popup.getElement().getStyle();
			if ( popup == dd ) {
				pstyle.setZIndex(2);
			} else {
				pstyle.setZIndex(1);
			}
		}
	}
			
	private static String buildImagePopupHtml(String iSrc) {
		return "<img src=\""+iSrc+"\" class=\"popupPreviewImage\"/>";
	}
	
	/**
	 * Shows the message in a popup panel relative to the given UI obect. 
	 * See {@link PopupPanel#showRelativeTo(UIObject)}. 
	 * 
	 * @param htmlMsg
	 * 		unchecked HTML message to show.
	 * @param obj
	 * 		show the message relative to this object
	 * 		(usually underneath, left-aligned)
	 */
	public static void showMessageAt(String htmlMsg, UIObject obj) {
        getSingleton();
		if ( singleton.infoMsgPopup == null )
			singleton.infoMsgPopup = new DashboardInfoPopup();
		singleton.infoMsgPopup.setInfoMessage(htmlMsg);
		singleton.infoMsgPopup.showRelativeTo(obj);
	}

	/**
	 * Shows an error message, along with the message from an
	 * exception, in a popup panel centered on the page.
	 * 
	 * @param htmlMsg
	 * 		unchecked HTML message to show before the exception message
	 * @param ex
	 * 		exception whose message is to be shown
	 */
	public static void showFailureMessage(String htmlMsg, Throwable ex) {
		String exceptMsg = ex != null ? ex.getMessage() : null;
		if ( exceptMsg == null )
			exceptMsg = htmlMsg;
		else
			exceptMsg = htmlMsg + "<br /><pre>" + 
					SafeHtmlUtils.htmlEscape(exceptMsg) + "</pre>";
		UploadDashboard.showMessage(exceptMsg);
	}

    public static boolean isFirefox() {
        String browser = Window.Navigator.getUserAgent();
        GWT.log("browser "+ browser);
        return browser.toLowerCase().contains("firefox");
    }
    public static boolean isSafari() {
        String browser = Window.Navigator.getUserAgent();
        browser = browser.toLowerCase();
        GWT.log("browser "+ browser);
        return browser.contains("safari") && ! browser.contains("chrome"); // Or contains Windows...Not changing now.
    }
    public static boolean isIE() {
        String browser = Window.Navigator.getUserAgent();
        GWT.log("browser "+ browser);
        browser = browser.toLowerCase();
        return browser.contains("msie") || browser.contains("trident");
    }
    public static boolean needsHistoryForcing() {
        return isFirefox() || isSafari();
    }
	/**
	 * Updates the displayed page by removing any page 
	 * currently being shown and adding the given page.
	 * 
	 * @param newPage
	 * 		new page to be shown; if null, not page is shown
	 */
	public static void updateCurrentPage(CompositeWithUsername newPage) {
        updateCurrentPage(newPage, false);
	}
    public static void pingService(OAPAsyncCallback<Void> callback) {
        for (String cookie : Cookies.getCookieNames()) {
            GWT.log("cookie: " + cookie);
        }
        String sessionId = Cookies.getCookie("JSESSIONID");
        GWT.log("JSESSIONID:" + sessionId);
        sesh.ping(sessionId, callback);
    }
	public static void _updateCurrentPage(CompositeWithUsername newPage) {
        logToConsole("_update to: " + newPage.pageName());
        getSingleton();
        closePopups();
        if ( singleton.currentPage != null ) {
            RootLayoutPanel.get().remove(singleton.currentPage);
        }
        singleton.currentPage = newPage;
        if ( singleton.currentPage != null ) {
            GWT.log("Setting page to " + newPage.pageName());
            singleton.currentPage.showing();
            RootLayoutPanel.get().add(singleton.currentPage);
            newPage.setBuildVersion(buildVersion);
            ApplicationHeaderTemplate._currentPage = newPage.pageName();
            if ( needsHistory(newPage.pageName())) {
                GWT.log("Adding new history: " + newPage.pageName());
        		History.newItem(newPage.pageName(), false);
            }
            Window.setTitle("SDIS " + newPage.pageName());
        } else {
            logToConsole("Null current page!");
        }
	}
	/**
     * @param pageName
     * @return
     */
    private static boolean needsHistory(String pageName) {
        return  ! (
                 PagesEnum.SHOW_DATASETS.name().equals(pageName) ||
                 PagesEnum.UPLOAD_DATA.name().equals(pageName) ||
//                 PagesEnum.EDIT_METADATA.name().equals(pageName) ||
                 PagesEnum.MANAGE_DOCUMENTS.name().equals(pageName) 
                );
    }
    public static void updateCurrentPage(CompositeWithUsername newPage, boolean doPing) {
        GWT.log("update ping " + doPing + " to " + newPage.pageName());
        if ( doPing ) {
            pingService(new OAPAsyncCallback<Void>() {
                @Override
                public void onSuccess(Void arg0) {
                    _updateCurrentPage(newPage);
                }
            });
        } else {
            _updateCurrentPage(newPage);
        }
	}
	
//	public static void updateCurrentPage(String username, CompositeWithUsername newPage) {
//	    newPage.setUsername(username);
//	    updateCurrentPage(newPage);
//	}

	/**
	 * Returns whether the given page is still the current page instance 
	 * in the dashboard.
	 * 
	 * @param page
	 * 		page to check; if null, checks if there is no current page
	 * @return
	 * 		true if the given page is the current page in the dashboard
	 */
	public static boolean isCurrentPage(CompositeWithUsername page) {
		return getSingleton().currentPage == page;
	}

	/**
	 * Return the cursor to the automatically assigned one
	 */
	public static void showAutoCursor() {
		showAutoCursor(RootLayoutPanel.get());
	}

	/**
	 * Displays the wait cursor over the entire page
	 */
	public static void showWaitCursor() {
        showWaitCursor(RootLayoutPanel.get());
	}
    
	public static void showWaitCursor(UIObject element) {
        setCursor(element, Style.Cursor.WAIT);
	}
	public static void showAutoCursor(UIObject element) {
        setCursor(element, Style.Cursor.AUTO);
	}
	public static void setCursor(UIObject element, Style.Cursor cursor) {
	    element.getElement().getStyle().setCursor(cursor);
	}

	@Override
	public void onModuleLoad() {
	    String browser = Window.Navigator.getUserAgent();
	    logToConsole("user agent: " + browser);
        if ( isIE()) {
            showNotification(IE_NOT_SUPPORTED, InfoMsgType.WARNING,
                             new OAPAsyncCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {
                                    logoutUser("OAPUploadDashboard.html");
                                }
                            });
            return;
        }
		if ( historyHandlerReg != null )
			historyHandlerReg.removeHandler();
		// setup history management
		historyHandlerReg = History.addValueChangeHandler(this);
		// show the appropriate page - if new, then the cruise list page
		History.fireCurrentHistoryState();
	}

	@Override
	public void onValueChange(ValueChangeEvent<String> event) {
		String token = event.getValue();
		logToConsole("history event:"+token);
		if ( (token == null) || token.isEmpty() || (currentPage == null) ) {
			// Initial history setup; show the cruise list page
            logToConsole("Initial page load");
            History.newItem(PagesEnum.SHOW_DATASETS.name(), false);
			DatasetListPage.showPage();
		} else {
            try {
                PagesEnum page = PagesEnum.valueOf(token);
                switch (page) {
                    case UPLOAD_DATA:
            			DataUploadPage.redisplayPage(currentPage.getUsername());
                        break;
                    case IDENTIFY_COLUMNS:
            			DataColumnSpecsPage.redisplayPage(currentPage.getUsername());
                        break; 
                    case SHOW_DATA_MESSAGES:
                        DataMessagesPage.redisplayPage(currentPage.getUsername());
                        break;
                    case EDIT_METADATA:
                        boolean isSafari = isSafari();
                        logToConsole("EDIT_METADATA Safari: " + isSafari);
                        if ( isSafari ) {
                            DatasetListPage.showPage();
                        } else {
                			MetadataManagerPage.redisplayPage(currentPage.getUsername());
                        }
                        break;
                    case MANAGE_DOCUMENTS:
            			AddlDocsManagerPage.redisplayPage(currentPage.getUsername());
                        break;
                    case PREVIEW_DATASET:
            			DatasetPreviewPage.redisplayPage(currentPage.getUsername());
                        break;
                    case SUBMIT_FOR_QC:
            			SubmitForQCPage.redisplayPage(currentPage.getUsername());
                        break;
                    case SUBMIT_TO_ARCHIVE:
            			SubmitToArchivePage.redisplayPage(currentPage.getUsername());
                        break;
                    case SHOW_DATASETS:
                    default:
            			DatasetListPage.showPage();
                }
            } catch (Exception ex) {
                logToConsole("Page name error:" + token + ":" + String.valueOf(ex));
    			DatasetListPage.showPage();
            }
		}
	}

	/**
	 * Removes the history change handler, if there is one
	 */
	public static void stopHistoryHandling() {
		if ( (singleton == null) || (singleton.historyHandlerReg == null) )
			return;
		singleton.historyHandlerReg.removeHandler();
		singleton.historyHandlerReg = null;
	}

	public static String getBaseUrl() {
		String baseUrl = GWT.getHostPageBaseURL();
		if ( ! baseUrl.endsWith("/")) {
			baseUrl += "/";
		}
		String appName = UPLOAD_DASHBOARD_SERVER_NAME;
//		if ( baseUrl.indexOf(appName) == -1 ) {
			baseUrl += appName + "/";
//		}
		return baseUrl;
	}

    static String mustLoginMsg = 
            "Your session has expired.<br/>"
            + "You must log in again.<br/>";

    public static void ask(String question, String yesText, String noText, InfoMsgType type, AsyncCallback<Boolean> callback) {
        new DashboardAskPopup(yesText, noText, type, callback).askQuestion(question);
    }
    
    static String loginFormHtml = 
      "<form id=\"reloginForm\" method=\"post\" action=\"j_security_check\"  >" + // onsubmit=\"closeLoginPopup()\" 
        "<table class=\"login_table\" border=\"0\" cellspacing=\"5\">" +
          "<tr>" +
            "<td style=\"text-align:center; padding-bottom: .5em;\" colspan=\"2\"><b>Your session has expired.</b></td>" +
          "</tr>" +
          "<tr>" +
            "<td style=\"text-align:center; padding-bottom: .25em;\" colspan=\"2\"><b>Please enter your OAP Dashboard Login Credentials</b></td>" +
          "</tr>" +
          "<tr>" +
            "<td style=\"text-align:center; padding-bottom: 1em;\" colspan=\"2\"><b>and try again.</b></td>" +
          "</tr>" +
          "<tr>" +
            "<td>Username:</td>" +
            "<td><input type=\"text\" name=\"j_username\" /></td>" +
          "</tr>" +
          "<tr>" +
            "<td>Password:</td>" +
            "<td><input type=\"password\" name=\"j_password\" /></td>" +
          "</tr>" +
          "<tr>" +
            "<td style=\"text-align:center;\" colspan=\"2\"><input type=\"submit\" class=\"reloginSubmit\"" +
                " onclick=\"completeRelogin()\" value=\"Submit\" /></td>" + 
          "</tr>" +
        "</table>" +
      "</form>" ;
  
    // This sets up so submit button onclick can call completeRelogin()
    static native void setupLoginPopupClose()/*-{
        console.log("setupClose");
        if ( ! $wnd.completeRelogin ) {
            console.log("wnd: " + $wnd);
            $wnd.completeRelogin = @gov.noaa.pmel.dashboard.client.UploadDashboard::completeRelogin();
            console.log("completeRelogin: " + $wnd.completeRelogin);
        }
    }-*/;
    
    // called from native JS in setupLoginPopupClose() above.
    public static void completeRelogin() {
       logger.info("Completing relogin submission");
       forceSubmittal();
       closeLoginPopup();
    }
    
    // Doing it this way because FF won't submit the form
    private static void forceSubmittal() {
        FormElement form = (FormElement)Document.get().getElementById("reloginForm");
        GWT.log("force form:"+form);
        StringBuilder postData = new StringBuilder();
        NodeList<Element>nodeList = form.getElementsByTagName("input");
        GWT.log("nodeList:"+nodeList+"["+nodeList.getLength());
        String amp = "";
        for (int i = 0; i < nodeList.getLength(); i++ ) {
            InputElement in = (InputElement)nodeList.getItem(i);
            GWT.log("element:"+i);
            String name = in.getName();
            String value = in.getValue();
            GWT.log("name:"+name+", val:"+value);
            if ( name != null && name.trim().length() > 0 ) {
                String encoded = UriUtils.encode(value);
                postData.append(amp).append(name).append("=").append(encoded);
                amp = "&";
            }
        }
        
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, "j_security_check");

        try {
          builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
          Request response = builder.sendRequest(postData.toString(), new RequestCallback() {
            
            @Override
            public void onResponseReceived(Request respRequest, Response resp) {
                GWT.log("reponse request:"+respRequest);
                GWT.log("reponse :"+resp);
            }
            
            @Override
            public void onError(Request errRequest, Throwable t) {
                logToConsole("reponse request:"+errRequest);
                logToConsole("reponse :"+t);
            }
        }); 
          GWT.log("response:"+response);
        } catch (RequestException e) {
          Window.alert("Failed to send the request: " + e.getMessage());
        }
    }

    public static void showPopupMessage(String htmlMsg) {
        showPopupMessage(htmlMsg, null);
    }
    
    public static void showPopupMessage(String htmlMsg, String closeButtonText) {
        getSingleton();
		if ( singleton.blankMsgPopup == null )
			singleton.blankMsgPopup = new DashboardBlankPagePopup();
		singleton.blankMsgPopup.setMessage(htmlMsg);
        if ( closeButtonText != null ) {
            singleton.blankMsgPopup.setCloseButtonText(closeButtonText);
        }
		singleton.blankMsgPopup.showCentered();
    }
    
    public static void showDataPreviewPopup(String previewHtml, int height, int width) {
		if ( singleton == null )
			singleton = new UploadDashboard();
//		if ( singleton.dataPreviewPopup == null )
			singleton.dataPreviewPopup = new FileDataPreviewPopup();
            singleton.dataPreviewPopup.setHeight(height + "px");
            singleton.dataPreviewPopup.setWidth(width + "px");
		singleton.dataPreviewPopup.setMessage(previewHtml);
		singleton.dataPreviewPopup.showCentered();
    }
    
    public static void showLoginPopup() {
        setupLoginPopupClose();
        showPopupMessage(loginFormHtml, "Cancel"); 
//        FormElement form = (FormElement)Document.get().getElementById("reloginForm");
//        Document.get().getBody().appendChild(form);
    }
    
    public static void closeLoginPopup() {
        logger.info("Closing login popup");
        if ( singleton.blankMsgPopup != null ) {
            singleton.blankMsgPopup.dismiss();
        }
    }
    /**
     * 
     */
    public static void showFeedbackPopup() {
        getSingleton();
		if ( singleton.feedbackPopup == null ) {
			singleton.feedbackPopup = new DashboardFeedbackPopup(new AsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean sendIt) {
                    if ( sendIt.booleanValue()) {
                        service.submitFeedback(singleton.currentPage.getUsername(), 
                                             singleton.feedbackPopup.getFeedbackType(),
                                             singleton.feedbackPopup.getMessage(), new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable arg0) {
                                logToConsole("send feedback failed: " + arg0);
                            }

                            @Override
                            public void onSuccess(Void arg0) {
                                GWT.log("send feedback success:" + arg0);
                            }
                        });
                    } else {
                        GWT.log("feedback cancelled");
                    }
                }
                @Override
                public void onFailure(Throwable arg0) {
                    logToConsole("Feedback failure: " + arg0);
                }
            });
		}
        singleton.feedbackPopup.reset();
		singleton.feedbackPopup.show();
    }
    public static void showUserInfoPopup() {
        GWT.log("pinging before change password");
        pingService(new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void arg0) {
                GWT.log("successful pinging before change password");
                _getUserInfoForPopup();
            }
            @Override
            public void customFailure(Throwable t) {
                logToConsole("ping fail: "+ t);
            }
        });
    }
    private static void _getUserInfoForPopup() {
        GWT.log("get user info");
        getSingleton();
        service.getUserProfile(singleton.currentPage.getUsername(), 
               new AsyncCallback<DashboardServiceResponse<UserInfo>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        logToConsole("Retrieve Profile Info failed: " + caught);
                        showFailureMessage("There was an error retrieving your profile information.", caught);
                    }
                    @Override
                    public void onSuccess(DashboardServiceResponse<UserInfo> result) {
                        GWT.log("retrieved user profile information:" + result);
                        if ( result.wasSuccessful()) {
                            _showUserInfoPopup(result.response());
                        } else {
                            showFailureMessage("There was a problem retrieving your profile information:\n" + result.error(), null);
                        }
                    }
        });
    }
    private static void _showUserInfoPopup(UserInfo userInfo) {
        GWT.log("show user info");
        getSingleton();
		if ( singleton.userInfoPopup == null ) {
			singleton.userInfoPopup = new EditProfilePopup(new AsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean sendIt) {
                    if ( sendIt.booleanValue()) {
                        EditProfilePopup popup = singleton.userInfoPopup;
                        UserInfo updatedInfo = new UserInfo(userInfo.username(), 
                                                            popup.getFirstName(), 
                                                            popup.getMiddle(), 
                                                            popup.getLastName(), 
                                                            popup.getEmail(), 
                                                            popup.getTelephone(), 
                                                            popup.getExtension(), 
                                                            popup.getOrganization());
                        service.updateUserProfile(updatedInfo,
                                           new AsyncCallback<DashboardServiceResponse<Void>>() {
                            @Override
                            public void onFailure(Throwable arg0) {
                                logToConsole("Update Profile Info failed: " + arg0);
                                showFailureMessage("There was an error updating your profile information.", arg0);
                            }

                            @Override
                            public void onSuccess(DashboardServiceResponse<Void> updated) {
                                GWT.log("Update Profile Info onSuccess:" + updated);
                                if ( updated.wasSuccessful()) {
                                    showMessage("Profile has been updated.");
                                } else {
                                    showFailureMessage("Failed to update profile information:\n" + updated.error(), null);
                                }
                            }
                        });
                    } else {
                        GWT.log("Update Profile cancelled");
                    }
                }
                @Override
                public void onFailure(Throwable arg0) {
                    logToConsole("Update Profile failure: " + arg0);
                    showFailureMessage("There was a service error updating your profile information.", arg0);
                }
            });
		}
        singleton.userInfoPopup.reset();
		singleton.userInfoPopup.show(userInfo);
    }
    
    public static void showChangePasswordPopup() {
        GWT.log("pinging before change password");
        pingService(new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void arg0) {
                GWT.log("successful pinging before change password");
                _showChangePasswordPopup();
            }
            @Override
            public void customFailure(Throwable t) {
                logToConsole("ping fail: "+ t);
            }
        });
    }
    private static void _showChangePasswordPopup() {
        GWT.log("show change password");
        getSingleton();
		if ( singleton.changePasswordPopup == null ) {
			singleton.changePasswordPopup = new ChangePasswordPopup(new AsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean sendIt) {
                    if ( sendIt.booleanValue()) {
                        service.changePassword(singleton.currentPage.getUsername(), 
                                               singleton.changePasswordPopup.getCurrentPassword(),
                                               singleton.changePasswordPopup.getNewPassword(), new AsyncCallback<DashboardServiceResponse>() {
                            @Override
                            public void onFailure(Throwable arg0) {
                                logToConsole("Change Password failed: " + arg0);
                                showFailureMessage("There was an error changing your password.", arg0);
                            }

                            @Override
                            public void onSuccess(DashboardServiceResponse changed) {
                                GWT.log("Change Password success:" + changed);
                                if ( changed.wasSuccessful()) {
                                    showMessage("Password changed.");
                                } else {
                                    showFailureMessage("Failed to change password:\n" + changed.error(), null);
                                }
                            }
                        });
                    } else {
                        GWT.log("Change Password cancelled");
                    }
                }
                @Override
                public void onFailure(Throwable arg0) {
                    logToConsole("Change Password failure: " + arg0);
                    showFailureMessage("There was a service error changing your password.", arg0);
                }
            });
		}
        singleton.changePasswordPopup.reset();
		singleton.changePasswordPopup.show(singleton.currentPage.getUsername());
    }
    
    /**
     * @param datasetId
     */
    public static void showUpdateSubmissionDialog(String username, DashboardDataset dataset) {
        getSingleton();
		if ( singleton.dataUpdatePopup == null ) {
            singleton.dataUpdatePopup = new DataUpdatePopup(singleton.currentPage.getUsername());
		}
        singleton.dataUpdatePopup.showPage(dataset);
    }
    /**
     * @param version
     */
    public static void setAppBuildVersion(String version) {
        buildVersion = version;
    }
    public static void setMaxUploadSize(long sizeLimit) {
        maxUploadSize = sizeLimit;
    }
    public static long getMaxUploadSize() {
        return maxUploadSize;
    }
    public static void setMaxUploadSizeDisplayStr(String sizeLimit) {
        maxUploadSizeStr = sizeLimit;
    }
    public static String getMaxUploadSizeStr() {
        return maxUploadSizeStr;
    }
 }
