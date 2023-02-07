/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FileType;
import gov.noaa.pmel.dashboard.shared.ObservationType;

/**
 * Page for uploading new or updated cruise data files.
 * 
 * @author Karl Smith
 */
public class DataUploadPage extends CompositeWithUsername {

	private static final String TITLE_TEXT = "Upload Data Files";
	private static final String MORE_HELP_TEXT = "Example Data";

	private static final String UPLOAD_FILE_INTRO_HTML = 
            "<h3>Upload data files for data checks and archival.</h3>" 
            + "Any file can be uploaded for archival.  However, only ASCII-delimited (CSV, etc.) files and Excel spreadsheets can be checked for errors."
            + "<br/><div style=\"padding-bottom:.5em; padding-top:.5em;\">To be checked, a delimited file must include:</div>"
			+ "<ul style=\"list-style-type:disk\">"
			+ "  <li>A complete header line with <span style=\"font-weight:bold;\">column names for every column</span>,</li>"
			+ "  <li>an optional header line of data column units where appropriate,</li>"
			+ "  <li>followed by any number of lines of data values.</li>"
			+ "</ul><br/>"
            + "In addition, a file may contain:"
			+ "<ul style=\"list-style-type:disk\">"
			+ "  <li>Any number of comment lines that begin with the hash (\'#\') character.</li>"
			+ "  <li>Any number of textual lines before the data column header row, provided they do not contain more than 5 columns.</li>"
			+ "</ul><br/>"
            ;
	private static final String MORE_HELP_HTML = 
			"<p>The first few lines of a comma-separated upload datafile " + 
			"may look something like the follow.  (Note that the data " +
			"lines were truncated to make it easier to see the format.) " +
			"<ul style=\"list-style-type:none\">" +
			"  <li>CruiseID, DATE_ddmmyyyy, TIME_hh:mm:ss, LAT_degN, LON_degE, ...<br /></li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:00:45, 12.638, -59.239, ...</li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:03:14, 12.633, -59.233, ...</li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:05:43, 12.628, -59.228, ...</li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:08:12, 12.622, -59.222, ...</li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:10:42, 12.617, -59.216, ...</li>" +
			"</ul>" +
			"The dataset ID for these samples will be <pre>ATLANTIS2001B</pre>" +
			"</p><p>" +
			"Identification of column types from names is case insensitive " +
			"and ignores any characters that are not letters or numbers. " +
			"Units for the columns can either follow the column name, as above, " +
			"or be given on a second column header line, such as the following:" +
			"<ul style=\"list-style-type:none\">" +
			"  <li>Cruise, Date, Time, Lat, Lon, ...<br /></li>" +
			"  <li> , ddmmyyyy, hh:mm:ss, deg N, deg E, ...</li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:00:45, 12.638, -59.239, ...</li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:03:14, 12.633, -59.233, ...</li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:05:43, 12.628, -59.228, ...</li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:08:12, 12.622, -59.222, ...</li>" +
			"  <li>Atlantis 20-01B, 19042012, 19:10:42, 12.617, -59.216, ...</li>" +
			"</ul></p>";

	private static final String FEATURE_TYPE_HELP_ANCHOR_TEXT = "Help about observation types.";
	private static final String FEATURE_TYPE_HELP_HTML = 
	        "The Observation Types are from the CF (Climate and Forecast) Conventions' Discrete Sampling Geometries.<br/>"
	        + "See <a href=\"http://cfconventions.org/cf-conventions/cf-conventions.html\" target=\"_blank\">CF Conventions</a> "
	        + "and <a href=\"http://cfconventions.org/cf-conventions/cf-conventions.html#discrete-sampling-geometries\" target=\"_blank\">Discrete Sampling Geometries</a>. "
	        + "<br/>Example observations for each type include:"
	        + "<ul style=\"list-style-type:none\">"
	        + "<li>TimeSeries: Observations at a moored buoy.</li>"
	        + "<li>Trajectory: Surface observations along a ship or vessel track.</li>"
	        + "<li>Profile: Observations from a CTD cast.</li>"
	        + "<li>TimeSeries Profile: A series of profiles at the same location over time.</li>"
	        + "<li>Trajectory Profile: A series of profiles along a ship or vessel track.</li>"
	        + "</ul>";
	
