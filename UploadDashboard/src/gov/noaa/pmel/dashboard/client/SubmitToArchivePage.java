/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.FileType;
import gov.noaa.pmel.dashboard.shared.QCFlag;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;

/**
 * @author kamb
 *
 */
public class SubmitToArchivePage extends CompositeWithUsername implements DataSubmissionPage {

	private static SubmitToArchivePage singleton = null;
	
	private static enum SELECT {
		NONE,
		MIN,
		REQUIRED,
		STANDARD,
		USER,
		ALL
	}
	
    @UiField HTML fileListLabel;
    @UiField HTML fileListHtml;
    @UiField HTML submitCommentLabel;
    @UiField HTML statusLabel;
    @UiField Label submissionTime;
    @UiField HTML statusListPanel;
    @UiField TextArea submitCommentTextArea;
//	@UiField Tree columnTree;
//	@UiField HTML introHtml;
//	@UiField Panel columnsPanel;
//	@UiField DataGrid<DataColumnType> columnsGrid;
    @UiField FlowPanel messagesPanel;
    @UiField Label messagesText;
    @UiField HorizontalPanel versionSubmitPanel;
    @UiField CheckBox versionSubmitChkBx;
    @UiField Label versionSubmitText;
    boolean versionCheck;
    @UiField HorizontalPanel dataIssuesPanel;
    @UiField CheckBox dataIssuesChkBx;
    @UiField Label dataIssuesText;
    boolean dataIssuesCheck;
    @UiField HorizontalPanel metadataIssuesPanel;
    @UiField CheckBox metadataIssuesChkBx;
    @UiField Label metadataIssuesText;
    boolean metadataIssuesCheck;
    @UiField HorizontalPanel genDoiPanel;
    @UiField CheckBox genDoiChkBx;
    @UiField Label genDoiText;
    @UiField HorizontalPanel policyAgreementPanel;
    @UiField CheckBox policyAgreementChkBx;
    @UiField Label policyAgreementText;
    @UiField Anchor showPolicyAgreement;
    
    boolean blocked = false; // Cannot submit.
    
	@UiField Button cancelButton;
	@UiField Button submitButton;
	
//	@UiField RadioButton select_none;
////	@UiField RadioButton select_min;
////	@UiField RadioButton select_req;
//	@UiField RadioButton select_std;
////	@UiField RadioButton select_user;
//	@UiField RadioButton select_all;
    
	@UiField ApplicationHeaderTemplate header;
	
	private String _datasetId;
	private DashboardDataset _dataset;
	private List<String> _submitIdsList;
	private List<String> _submitColsList;

	private List<CheckBox> _allCBoxes;
	private ArrayList<String> _userColNames;
	private ArrayList<DataColumnType> _colTypes;
	private Map<String, CheckBox> _columnBoxMap;
	private DashboardDatasetList _datasets;
	
	interface SubmitToArchivePageUiBinder extends UiBinder<Widget, SubmitToArchivePage> {
	}

	private static SubmitToArchivePageUiBinder uiBinder = 
			GWT.create(SubmitToArchivePageUiBinder.class);

	private static DashboardServicesInterfaceAsync service = 
			GWT.create(DashboardServicesInterface.class);

    private static final String submitCommentHtml =
        "<div id='submitCommentLabelHead'>Optional Submission Comment:</div>"
        + "<div id='submitCommentLabelExp'>"
        + "<div id='submitCommentLabelLine1'>This optional comment will not be archived.<br/></div>"
        + "<div id='submitCommentLabelLine2'>Do not use this comment to include metadata or other important dataset information.<br/></div>"
        + "<div id='submitCommentLabelLine3'>Its use is solely to communicate special information or archiving considerations to the archive staff.</div>"
        + "</div>";
    
