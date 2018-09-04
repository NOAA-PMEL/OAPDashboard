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
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;

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
	
//	@UiField Tree columnTree;
	@UiField HTML introHtml;
	@UiField Panel columnsPanel;
//	@UiField DataGrid<DataColumnType> columnsGrid;
	@UiField Button cancelButton;
	@UiField Button submitButton;
	
	@UiField RadioButton select_none;
//	@UiField RadioButton select_min;
//	@UiField RadioButton select_req;
	@UiField RadioButton select_std;
//	@UiField RadioButton select_user;
	@UiField RadioButton select_all;
    
	@UiField TextArea submitMsgTextBox;

	@UiField ApplicationHeaderTemplate header;
	
	private String _datasetId;
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

	public SubmitToArchivePage() {
		initWidget(uiBinder.createAndBindUi(this));
		setupHandlers();
		buildGrid();
		
		
		cancelButton.setText("Cancel");
		cancelButton.setTitle("Cancel");
		submitButton.setText("Submit");
		submitButton.setTitle("Submit");
		
		header.titleLabel.setText("Submit Datasets for Archving");
		introHtml.setHTML("Select Columns to Submit to Archive: <br/>");
		
		_allCBoxes = new ArrayList<>();
		_columnBoxMap = new HashMap<>();
		_submitIdsList = new ArrayList<>();
		_submitColsList = new ArrayList<>();
	}
	
	private void buildGrid() {
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
		select_none.addClickHandler(new RadioButtonClickHandler(SELECT.NONE));
//		select_min.addClickHandler(new RadioButtonClickHandler(SELECT.MIN));
//		select_req.addClickHandler(new RadioButtonClickHandler(SELECT.REQUIRED));
		select_std.addClickHandler(new RadioButtonClickHandler(SELECT.STANDARD));
//		select_user.addClickHandler(new RadioButtonClickHandler(SELECT.USER));
		select_all.addClickHandler(new RadioButtonClickHandler(SELECT.ALL));
	}

	static void showPage(DashboardDatasetList datasets) {
		if ( singleton == null ) {
			singleton = new SubmitToArchivePage();
		}
		UploadDashboard.updateCurrentPage(datasets.getUsername(), singleton);
		singleton._datasets = datasets;
		singleton.updateDatasetColumns(datasets);
		History.newItem(PagesEnum.SUBMIT_TO_ARCHIVE.name(), false);
	}

	protected void updateDatasetColumns(DashboardDatasetList datasets) {
	    _submitIdsList.clear();
	    select_none.setValue(false);
	    select_std.setValue(false);
	    select_all.setValue(false);
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
		
		DashboardDataset dataset = datasets.values().iterator().next();
	    if ( dataset.getDatasetId().equals(_datasetId)) {
	        return;
	    }
		_datasetId = dataset.getDatasetId();
		header.userInfoLabel.setText(WELCOME_INTRO + getUsername());
		
        submitMsgTextBox.setText(dataset.getArchiveSubmissionMessage());
		columnsPanel.clear();
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
			columnsPanel.add(cb);
		}
	}

	private boolean okToArchive(DashboardDatasetList datasets) {
	    boolean isOk = true;
	    
        return isOk; // XXX
    }
	
	private boolean checkDatasetColumns() {
	    boolean isOk = true;
	    
	    return isOk; // XXX
	}

    @UiHandler("cancelButton")
	void cancelOnClick(ClickEvent event) {
		// Return to the list of cruises which could have been modified by this page
		DatasetListPage.showPage();
	}

	@UiHandler("submitButton")
	void submitOnClick(ClickEvent event) {
	    
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
		// Submit the dataset
		UploadDashboard.showWaitCursor();
		service.submitDatasetsToArchive(getUsername(), _submitIdsList, _submitColsList, 
		                                archiveStatus, localTimestamp, repeatSend, 
                                        submitMsgTextBox.getText(),
			new AsyncCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					// Success - go back to the cruise list page
					DatasetListPage.showPage();
					UploadDashboard.showAutoCursor();
				}
				@Override
				public void onFailure(Throwable ex) {
					// Failure, so show fail message
					// But still go back to the cruise list page since some may have succeeded
					UploadDashboard.showFailureMessage(SUBMIT_FAILURE_MSG_START + " for Archiving: ", ex);
					DatasetListPage.showPage();
					UploadDashboard.showAutoCursor();
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
