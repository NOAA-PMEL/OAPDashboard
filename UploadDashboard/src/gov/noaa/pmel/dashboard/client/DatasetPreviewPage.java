/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.apache.logging.log4j.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.PreviewPlotResponse;

/**
 * Page showing various plots of cruise data.
 * These plots are to be examined by a user 
 * to catch errors prior to submitting for QC.
 * 
 * @author Karl Smith
 */
public class DatasetPreviewPage extends CompositeWithUsername {
	
	Logger logger = Logger.getLogger("DatasetPreviewPage");

	private static final String TITLE_TEXT = "Preview Dataset";
	private static final String WELCOME_INTRO = "Logged in as ";
	private static final String LOGOUT_TEXT = "Logout";

	private static final String INTRO_HTML_PROLOGUE = 
			"Plots of the dataset: ";

	private static final String REFRESH_TEXT = "Refresh plots";
	private static final String REFRESH_HOVER_HELP = "Refresh the display the generated plots";
	private static final String DISMISS_TEXT = "Done";

	private static final String PLOT_GENERATION_FAILURE_HTML = "<b>Problems generating the plot previews</b>";

	private static final String TAB0_TEXT = "Overview";
	private static final String TAB1_TEXT = "General";
	private static final String TAB2_TEXT = "BioGeoChem";
	private static final String TAB3_TEXT = "Nutrients +";

	private static final String REFRESH_HELP_ADDENDUM = 
			" -- if plots do not show after awhile, try pressing the '" + REFRESH_TEXT + "' button given below this image.";

	private static final String TAB0_ALT_TEXT = "Overview plots";
	private static final String TAB1_ALT_TEXT = "latitude versus longitude";
	private static final String TAB2_ALT_TEXT = "latitude, longitude versus time";
	private static final String TAB3_ALT_TEXT = "sample number (row number) versus time";

	public static final String LAT_VS_LON_IMAGE_NAME = "lat_vs_lon";
	public static final String LAT_LON_IMAGE_NAME = "lat_lon";
	public static final String SAMPLE_VS_TIME_IMAGE_NAME = "sample_vs_time";

	interface DatasetPreviewPageUiBinder extends UiBinder<Widget, DatasetPreviewPage> {
	}

	private static DatasetPreviewPageUiBinder uiBinder = 
			GWT.create(DatasetPreviewPageUiBinder.class);

	private static DashboardServicesInterfaceAsync service = 
			GWT.create(DashboardServicesInterface.class);

	@UiField InlineLabel titleLabel;
	@UiField InlineLabel userInfoLabel;
	@UiField Button logoutButton;
	@UiField HTML introHtml;
	@UiField Button refreshButton;
	@UiField Button dismissButton;
	
//	@UiField ResizeLayoutPanel resizePanel;
	@UiField TabLayoutPanel tabsPanel;
	@UiField FlowPanel tab0Panel;
	@UiField FlowPanel tab1Panel;
	@UiField FlowPanel tab2Panel;
	@UiField FlowPanel tab3Panel;

	@UiField HTML tab0Html;
	@UiField HTML tab1Html;
	@UiField HTML tab2Html;
	@UiField HTML tab3Html;

	List<List<Image>> tabImages = new ArrayList<List<Image>>();
	List<List<String>> availablePlots = new ArrayList<>();
	List<FlowPanel> tabPanels = new ArrayList<FlowPanel>();
	
	String expocode;
	String timetag;
	AsyncCallback<PreviewPlotResponse> checkStatusCallback;

	// The singleton instance of this page
	private static DatasetPreviewPage singleton;

	public DatasetPreviewPage() {
		initWidget(uiBinder.createAndBindUi(this));
		singleton = this;

		setUsername(null);
		expocode = "";
		// Callback when generating plots
		checkStatusCallback = new AsyncCallback<PreviewPlotResponse>() {
			@Override
			public void onSuccess(PreviewPlotResponse plotResponse) {
				logger.fine("Got response " + plotResponse);
				boolean isDone = plotResponse.isFinished();
				if ( UploadDashboard.isCurrentPage(singleton) ) {
					if ( isDone ) {
						UploadDashboard.showAutoCursor();
					}
					availablePlots = plotResponse.getPlotTabs();
					// Refresh this page to get the new image(s)
					singleton.resetImageUrls(true);
					if ( ! isDone ) {
						// More images to be generated - inquire again
						service.buildPreviewImages(getUsername(), singleton.expocode, 
								singleton.timetag, false, checkStatusCallback);
					}
				}
			}
			@Override
			public void onFailure(Throwable ex) {
				logger.log(Level.FINE, "Get Preview failure", ex);
				if ( UploadDashboard.isCurrentPage(singleton) ) {
					UploadDashboard.showAutoCursor();
					singleton.resetImageUrls(false);
					UploadDashboard.showFailureMessage(PLOT_GENERATION_FAILURE_HTML, ex);
				}
			}
		};

		titleLabel.setText(TITLE_TEXT);
		logoutButton.setText(LOGOUT_TEXT);

		refreshButton.setText(REFRESH_TEXT);
		refreshButton.setTitle(REFRESH_HOVER_HELP);
		dismissButton.setText(DISMISS_TEXT);

		// Set the HTML for the tabs
		tab0Html.setHTML(TAB0_TEXT);
		tab1Html.setHTML(TAB1_TEXT);
		tab2Html.setHTML(TAB2_TEXT);
		tab3Html.setHTML(TAB3_TEXT);

		// Set hover helps for the tabs
		tab0Html.setTitle(TAB0_ALT_TEXT);
		tab1Html.setTitle(TAB1_ALT_TEXT);
		tab2Html.setTitle(TAB2_ALT_TEXT);
		tab3Html.setTitle(TAB3_ALT_TEXT);

		tabPanels.add(tab0Panel);
		tabPanels.add(tab1Panel);
		tabPanels.add(tab2Panel);
		tabPanels.add(tab3Panel);
		
		// Set text alternative for the images
//		latVsLonImage.setAltText(TAB1_ALT_TEXT + REFRESH_HELP_ADDENDUM);
//		latLonImage.setAltText(TAB2_ALT_TEXT + REFRESH_HELP_ADDENDUM);
//		sampleVsTimeImage.setAltText(TAB3_ALT_TEXT + REFRESH_HELP_ADDENDUM);

		// Set hover helps for the images
//		latVsLonImage.setTitle(TAB1_ALT_TEXT);
//		latLonImage.setTitle(TAB2_ALT_TEXT);
//		sampleVsTimeImage.setTitle(TAB3_ALT_TEXT);
	}