	private static final String ADVANCED_HTML_MSG = 
			"Select a character set encoding for this file." +
			"<ul>" +
			"<li>If you are unsure of the encoding, UTF-8 should work fine.</li>" +
			"<li>The main differences in UTF-8 and ISO encodings are the " +
			"\"extended\" characters.</li>" +
			"<li>Use UTF-16 only if you know your file is encoded in that format, " +
			"but be aware that only Western European characters can be " +
			"properly handled.</li>" +
			"<li>Use the Windows encoding only for files produced by older " +
			"Windows programs. </li>" +
			"<li>The preview button will show the beginning of the file as it will " +
			"be seen by the dashboard using the given encoding.</li>" +
			"</ul>";
	private static final String ENCODING_TEXT = "File encoding:";
	private static final String[] KNOWN_ENCODINGS = {
		"ISO-8859-1", "ISO-8859-15", "UTF-8", "UTF-16", "Windows-1252"
	};
	private static final String PREVIEW_TEXT = "Preview";
	private static final String NO_PREVIEW_HTML_MSG = "<p>(No file previewed)</p>";

	private static final String CREATE_TEXT = "only create new dataset(s)";
	private static final String CREATE_HOVER_HELP = 
			"the data uploaded must only create new datasets";

	private static final String SUBMIT_TEXT = "Upload";
	private static final String CANCEL_TEXT = "Cancel";

	private static final String SELECT_FEATURE_TYPE_MSG = 
            "Please select observation type.";
	private static final String NO_FILE_ERROR_MSG = 
			"Please select a data file to upload";
	private static final String UNEXPLAINED_FAIL_MSG = 
			"<h3>Upload failed.</h3>" + 
			"<p>Unexpected server error: no explanation of the failure was given.</p>";
	private static final String FAIL_MSG_START = 
			"<h3>";
	private static final String EXPLAINED_FAIL_MSG_START =
			"<br /><font style=\"color:red;\">Upload failed.</font></h3>" +
			"<p><pre>\n";
	private static final String EXPLAINED_FAIL_MSG_END = 
			"</pre></p>";
    private static final String UPLOAD_FAILED_SUGGESTIONS = 
            "This may be due to failure to properly parse the file<br/>"
            + "or a file that does not conform to the format requirements.<br/>"
            + "Only ASCII delimited files and Excel spreadsheets with a header row<br/>"
            + "describing the column values can be parsed.<br/>"
            + "You can use the Preview function to check the file.";
	private static final String DATASET_EXISTS_FAIL_MSG_START = 
			"<br />A dataset exists with a given dataset ID.</h3>";
	private static final String DATASET_EXISTS_FAIL_MSG_END = 
			"<p>Either you specified that this data upload file should " +
			"only create new datasets, or you do not have permission " +
			"to modify a dataset for this data.</p>";

	// Remove javascript added by the firewall
	private static final String JAVASCRIPT_START = "<script language=\"javascript\">";
	private static final String JAVASCRIPT_CLOSE = "</script>";

	interface DashboardDatasetUploadPageUiBinder extends UiBinder<Widget, DataUploadPage> {
	}

	private static DashboardDatasetUploadPageUiBinder uiBinder = 
			GWT.create(DashboardDatasetUploadPageUiBinder.class);
   
    @UiField ListBox featureTypeSelector;
    @UiField Hidden observationTypeToken;
    @UiField Anchor featureTypeHelpAnchor;
    
	@UiField HTML introHtml;
	@UiField Anchor moreHelpAnchor;
	@UiField FormPanel uploadForm;
    @UiField FileUpload fileUpload;
	@UiField Hidden timestampToken;
	@UiField Hidden actionToken;
	@UiField Hidden encodingToken;
    @UiField Hidden fileDataFormatToken;
    @UiField Hidden datasetIdToken;
    @UiField Hidden datasetIdColumnToken;
	@UiField DisclosurePanel advancedPanel;
	@UiField HTML advancedHtml;
	@UiField Label encodingLabel;
	@UiField ListBox encodingListBox;
	@UiField Button previewButton;
	@UiField Button submitButton;
	@UiField Button cancelButton;