    private ValueChangeHandler<Boolean> agreementHandler = new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> event) {
            submitButton.setEnabled(event.getValue().booleanValue());
        }
    };
    private ValueChangeHandler<Boolean> noticesHandler = new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> event) {
            GWT.log(event.getSource() + " changed to " + event.getValue());
            boolean enablePolicy = 
                 ( ! versionCheck || versionSubmitChkBx.getValue().booleanValue()) &&
                 ( ! dataIssuesCheck || dataIssuesChkBx.getValue().booleanValue()) &&
                 ( ! metadataIssuesCheck || metadataIssuesChkBx.getValue().booleanValue());
            enablePolicyAgreement(enablePolicy);
        }
    };
	public SubmitToArchivePage() {
        super(PagesEnum.SUBMIT_TO_ARCHIVE.name());
		initWidget(uiBinder.createAndBindUi(this));
		setupHandlers();
        
		header.setPageTitle("Submit Datasets for Archving");
        
        fileListLabel.setText("The following files will be archived at NCEI:");
        submitCommentLabel.setHTML(submitCommentHtml);
        
        messagesPanel.setVisible(false);
        versionSubmitPanel.setVisible(false);
        dataIssuesPanel.setVisible(false);
        metadataIssuesPanel.setVisible(false);
        
        showPolicyAgreement.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                event.preventDefault();
                Window.alert("Show policy agreement");
            }
        });
        
		cancelButton.setText("Cancel");
		cancelButton.setTitle("Cancel");
		submitButton.setText("Submit");
		submitButton.setTitle("Submit");
		
        versionSubmitChkBx.addValueChangeHandler(noticesHandler);
        dataIssuesChkBx.addValueChangeHandler(noticesHandler);
        metadataIssuesChkBx.addValueChangeHandler(noticesHandler);
        policyAgreementChkBx.addValueChangeHandler(agreementHandler);
		
		_allCBoxes = new ArrayList<>();
		_columnBoxMap = new HashMap<>();
		_submitIdsList = new ArrayList<>();
		_submitColsList = new ArrayList<>();
	}
	
	public final class RadioButtonClickHandler implements ClickHandler {

		private SELECT select;
		
		RadioButtonClickHandler(SELECT select) {
			this.select = select;
		}
		
		@Override
		public void onClick(ClickEvent event) {
			GWT.log(select + ": " + event);
			clearSelections();
			switch (select) {
				case NONE:
					break;
//				case MIN:
//					setSelection(getColumns(SELECT.MIN), true);
//					break;
//				case REQUIRED:
//					setSelection(getColumns(SELECT.REQUIRED), true);
//					break;
				case STANDARD:
					setSelection(getColumns(SELECT.STANDARD), true);
					break;
//				case User:
//					break;
				case ALL:
					setAll();
					break;
			}
		}


	}
	
	private List<String> minSet = Arrays.asList(new String[] { "dataset_name", 
	            "date", "time_of_day", "longitude", "latitude", 
	            "station", "niskin",
		        });
	private List<String> stdSet = join(minSet,  new String[] { 
	            "ctd_pressure", "ctd_temperature", "ctd_salinity", "ctd_density", 
	            "ctd_oxygen", "ctd_oxygen_qc",
	            "oxygen", "oxygen_qc",
	            "ctd_salinity", "ctd_salinity_qc",
	            "salinity", "salinity_qc",
	            "inorganic_carbon", "inorganic_carbon_qc",
	            "alkalinity", "alkalinity_qc",
	            "ph_total", "ph_total_qc",
	            "ph_temperature", "ph_temperature_qc",
	            "ammonia", "ammonia_qc", 
	            "pyrex",
	            "nitrite", "nitrite_qc",
	            "nitrate", "nitrate_qc",
	            "silicate", "silicate_qc",
	            "phosphate", "phosphate_qc",
	            "carbonate_ion", "carbonate_ion_qc"
	            });
