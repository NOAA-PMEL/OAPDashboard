/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FileInfo;
import gov.noaa.pmel.dashboard.shared.MetadataPreviewInfo;
import gov.noaa.pmel.dashboard.shared.NotFoundException;

/**
 * @author Karl Smith
 */
public class MetadataManagerPage extends CompositeWithUsername {

	private static final String TITLE_TEXT = "Manage Metadata";
	private static final String WELCOME_INTRO = "Logged in as ";
	private static final String LOGOUT_TEXT = "Logout";
	private static final String UPLOAD_TEXT = "Upload";
	private static final String DOWNLOAD_TEXT = "Download";
	private static final String DONE_TEXT = "Done";

	private static final String CRUISE_HTML_INTRO_PROLOGUE = 
			"<p>At this time, the system only manages OADS XML metadata files.</p>" +
//			"<p>To generate a SOCAT OME XML metadata file to upload: <ul>" +
//			"<li>Go to the Online Metadata Editor site " +
//			"<a href=\"http://mercury.ornl.gov/socatome/\" target=\"_blank\">" +
//			"http://mercury.ornl.gov/socatome/</a></li>" +
//			"<li>Fill in the appropriate metadata</li>" +
//			"<li>Save a local copy (preferrably with validation)</li>" +
//			"</ul>" +
//			"This will create a SOCAT OME XML metadata file on your system that can be uploaded here. " +
//			"</p><p>" +
			"Dataset: <ul><li>";
	private static final String CRUISE_HTML_INTRO_EPILOGUE = "</li></ul></p>";

	private static final String NO_FILE_ERROR_MSG = 
			"Please select an OADS XML metadata file to upload";

	private static final String OVERWRITE_WARNING_MSG = 
			"The OADS XML metadata for this dataset will be overwritten.  Do you wish to proceed?";
	private static final String OVERWRITE_YES_TEXT = "Yes";
	private static final String OVERWRITE_NO_TEXT = "No";

	private static final String UNEXPLAINED_FAIL_MSG = 
			"<h3>Upload failed.</h3>" + 
			"<p>Unexpectedly, no explanation of the failure was given</p>";
	private static final String EXPLAINED_FAIL_MSG_START = 
			"<h3>Upload failed.</h3>" +
			"<p><pre>\n";
	private static final String EXPLAINED_FAIL_MSG_END = 
			"</pre></p>";
	private static final String DOWNLOAD_SERVICE_NAME = "MetadataDownloadService";
	private static final String NO_METADATA = "No metadata found";

	interface MetadataManagerPageUiBinder extends UiBinder<Widget, MetadataManagerPage> {
	}

	private static MetadataManagerPageUiBinder uiBinder = 
			GWT.create(MetadataManagerPageUiBinder.class);

	private static DashboardServicesInterfaceAsync service = 
			GWT.create(DashboardServicesInterface.class);

	@UiField InlineLabel titleLabel;
	@UiField InlineLabel userInfoLabel;
	@UiField Button logoutButton;
	@UiField HTML introHtml;
	@UiField HTML metadataFileInfoHtml;
	@UiField FormPanel uploadForm;
	@UiField FileUpload mdUpload;
	@UiField Hidden timestampField;
	@UiField Hidden datasetIdsField;
	@UiField InlineLabel previewTitle;
	@UiField HTML filePreviewPanel;
	@UiField Button uploadButton;
	@UiField Button downloadButton;
	@UiField Button doneButton;

	private DashboardDataset cruise;
	private DashboardAskPopup askOverwritePopup;

	// Singleton instance of this page
	private static MetadataManagerPage singleton;
	
	MetadataManagerPage() {
		initWidget(uiBinder.createAndBindUi(this));
		singleton = this;

		setUsername(null);
		cruise = null;
		askOverwritePopup = null;

		titleLabel.setText(TITLE_TEXT);
		logoutButton.setText(LOGOUT_TEXT);

		previewTitle.setText("Current metadata:");
		
		uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadForm.setMethod(FormPanel.METHOD_POST);
		uploadForm.setAction(GWT.getModuleBaseURL() + "MetadataUploadService");

		clearTokens();

		uploadButton.setText(UPLOAD_TEXT);
		downloadButton.setText(DOWNLOAD_TEXT);
		doneButton.setText(DONE_TEXT);
	}