	private DashboardInfoPopup moreHelpPopup;
    private DashboardInfoPopup featureTypeHelpPopup;
	private Element uploadElement;

	// Singleton instance of this page
	private static DataUploadPage singleton = null;
    private String observationType = null;
    private FileType selectedFileType = FileType.UNSPECIFIED;
    
    public static native int getFileSize(final Element data) /*-{
        return data.files[0].size;
    }-*/;
    
	/**
	 * Creates an empty cruise upload page.  Do not call this 
	 * constructor; instead use the showPage static method 
	 * to show the singleton instance of this page. 
	 */
	DataUploadPage() {
		super(PagesEnum.UPLOAD_DATA.name());
		initWidget(uiBinder.createAndBindUi(this));
		singleton = this;

		setUsername(null);

        featureTypeSelector.addItem("-- Observation Type --");
        for (String obs : ObservationType.types) {
            featureTypeSelector.addItem(obs);
        }
//        featureTypeSelector.addItem("Timeseries", FeatureType.TIMESERIES.name());
//        featureTypeSelector.addItem("Trajectory", FeatureType.TRAJECTORY.name());
//        featureTypeSelector.addItem("Profile", FeatureType.PROFILE.name());
//        featureTypeSelector.addItem("Timeseries Profile", FeatureType.TIMESERIES_PROFILE.name());
//        featureTypeSelector.addItem("Trajectory Profile", FeatureType.TRAJECTORY_PROFILE.name());
//        featureTypeSelector.addItem("Other", FeatureType.OTHER.name());
        featureTypeSelector.getElement().<SelectElement>cast().getOptions().getItem(0).setDisabled(true); // Please select...
        featureTypeSelector.addChangeHandler(new ChangeHandler() {
          @Override
          public void onChange(ChangeEvent event) {
              if ( featureTypeSelector.getSelectedIndex() > 0 ) {
                  featureTypeSelector.removeStyleName("missingInfoItem");
              }
              observationType = featureTypeSelector.getSelectedValue();
              GWT.log("Selecting obs type " + observationType);
          }
        });
        featureTypeHelpAnchor.setText(FEATURE_TYPE_HELP_ANCHOR_TEXT);
        featureTypeHelpPopup = null;

        header.setPageTitle(TITLE_TEXT);
		introHtml.setHTML(UPLOAD_FILE_INTRO_HTML);
		moreHelpAnchor.setText(MORE_HELP_TEXT);
        moreHelpAnchor.setVisible(false);
		moreHelpPopup = null;

		uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadForm.setMethod(FormPanel.METHOD_POST);
		uploadForm.setAction(GWT.getModuleBaseURL() + "DataUploadService");
//        fileUpload.getElement().setAttribute("multiple", "multiple");
        fileUpload.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                String files = fileUpload.getFilename(); // getInputFileNames(uploadElement);
                boolean fileSelected = files != null && files.length() > 0;
                submitButton.setEnabled(fileSelected);
                previewButton.setEnabled(fileSelected);
                cancelButton.setText("Cancel");
            }
        });
        uploadElement = fileUpload.getElement();
		// Create the HTML5 multiple-file upload in the HTML <div>
//		dataUpload.setHTML("<input type=\"file\" name=\"datafiles\" " + "id=\"datafiles\" style=\"width: 100%;\" multiple />");
		// Get the multiple file input element within the HTML <div>