//	private List<String> userSet;
	
	private static List<String> join(List<String>list, String[] array) {
		List<String> copy = new ArrayList<>(list);
		copy.addAll(Arrays.asList(array));
		return copy;
	}
	
	private static List<String> join(String[] list, String[] array) {
		List<String> copy = Arrays.asList(array);
		copy.addAll(Arrays.asList(array));
		return copy;
	}
	
	private ArrayList<String> getColumns(SELECT columnSet) {
		ArrayList<String> columns = new ArrayList<>();
		switch (columnSet) {
//			case MIN:
//				columns.addAll(minSet);
//				break;
//			case REQUIRED:
//				columns.addAll(reqSet);
//				break;
			case STANDARD:
				columns.addAll(stdSet);
				break;
//			case User:
//				columns.addAll(userSet);
//				break;
			case ALL:
				setAll();
				break;
			default:
				break;
		}
		return columns;
	}

	private void setSelection(List<String> colNames, boolean b) {
		for (String col : colNames) {
			CheckBox box = _columnBoxMap.get(col);
			if ( box == null ) {
				GWT.log("No checkbox found for column : " + col);
			} else {
				box.setValue(b);
			}
		}
	}
	
	private void clearSelections() {
		for (CheckBox box : _allCBoxes) {
			box.setValue(Boolean.FALSE);
		}
	}
	
	private void setAll() {
		for (CheckBox box : _allCBoxes) {
			box.setValue(Boolean.TRUE);
		}
	}
	
	private void setupHandlers() {
//		select_none.addClickHandler(new RadioButtonClickHandler(SELECT.NONE));
////		select_min.addClickHandler(new RadioButtonClickHandler(SELECT.MIN));
////		select_req.addClickHandler(new RadioButtonClickHandler(SELECT.REQUIRED));
//		select_std.addClickHandler(new RadioButtonClickHandler(SELECT.STANDARD));
////		select_user.addClickHandler(new RadioButtonClickHandler(SELECT.USER));
//		select_all.addClickHandler(new RadioButtonClickHandler(SELECT.ALL));
	}

    static void redisplayPage(String username) {
        if ( (username == null) || username.isEmpty() || 
             (singleton == null) || ! singleton.getUsername().equals(username) ) {
            DatasetListPage.showPage();
        }
        else {
            UploadDashboard.updateCurrentPage(singleton);
        }
    }

	static void showPage(DashboardDatasetList datasets) {
		if ( singleton == null ) {
			singleton = new SubmitToArchivePage();
		}
		singleton._datasets = datasets;
        singleton.header.addDatasetIds(datasets);
        singleton.setUsername(datasets.getUsername());
		singleton.updateDatasetColumns(datasets);
        singleton.reset();
        if ( alreadySubmitted(datasets)) {
            singleton.showVersionSubmitConfirmation(datasets);
        }
        if ( dataIssues(datasets) ) {
            singleton.showDataIssuesConfirmation(datasets);
        }
        if ( metadataIssues(datasets)) {
            singleton.showBlockingMessage("The metadata is incomplete. You must provide minimal metadata to submit to the archive.");
            singleton.blocked = true;
        }
        if ( singleton.blocked ) {
            singleton.enableChecks(false);
            singleton.enablePolicyAgreement(false);
        }
        getStatus(singleton);
		UploadDashboard.updateCurrentPage(singleton, UploadDashboard.DO_PING);
	}
    
    /**
     * 
     */
    private void reset() {
        submitButton.setEnabled(false);
        cancelButton.setText("Cancel");
        blocked = false;
        versionCheck = false;
        dataIssuesCheck = false;
        metadataIssuesCheck = false;
        enableChecks(true);
        enablePolicyAgreement(true);
        messagesText.removeStyleName("blocking");
        messagesPanel.setVisible(false);
        versionSubmitPanel.setVisible(false);
        dataIssuesPanel.setVisible(false);
        metadataIssuesPanel.setVisible(false);
    }

    /**
     * 
     */
    private void enableChecks(boolean enable) {
        genDoiChkBx.setEnabled(enable);
        versionSubmitChkBx.setEnabled(enable);
        dataIssuesChkBx.setEnabled(enable);
        metadataIssuesChkBx.setEnabled(enable);
        if ( enable ) {
            genDoiText.removeStyleName("gray");
            versionSubmitText.removeStyleName("gray");
            dataIssuesText.removeStyleName("gray");
            metadataIssuesText.removeStyleName("gray");
        } else {
            genDoiText.addStyleName("gray");
            versionSubmitText.addStyleName("gray");
            dataIssuesText.addStyleName("gray");
            metadataIssuesText.addStyleName("gray");
        }
    }
    private void enablePolicyAgreement(boolean enable) {
        policyAgreementChkBx.setEnabled(enable);
        showPolicyAgreement.setEnabled(enable);
        if ( enable ) {
            policyAgreementText.removeStyleName("gray");
        } else {
            policyAgreementText.addStyleName("gray");
        }
    }

    /**
     * @param string
     */
    private void showBlockingMessage(String msgText) {
        showMessage(msgText);
        messagesText.setStyleName("blocking", true);
    }
    private void showMessage(String msgText) {
        messagesText.setText(msgText);
        messagesPanel.setVisible(true);
    }

    /**
     * @param datasets
     * @return
     */
    private static boolean alreadySubmitted(DashboardDatasetList datasets) {
        for (DashboardDataset dataset : datasets.values()) {
            Date archiveDate = dataset.getArchiveDate();
            if ( archiveDate != null ) {
                String uploadTimestamp = dataset.getUploadTimestamp();
                Date uploadDate = DateTimeFormat.getFormat("yyyy-MM-dd hh:mm Z").parse(uploadTimestamp);
                return uploadDate.before(archiveDate);
            }
        }
        return false;
    }

    /**
     * @param datasets
     */
    private void showVersionSubmitConfirmation(DashboardDatasetList datasets) {
        versionSubmitText.setText("Dataset has already been submitted. Check to confirm resbmission.");
        versionSubmitPanel.setVisible(true);
        versionCheck = true;
        enableSubmit(false);
    }

    private void enableSubmit(boolean enable) {
        policyAgreementChkBx.setEnabled(enable);
        submitButton.setEnabled(enable);
    }
    
    /**
     * @param datasets
     * @return
     */
    private static boolean dataIssues(DashboardDatasetList datasets) {
        for (DashboardDataset dataset : datasets.values()) {
            int nErrRows = dataset.getNumErrorRows();
            int nWarnRows = dataset.getNumWarnRows();
            int nChkWarn = 0;
            int nChkErr = 0;
            int nChkCrit = 0;
            for (QCFlag f : dataset.getCheckerFlags()) {
                Severity s = f.getSeverity();
                switch (s) {
                    case CRITICAL:
                        nChkCrit += 1;
                        break;
                    case ERROR:
                        nChkErr += 1;
                        break;
                    case WARNING:
                        nChkWarn += 1;
                        break;
                    default:
                }
            }
            GWT.log("nErr:"+nErrRows+",nWarn:"+nWarnRows+",chkCrit:"+nChkCrit+",chkErr:"+nChkErr+",chkWarn:"+nChkWarn);
            if ( nErrRows > 0 ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param datasets
     */
    private void showDataIssuesConfirmation(DashboardDatasetList datasets) {
        dataIssuesText.setText("The dataset has data errors. Check to confirm submission with errors.");
        dataIssuesPanel.setVisible(true);
        dataIssuesCheck = true;
        enableSubmit(false);
    }

    /**
     * @param datasets
     * @return
     */
    private static boolean metadataIssues(DashboardDatasetList datasets) {
        for (DashboardDataset dataset : datasets.values()) {
            if ( checkMetadata(dataset).length > 0 ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param datasets
     */
    private void showMetadataIssuesConfirmation(DashboardDatasetList datasets) {
        metadataIssuesText.setText("The dataset has metadata issues. Check to confirm submit with errors.");
        metadataIssuesPanel.setVisible(true);
        metadataIssuesCheck = true;
    }
    
    // COPIED from DatasetListPage, where it's not currently being used...
    private static final String[] EMPTY_MESSAGES = new String[0];
	private static boolean checkDatasetsForSubmitting(DashboardDatasetList checkSet) {
        boolean okToSubmit = true;
        StringBuilder errorMsgBldr = new StringBuilder("The following problems were found:");
        errorMsgBldr.append("<ul>");
        
		for ( DashboardDataset dataset : checkSet.values() ) {
            boolean thisOneIsOk = true;
            String[] errorMessages;
            errorMsgBldr.append("<li>").append(dataset.getDatasetId())
                        .append("<ul>");
            errorMessages = checkMetadata(dataset);
            if ( errorMessages.length > 0 ) {
                addErrorMessages(errorMsgBldr, errorMessages);
//                addMetadataCheck();
                thisOneIsOk = false;
            }
            errorMessages = dataCheck(dataset);
            if ( errorMessages.length > 0 ) {
                addErrorMessages(errorMsgBldr, errorMessages);
                thisOneIsOk = false;
            }
            errorMessages = columnCheck(dataset);
            if ( errorMessages.length > 0 ) {
                addErrorMessages(errorMsgBldr, errorMessages);
                thisOneIsOk = false;
            }
            errorMsgBldr.append("</ul></li>");
            okToSubmit = okToSubmit && thisOneIsOk;
		}
        errorMsgBldr.append("</ul>");
        if ( !okToSubmit ) {
            UploadDashboard.showMessage(errorMsgBldr.toString());
        }
        return okToSubmit;
	}
    /**
     * @param dataset
     * @return
     */
    private static String[] checkMetadata(DashboardDataset dataset) {
        String[] messages = EMPTY_MESSAGES;
        String status = dataset.getMdStatus();
        if ( dataset.getMdTimestamp().isEmpty() ||
             status.isEmpty() || 
             status.contains("incomplete")) {
            messages = new String[] { "Missing or incomplete metadata." };
        }
        return messages;
    }

    /**
     * @param dataset
     * @return
     */
    private static String[] dataCheck(DashboardDataset dataset) {
        String status = dataset.getDataCheckStatus();
        if ( FileType.OTHER.equals(dataset.getFileType()) 
             || DashboardUtils.CHECK_STATUS_ACCEPTABLE.equals(status)) {
            return EMPTY_MESSAGES;
        }
        if ( DashboardUtils.CHECK_STATUS_NOT_CHECKED.equals(status)) {
            return new String[] { "Dataset has not been checked." };
        }
        if ( status.contains("error")) {
            return new String[] { "Dataset has data validation errors." };
        }
        return EMPTY_MESSAGES;
    }

	/**
     * @param dataset
     * @return
     */
    private static String[] columnCheck(DashboardDataset dataset) {
        if ( ! hasNoUnknownColumns(dataset)) {
            return new String[] { "Dataset has Unknown columns." };
        }
        return EMPTY_MESSAGES;
    }
    
	/**
     * @param dd
     * @return
     */
    private static boolean hasNoUnknownColumns(DashboardDataset dd) {
        for (DataColumnType col : dd.getDataColTypes()) {
            if ( col.typeNameEquals(DashboardUtils.UNKNOWN)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param errorMsgBldr
     * @param errorMessages
     */
    private static void addErrorMessages(StringBuilder errorMsgBldr, String[] errorMessages) {
        for ( String msg : errorMessages ) {
            errorMsgBldr.append("<li>").append(SafeHtmlUtils.htmlEscape(msg)).append("</li>");
        }
    }


    private static void getSubmitStatus(SubmitToArchivePage page) {
        page.submissionTime.setText("Package submitted.");
        service.getPackageArchiveStatus(page.getUsername(), page._datasetId, 
			new AsyncCallback<String>() {
				@Override
				public void onSuccess(String htmlList) {
                    UploadDashboard.logToConsole("getStatus : " + htmlList);
                    page.statusListPanel.setHTML(htmlList);
				}
				@Override
				public void onFailure(Throwable ex) {
                    UploadDashboard.logToConsole(String.valueOf(ex));
                    page.statusListPanel.setHTML("There was a problem retrieving package archive status.");
				}
			});
		UploadDashboard.showMessageWithContinuation("Dataset submitted.", new OAPAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void arg0) {
                DatasetListPage.showPage();
            }
        });
    }
    private static void getStatus(SubmitToArchivePage page) {
        UploadDashboard.logToConsole("getStatus for " + page._datasetId);
        page.statusLabel.setHTML("<h3>Submission status for " + page._datasetId + "</h3>");
        DashboardDataset dd = page._dataset;
        Date archiveDate = dd.getArchiveDate();
        if ( archiveDate == null ) {
            page.submissionTime.setText("Package not yet submitted.");
            page.statusListPanel.setHTML("");
        } else {
            String dateStr = dd.getArchiveDateAsString(DashboardUtils.LOCALIZED_DATE_FORMAT_TO_MINUTES);
            page.submissionTime.setText("Package submitted on " + dateStr.substring(0, dateStr.lastIndexOf(' ')));
            service.getPackageArchiveStatus(page.getUsername(), page._datasetId, 
    			new AsyncCallback<String>() {
    				@Override
    				public void onSuccess(String htmlList) {
                        UploadDashboard.logToConsole("getStatus : " + htmlList);
                        page.statusListPanel.setHTML(htmlList);
    				}
    				@Override
    				public void onFailure(Throwable ex) {
                        UploadDashboard.logToConsole(String.valueOf(ex));
                        page.statusListPanel.setHTML("There was a problem retrieving package archive status.");
    				}
    			});
        }
    }
    
	protected void updateDatasetColumns(DashboardDatasetList datasets) {
	    _submitIdsList.clear();
//	    select_none.setValue(false);
//	    select_std.setValue(false);
//	    select_all.setValue(false);
	    clearSelections();
	    if ( ! okToArchive(datasets)) {
	        UploadDashboard.showMessage("All datasets selected for archival must have the same set of columns.");
	        return;
	    }
		for (DashboardDataset dd : datasets.values()) {
//			if ( okToArchive(dd)) {
				_submitIdsList.add(dd.getDatasetId());
//			} else {
//				_skipList.add(dd);
//			}
		}
		
		_dataset = datasets.values().iterator().next();
	    if ( _dataset.getDatasetId().equals(_datasetId)) {
	        return;
	    }
		_datasetId = _dataset.getDatasetId();
		header.userInfoLabel.setText(WELCOME_INTRO + getUsername());
		
        submitCommentTextArea.setText(_dataset.getArchiveSubmissionMessage());
        genDoiChkBx.setValue(_dataset.getArchiveDOIrequested());
//		columnsPanel.clear();
        
        setFilesToBeArchived(_dataset);
        
//        setDataColumns(dataset);
	}

	/**
     * @param dataset
     */
    private void setFilesToBeArchived(DashboardDataset dataset) {
        StringBuilder b = new StringBuilder();
        b.append("<ul>");
        addLine(b, "Data File", dataset.getUploadFilename());
        addLine(b, "Metadata File", dataset.getDatasetId()+"_OADS.xml");
        if ( dataset.getAddlDocs().size() > 0 ) {
            b.append("<li>Supplemental Documents<ul>");
            for (String f : dataset.getAddlDocs()) {
                addFile(b, f);
            }
            b.append("</ul></li>");
        }
        b.append("</ul>");
        fileListHtml.setHTML(b.toString());
    }

    /**
     * @param b 
     * @param string
     * @param datasetId
     */
    private void addLine(StringBuilder b, String title, String filename) {
        b.append("<li>").append(title).append(" : ").append(filename).append("</li>");
    }

    /**
     * @param b 
     * @param f
     */
    private void addFile(StringBuilder b, String filename) {
        b.append("<li>").append(filename).append("</li>");
    }

    /**
     * @param dataset
     */
    private void setDataColumns(DashboardDataset dataset) {
		_allCBoxes.clear();
		_columnBoxMap.clear();
		_userColNames = dataset.getUserColNames(); 
		_colTypes = dataset.getDataColTypes();
		GWT.log("sizes:"+_userColNames.size() + ", " + _colTypes.size());
		for (int i = 0; i < _userColNames.size(); i++) {
			String userColName = _userColNames.get(i);
			CheckBox cb = new CheckBox(userColName);
			DataColumnType colType = _colTypes.get(i);
			String varName = colType.getVarName();
			GWT.log(userColName + " : " + varName);
			cb.setName(userColName);
			_allCBoxes.add(cb);
			_columnBoxMap.put(varName, cb);
//			columnsPanel.add(cb);
		}
        
    }

    private boolean okToArchive(DashboardDatasetList datasets) {
	    boolean isOk = true;
	    
        return isOk; // XXX
    }
	
    @UiHandler("cancelButton")
    static void cancelOnClick(ClickEvent event) {
		// Return to the list of cruises which could have been modified by this page
		DatasetListPage.showPage();
	}

	@UiHandler("submitButton")
	void submitOnClick(ClickEvent event) {
	    
        submitButton.setEnabled(false);
	    // check that all datasets (if more than 1 selected) have the selected columns
//	    if ( checkDatasetColumns()) {
	        continueSubmit();
//	    } else {
//	        UploadDashboard.showMessage("Inconsistent columns");
//	    }
	    
//		if ( ! agreeShareCheckBox.getValue() ) {
//			UploadDashboard.showMessageAt(AGREE_SHARE_REQUIRED_MSG, agreeShareCheckBox);
//			return;
//		}
//		if ( hasSentDataset && nowRadio.getValue() ) {
//			// Asking to submit to CDIAC now, but has a cruise already sent
//			if ( resubmitAskPopup == null ) {
//				resubmitAskPopup = new DashboardAskPopup(YES_RESEND_TEXT, 
//						NO_CANCEL_TEXT, new AsyncCallback<Boolean>() {
//					@Override
//					public void onSuccess(Boolean okay) {
//						// Continue setting the archive status (and thus, 
//						// sending the request to CDIAC) only if user okays it
//						if ( okay ) {
//							continueSubmit();
//						}
//					}
//					@Override
//					public void onFailure(Throwable ex) {
//						// Never called
//						;
//					}
//				});
//			}
//			resubmitAskPopup.askQuestion(REARCHIVE_QUESTION);
//		}
//		else {
//			// Either no cruises sent to CDIAC, or not a send to CDIAC now request. 
//			// Continue setting the archive status.
//			continueSubmit();
//		}
//		DatasetListPage.showPage();
//		continueSubmit();
	}

	/**
	 * Submits cruises and updated archival selection
	 */
	void continueSubmit() {
		String localTimestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm Z").format(new Date());
		String archiveStatus = null;
		buildSubmitColumnList();
//		if ( laterRadio.getValue() ) {
//			// Archive with the next release
//			archiveStatus = DashboardUtils.ARCHIVE_STATUS_WITH_NEXT_RELEASE;
//		}
//		else if ( nowRadio.getValue() ) {
//			// Archive now
			archiveStatus = DashboardUtils.ARCHIVE_STATUS_SENT_FOR_ARCHIVAL;
//		}
//		else if ( ownerRadio.getValue() ) {
//			// Owner will archive
//			archiveStatus = DashboardUtils.ARCHIVE_STATUS_OWNER_TO_ARCHIVE;
//		}
//		else {
//			// Archive option not selected - fail
//			UploadDashboard.showMessageAt(ARCHIVE_PLAN_REQUIRED_MSG, archivePlanHtml);
//			return;
//		}

		boolean repeatSend = true; // XXX
        boolean requestDOI = genDoiChkBx.getValue().booleanValue();
		// Submit the dataset
		UploadDashboard.showWaitCursor();
        UploadDashboard.showWaitCursor(submitButton);
        UploadDashboard.showWaitCursor(cancelButton);
		service.submitDatasetsToArchive(getUsername(), _submitIdsList, _submitColsList, 
		                                archiveStatus, repeatSend, 
                                        submitCommentTextArea.getText(), requestDOI,
			new AsyncCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					// Success - go back to the cruise list page
					UploadDashboard.showAutoCursor();
                    UploadDashboard.showAutoCursor(singleton.submitButton);
                    UploadDashboard.showAutoCursor(singleton.cancelButton);
                    cancelButton.setText("Done");
                    getSubmitStatus(singleton);
				}
				@Override
				public void onFailure(Throwable ex) {
					// Failure, so show fail message
					// But still go back to the cruise list page since some may have succeeded
                    // XXX No!  Deal with this.
					UploadDashboard.showFailureMessage(SUBMIT_FAILURE_MSG_START + " for Archiving: ", ex);
//					DatasetListPage.showPage();
                    SubmitToArchivePage.singleton.submissionTime.setText("Package submitted.");
					UploadDashboard.showAutoCursor();
                    UploadDashboard.showAutoCursor(submitButton);
                    UploadDashboard.showAutoCursor(cancelButton);
				}
			});
	}

	private void buildSubmitColumnList() {
		for (CheckBox cbox : _allCBoxes) {
		    GWT.log(cbox.getName() + ": " + cbox.isChecked() + " : " + cbox.getValue());
			if ( cbox.getValue().booleanValue()) {
				String columnName = cbox.getName();
				_submitColsList.add(columnName);
			}
		}
	}

}