	/**
	 * Display the metadata upload page in the RootLayoutPanel
	 * for the given cruise.  Adds this page to the page history.
	 * 
	 * @param cruises
	 * 		add/replace the metadata for the cruise in this list 
	 */
	static void showPage(DashboardDatasetList cruises) {
		if ( singleton == null )
			singleton = new MetadataManagerPage();
		singleton.updateDataset(cruises);
		UploadDashboard.updateCurrentPage(singleton);
		History.newItem(PagesEnum.EDIT_METADATA.name(), false);
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
	 * Updates this page with the username and the cruise in the given set of cruise.
	 * 
	 * @param cruises
	 * 		associate the uploaded metadata to the cruise in this set of cruises
	 */
	private void updateDataset(DashboardDatasetList cruises) {
		// Update the current username
		setUsername(cruises.getUsername());
		userInfoLabel.setText(WELCOME_INTRO + getUsername());

		// Update the cruise associated with this page
		cruise = cruises.values().iterator().next();
		String datasetId = cruise.getDatasetId();
		
		// Update the HTML intro naming the cruise
		introHtml.setHTML(CRUISE_HTML_INTRO_PROLOGUE + 
				SafeHtmlUtils.htmlEscape(datasetId) + 
				CRUISE_HTML_INTRO_EPILOGUE);

		setMetadataFileInfo(null);
		filePreviewPanel.setHTML(NO_METADATA);
		
		// Clear the hidden tokens just to be safe
		clearTokens();
		getMetadataPreview(cruise.getDatasetId());
	}

	private void getMetadataPreview(String datasetId) {
		service.getMetadataPreviewInfo(getUsername(), datasetId, new SessionHandlingCallbackBase<MetadataPreviewInfo>() {
			@Override
			public void onSuccess(MetadataPreviewInfo result) {
				String html = result.getMetadataPreview();
				filePreviewPanel.setHTML(html);
				setMetadataFileInfo(result.getMetadataFileInfo());
			}
			@Override
			public void handleFailure(Throwable caught) {
				setMetadataFileInfo(null);
				String msg = caught.getMessage();
				if ( caught instanceof NotFoundException ) {
					UploadDashboard.showMessage(msg);
				} else {
					UploadDashboard.showFailureMessage(msg, caught);
				}
			}
		});
	}
	
	private void setMetadataFileInfo(FileInfo metadataFileInfo) {
		String fileInfoHtml;
		if ( metadataFileInfo == null ) {
			fileInfoHtml = "";
		} else {
			fileInfoHtml = "Metadata File: " + metadataFileInfo.getFileName();
			fileInfoHtml += "<br/><ul>" +
							"<li>created: " + metadataFileInfo.getFileCreateTime() + "</li>" + 
							"<li>modified: " + metadataFileInfo.getFileModTime() + "</li>" + 
							"<li>size: " + metadataFileInfo.getFileSize() + "</li>" +
							"</ul>";
		}
		metadataFileInfoHtml.setHTML(fileInfoHtml);
	}


	/**
	 * Clears all the Hidden tokens on the page. 
	 */
	private void clearTokens() {
		timestampField.setValue("");
		datasetIdsField.setValue("");
	}

	/**
	 * Assigns all the Hidden tokens on the page. 
	 */
	private void assignTokens() {
		String localTimestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm Z").format(new Date());
		timestampField.setValue(localTimestamp);
		datasetIdsField.setValue(cruise.getDatasetId());
	}

	@UiHandler("logoutButton")
	void logoutOnClick(ClickEvent event) {
		DashboardLogoutPage.showPage();
	}

	@UiHandler("doneButton")
	void doneButtonOnClick(ClickEvent event) {
		// Return to the cruise list page which might have been updated
		uploadForm.reset();
		DatasetListPage.showPage();
	}

	@UiHandler("uploadButton") 
	void uploadButtonOnClick(ClickEvent event) {
		// Make sure a file was selected
		String uploadFilename = DashboardUtils.baseName(mdUpload.getFilename());
		if ( uploadFilename.isEmpty() ) {
			UploadDashboard.showMessage(NO_FILE_ERROR_MSG);
			return;
		}

		// If an overwrite will occur, ask for confirmation
		if ( ! cruise.getMdTimestamp().isEmpty() ) {
			if ( askOverwritePopup == null ) {
				askOverwritePopup = new DashboardAskPopup(OVERWRITE_YES_TEXT, 
						OVERWRITE_NO_TEXT, new AsyncCallback<Boolean>() {
					@Override
					public void onSuccess(Boolean result) {
						// Submit only if yes
						if ( result == true ) {
							assignTokens();
							uploadForm.submit();
						}
					}
					@Override
					public void onFailure(Throwable ex) {
						// Never called
						;
					}
				});
			}
			askOverwritePopup.askQuestion(OVERWRITE_WARNING_MSG);
			return;
		}

		// Nothing overwritten, submit the form
		assignTokens();
		uploadForm.submit();
	}

	@UiHandler("uploadForm")
	void uploadFormOnSubmit(SubmitEvent event) {
		UploadDashboard.showWaitCursor();
	}

	@UiHandler("uploadForm")
	void uploadFormOnSubmitComplete(SubmitCompleteEvent event) {
		clearTokens();
		processResultMsg(event.getResults());
		// Restore the usual cursor
		UploadDashboard.showAutoCursor();
	}

	@UiHandler("downloadButton")
	void downloadButtonOnClick(ClickEvent event) {
		String downloadUrl = getDownloadUrl(cruise.getDatasetId());
		Window.Location.replace(downloadUrl);
	}
	
	private String getDownloadUrl(String datasetId) {
		StringBuilder b = new StringBuilder(UploadDashboard.getBaseUrl())
								.append(DOWNLOAD_SERVICE_NAME)
								.append("/").append(datasetId);
		return b.toString();
	}
	
	/**
	 * Process the message returned from the upload of a dataset.
	 * 
	 * @param resultMsg
	 * 		message returned from the upload of a dataset
	 */
	private void processResultMsg(String resultMsg) {
		if ( resultMsg == null ) {
			UploadDashboard.showMessage(UNEXPLAINED_FAIL_MSG);
			return;
		}
		resultMsg = resultMsg.trim();
		if ( resultMsg.startsWith(DashboardUtils.SUCCESS_HEADER_TAG) ) {
			// cruise file created or updated; return to the cruise list, 
			// having it request the updated cruises for the user from the server
			uploadForm.reset();
//			mdUpload.getElement().setPropertyString("value", "");
			DatasetListPage.showPage();
		}
		else {
			// Unknown response, just display the entire message
			UploadDashboard.showMessage(EXPLAINED_FAIL_MSG_START + 
					SafeHtmlUtils.htmlEscape(resultMsg) + EXPLAINED_FAIL_MSG_END);
		}
	}

}