//		uploadElement = dataUpload.getElement();
		for (int k = 0; k < uploadElement.getChildCount(); k++) {
			Element childElem = (Element) uploadElement.getChild(k);
			if ( "datafiles".equals(childElem.getId()) ) {
				uploadElement = childElem;
				break;
			}
		}

		clearTokens();

		submitButton.setText(SUBMIT_TEXT);
        submitButton.setEnabled(false); // files and feature type must be selected.
        previewButton.setEnabled(false); // files and feature type must be selected.
		cancelButton.setText(CANCEL_TEXT);

		advancedHtml.setHTML(ADVANCED_HTML_MSG);
		encodingLabel.setText(ENCODING_TEXT);
		encodingListBox.setVisibleItemCount(1);
		for ( String encoding : KNOWN_ENCODINGS )
			encodingListBox.addItem(encoding);
		previewButton.setText(PREVIEW_TEXT);
	}
    
	/**
	 * Display the cruise upload page in the RootLayoutPanel
	 * after clearing as much of the page as possible.  
	 * The upload filename cannot be cleared. 
	 * Adds this page to the page history.
	 */
	static void showPage(String username) {
		if ( singleton == null )
			singleton = new DataUploadPage();
		singleton.setUsername(username);
		singleton.clearTokens();
        singleton.clearForm();
        singleton.cruiseIDs.clear();
        singleton.wasActuallyOk = false;
        singleton.featureTypeSelector.removeStyleName("missingInfoItem");
        singleton.featureTypeSelector.setSelectedIndex(0);
		singleton.encodingListBox.setSelectedIndex(2);
		singleton.advancedPanel.setOpen(false);
        singleton.header.userInfoLabel.setText(WELCOME_INTRO + username);
		UploadDashboard.updateCurrentPage(singleton, true);
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
	 * Assigns the values of the Hidden tokens on the page.
	 * 
	 * @param requestAction
	 * 		action to request (value to assign to the actionToken)
	 */
	private void assignTolkiens(String requestAction) {
		String localTimestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm Z").format(new Date());
		String encoding = KNOWN_ENCODINGS[encodingListBox.getSelectedIndex()];
		timestampToken.setValue(localTimestamp);
		actionToken.setValue(requestAction);
		encodingToken.setValue(encoding);
        observationTypeToken.setValue(observationType);
	}

	/**
	 * Clears the values of the Hidden tokens on the page.
	 */
	private void clearTokens() {
		timestampToken.setValue("");
		actionToken.setValue("");
		encodingToken.setValue("");
	}

    void closePopups() {
        if ( moreHelpPopup != null ) moreHelpPopup.dismiss();
        if ( featureTypeHelpPopup != null ) featureTypeHelpPopup.dismiss();
    }
    
	@UiHandler("moreHelpAnchor")
	void moreHelpOnClick(ClickEvent event) {
		// Create the popup only when needed and if it does not exist
		if ( moreHelpPopup == null ) {
			moreHelpPopup = new DashboardInfoPopup();
			moreHelpPopup.setInfoMessage(MORE_HELP_HTML);
		}
		moreHelpPopup.showCentered();
	}
    @UiHandler("featureTypeHelpAnchor")
    void featureTypeHelpOnClick(ClickEvent event) {
        // Create the popup only when needed and if it does not exist
        if ( featureTypeHelpPopup == null ) {
            featureTypeHelpPopup = new DashboardInfoPopup();
            featureTypeHelpPopup.setInfoMessage(FEATURE_TYPE_HELP_HTML);
        }
        featureTypeHelpPopup.showCentered();
    }

	/**
	 * @param input
	 * 		multiple file input HTML element
	 * @return
	 * 		a " ; "-separated list of the filenames given 
	 * 		in the multiple file input HTML element
	 */
	private static native String getInputFileNames(Element input) /*-{
        var namesString = "";

        // Just in case not multiple
        if ( typeof (input.files) == 'undefined' || 
             typeof (input.files.length) == 'undefined') {
            return input.value;
        }

        for (var k = 0; k < input.files.length; k++) {
            if ( k > 0 ) {
                namesString += " ; ";
            }
            namesString += input.files[k].name;
        }
        return namesString;
	}-*/;

	private static native void clearInputFileNames(Element input) /*-{
        if ( typeof (input.files) == 'undefined' || 
             typeof (input.files.length) == 'undefined') {
            input.value = 'undefined';
        } else {
           
        }
    }-*/;
	
	@UiHandler("previewButton") 
	void previewButtonOnClick(ClickEvent event) {
		String namesString = getInputFileNames(uploadElement).trim();
		if (  namesString.isEmpty() ) {
			UploadDashboard.showMessage(NO_FILE_ERROR_MSG);
			return;
		}
		assignTolkiens(DashboardUtils.PREVIEW_REQUEST_TAG);
		uploadForm.submit();
	}

	@UiHandler("submitButton") 
	void submitButtonOnClick(ClickEvent event) {
        submitButton.setEnabled(false);
		String namesString = getInputFileNames(uploadElement).trim();
		int size = getFileSize(uploadElement);
		UploadDashboard.logToConsole("Submitting file " + namesString + " of size " + size);
		if (  namesString.isEmpty() ) {
			UploadDashboard.showMessage(NO_FILE_ERROR_MSG);
			return;
		}
        String errorMessage = null;
        if ( featureTypeSelector.getSelectedIndex() <= 0 ) {
            featureTypeSelector.addStyleName("missingInfoItem");
            errorMessage = SELECT_FEATURE_TYPE_MSG;
        }
        if ( errorMessage != null ) {
			UploadDashboard.showMessage(errorMessage);
            submitButton.setEnabled(true);
            return;
        }
//		if ( overwriteRadio.getValue().booleanValue() )
//			assignTolkiens(DashboardUtils.OVERWRITE_DATASETS_REQUEST_TAG);
//		else if ( appendRadio.getValue() )
//			assignTokens(DashboardUtils.APPEND_DATASETS_REQUEST_TAG);
//		else 
			assignTolkiens(DashboardUtils.NEW_DATASETS_REQUEST_TAG);
		uploadForm.submit();
	}

	@UiHandler("cancelButton")
	void cancelButtonOnClick(ClickEvent event) {
		// Return to the cruise list page after updating the cruise list
        closePopups();
        DatasetListPage.showPage();
		// Make sure the normal cursor is shown
		UploadDashboard.showAutoCursor();
	}

	@UiHandler("uploadForm")
	void uploadFormOnSubmit(SubmitEvent event) {
		UploadDashboard.showWaitCursor();
	}

    private void clearForm() {
        uploadForm.reset();
        clearInputFileNames(uploadElement);
        featureTypeSelector.setSelectedIndex(0);
        submitButton.setEnabled(false); 
        previewButton.setEnabled(false); 
        cancelButton.setText("Done");
    }
        
	private ArrayList<String> cruiseIDs = new ArrayList<String>();
    private boolean wasActuallyOk = false;
	@UiHandler("uploadForm")
	void uploadFormOnSubmitComplete(SubmitCompleteEvent event) {
        wasActuallyOk = processResultMsg(event.getResults());
        UploadDashboard.logToConsole("Upload was successful: " + wasActuallyOk);
		clearTokens();
        if ( wasActuallyOk ) {
            clearForm();
			DatasetListPage.resortTable();
            DatasetListPage.showPage(cruiseIDs);
		}
		UploadDashboard.showAutoCursor();
	}

	/**
	 * Process the message returned from the upload of a data file.
	 * 
	 * @param resultMsg
	 * 		message returned from the upload of a dataset
	 */
	private boolean processResultMsg(String resultMsg) {
        UploadDashboard.logToConsole("Upload result msg:" + resultMsg + ".");
		// Check the returned results
		if ( resultMsg == null || resultMsg.trim().length() == 0 ) {
		    if ( wasActuallyOk ) {
                return true;
    		} 
			UploadDashboard.showMessage(UNEXPLAINED_FAIL_MSG);
			return false;
		}
		String[] splitMsgs = resultMsg.trim().split("\n");

		// Preview is a special case - the start of the first file is returned
		if ( splitMsgs[0].startsWith(DashboardUtils.FILE_PREVIEW_HEADER_TAG) ) {
			// show partial file contents in the preview
			String previewMsg = "<pre>\n";
			for (int k = 1; k < splitMsgs.length; k++) {
				// Some clean-up: remove the javascript that is added by the firewall
				if ( splitMsgs[k].trim().startsWith(JAVASCRIPT_START) ) {
					do {
						k++;
						if ( k >= splitMsgs.length )
							break;
					} while ( ! splitMsgs[k].trim().startsWith(JAVASCRIPT_CLOSE) );
				}
				else {
					previewMsg += SafeHtmlUtils.htmlEscape(splitMsgs[k]) + "\n";
				}
			}
			previewMsg += "</pre>";
            int wHeight = Window.getClientHeight();
            int cHeight = (int)(.6 * wHeight);
            int wWidth = Window.getClientWidth();
            int cWidth = (int)(.7 * wWidth);
            UploadDashboard.showDataPreviewPopup(previewMsg, cHeight, cWidth);
			return false;
		}

		ArrayList<String> errMsgs = new ArrayList<String>();
		for (int k = 0; k < splitMsgs.length; k++) {
			String responseMsgItem = splitMsgs[k].trim();
			if ( responseMsgItem.startsWith(DashboardUtils.SUCCESS_HEADER_TAG) ) {
				// Success
                String cruiseId = responseMsgItem.substring(DashboardUtils.SUCCESS_HEADER_TAG.length()).trim();
				cruiseIDs.add(cruiseId);
                GWT.log("successful upload: " + cruiseId);
			}
			else if ( responseMsgItem.startsWith(DashboardUtils.INVALID_FILE_HEADER_TAG) ) {
				// An exception was thrown while processing the input file
				String filename = responseMsgItem.substring(DashboardUtils.INVALID_FILE_HEADER_TAG.length()).trim();
				String failMsg = FAIL_MSG_START + SafeHtmlUtils.htmlEscape(filename) + EXPLAINED_FAIL_MSG_START;
				for (k++; k < splitMsgs.length; k++) {
					if ( splitMsgs[k].trim().startsWith(DashboardUtils.END_OF_ERROR_MESSAGE_TAG) )
						break;
					failMsg += SafeHtmlUtils.htmlEscape(splitMsgs[k]) + "\n";
				}
				errMsgs.add(failMsg);
				errMsgs.add(EXPLAINED_FAIL_MSG_END);
				errMsgs.add(UPLOAD_FAILED_SUGGESTIONS);
			}
			else if ( responseMsgItem.startsWith(DashboardUtils.DATASET_EXISTS_HEADER_TAG) ) {
				// Dataset file exists and not permitted to modify
				String[] info = responseMsgItem.substring(DashboardUtils.DATASET_EXISTS_HEADER_TAG.length()).trim().split(" ; ", 4);
				String failMsg = FAIL_MSG_START;
				if ( info.length > 1 ) 
					failMsg += SafeHtmlUtils.htmlEscape(info[1].trim()) + " - ";
				failMsg += SafeHtmlUtils.htmlEscape(info[0].trim());
				failMsg += DATASET_EXISTS_FAIL_MSG_START;
				if ( info.length > 2 )
					failMsg += "<p>&nbsp;&nbsp;&nbsp;&nbsp;Owner = " + SafeHtmlUtils.htmlEscape(info[2].trim()) + "</p>";
				if ( info.length > 3 )
					failMsg += "<p>&nbsp;&nbsp;&nbsp;&nbsp;Submit Status = " + SafeHtmlUtils.htmlEscape(info[3].trim()) + "</p>";
				errMsgs.add(failMsg + DATASET_EXISTS_FAIL_MSG_END); 
			}
			else if ( responseMsgItem.startsWith(JAVASCRIPT_START) ) {
				// ignore the added javascript from the firewall
				do {
					k++;
					if ( k >= splitMsgs.length )
						break;
				} while ( ! splitMsgs[k].trim().startsWith(JAVASCRIPT_CLOSE) );
			} else if ( resultMsg.indexOf("SESSION HAS EXPIRED") >= 0 ) {
			    UploadDashboard.showLoginPopup();
                return false; // XXX mid-method bail-out. yuk.
			} else {
				//  some other error message, display the whole message and be done with it
				String failMsg = "<pre>";
				do {
					failMsg += SafeHtmlUtils.htmlEscape(splitMsgs[k]) + "\n";
					k++;
				} while ( k < splitMsgs.length );
				errMsgs.add(failMsg + "</pre>");
			}
		}

		boolean wasCompleteSuccess = errMsgs.size() == 0;
		
		// Display any error messages from the upload
		if ( errMsgs.size() > 0 ) {
			String errors = "";
			for ( String msg : errMsgs ) 
				errors += msg;
			UploadDashboard.showMessage(errors);
		}

		return wasCompleteSuccess;
	}

    /**
     * @param value
     */
    public void setDatasetIdToken(String value) {
        datasetIdToken.setValue(value);
    }
    
    public void setDatasetIdColumnNameToken(String value) {
        datasetIdColumnToken.setValue(value);
    }

    /**
     * @param name
     */
    public void setFileDataFormatToken(String name) {
        fileDataFormatToken.setValue(name);
    }
}