	/**
	 * Display the preview page in the RootLayoutPanel with data plots  
	 * for the first cruise in the given cruiseList.  
	 * Adds this page to the page history.
	 */
	static void showPage(DashboardDatasetList cruiseList) {
		if ( singleton == null )
			singleton = new DatasetPreviewPage();
		UploadDashboard.updateCurrentPage(singleton);
		singleton.updatePreviewPlots(cruiseList.keySet().iterator().next(), 
									 cruiseList.getUsername());
		History.newItem(PagesEnum.PREVIEW_DATASET.name(), false);
	}

	/**
	 * Redisplays the last version of this page if the username
	 * associated with this page matches the given username.
	 */
	static void redisplayPage(String username) {
		if ( (username == null) || username.isEmpty() || 
			 (singleton == null) || ! singleton.getUsername().equals(username) ) {
			DatasetListPage.showPage();
		}
		else {
			UploadDashboard.updateCurrentPage(singleton);
		}
	}

	/**
	 * Updates the this page with the plots for the indicated cruise.
	 * 
	 * @param dataset
	 * 		cruises to use
	 * @param username
	 * 		user requesting these plots 
	 */
	private void updatePreviewPlots(String expocode, String username) {
		// Update the username
		setUsername(username);
		userInfoLabel.setText(WELCOME_INTRO + getUsername());

		if ( expocode != null )
			this.expocode = expocode.trim().toUpperCase();
		else
			this.expocode = "";
		introHtml.setHTML(INTRO_HTML_PROLOGUE + SafeHtmlUtils.htmlEscape(this.expocode));
//		if ( this.expocode.length() > 11 ) { // WTF?
			// Tell the server to generate the preview plots
			UploadDashboard.showWaitCursor();
			DateTimeFormat formatter = DateTimeFormat.getFormat("MMddHHmmss");
			this.timetag = formatter.format(new Date(), TimeZone.createTimeZone(0));
			service.buildPreviewImages(getUsername(), this.expocode, 
					this.timetag, true, checkStatusCallback);
//		}
		// Set the URLs for the images.
		resetImageUrls();
	}

	/**
	 * Assigns the URLs to the images.  
	 * This triggers load events so the page should refresh when this is called.
	 */
	private void resetImageUrls() {
		String imagePrefix;
		String imageSuffix;
		// XXX This should really come from the server...
		imagePrefix = "preview/plots/" + expocode.substring(0,4) + "/";
		clearTabImages();
		if ( availablePlots == null || availablePlots.isEmpty()) {
			String url = "images/loadingCircleSpinner.gif";
			Image img = new Image(url);
			img.setStyleName("plotsimage");
			List<Image> tab0Images = new ArrayList<>();
			tab0Images.add(img);
			tab0Panel.add(img);
			tabImages.add(tab0Images);
		} else {
			int tab = 0;
			Image img = null;
			String noCache="?ts=" + System.currentTimeMillis();
			for (List<String> tabImageFileNames : availablePlots) {
				List<Image> tabXimages = new ArrayList<Image>(); // tabImages.get(tab);
				tabImages.add(tabXimages);
				FlowPanel tabPanel = tabPanels.get(tab);
				for (String imageName : tabImageFileNames) {
					String url = imagePrefix + imageName + noCache;
					img = new Image(url);
//					img.setPixelSize(imageSize, imageSize);
					img.setStyleName("plotsimage");
					tabPanel.add(img);
					tabXimages.add(img);
				}
				tab += 1;
				if ( img != null ) {
					img.addStyleName("lastimage");
				}
			}
		}
	}

	private void clearTabImages() {
		if ( tabImages.isEmpty()) { return; }
		for (int tab = 0; tab < tabImages.size(); tab++ ) {
			List<Image> tabXimages = tabImages.get(tab);
			FlowPanel tabPanel = tabPanels.get(tab);
			for (Image img : tabXimages) {
				tabPanel.remove(img);
			}
			tabXimages.clear();
		}
		tabImages.clear();
	}

	private void resetImageUrls(boolean successful) {
		resetImageUrls();
	}
	
	@UiHandler("refreshButton")
	void refreshOnClick(ClickEvent event) {
		// Reload the images by setting the URLs again
		resetImageUrls();
	}

	@UiHandler("logoutButton")
	void logoutOnClick(ClickEvent event) {
		DashboardLogoutPage.showPage();
	}

	@UiHandler("dismissButton")
	void cancelOnClick(ClickEvent event) {
		// Change to the latest cruise listing page.
		DatasetListPage.showPage();
	}

}
