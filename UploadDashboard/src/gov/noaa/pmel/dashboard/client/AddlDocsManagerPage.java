/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.DOM;
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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

/**
 * Page for managing supplemental documents for a dataset.  
 *  
 * @author Karl Smith
 */
public class AddlDocsManagerPage extends CompositeWithUsername {

	private static final String TITLE_TEXT = "Supplemental Documents";

	private static final String INTRO_HTML_PROLOGUE = 
			"Supplemental documents associated with the datasets: <ul>";
	private static final String INTRO_HTML_EPILOGUE = 
			"</ul>";

	private static final String UPLOAD_TEXT = "Upload";
	private static final String UPLOAD_HOVER_HELP = 
			"upload a file that will be added as a new supplemental document, " +
			"or replace an existing supplemental document, for the datasets";

	private static final String DISMISS_TEXT = "Done";

	private static final String NO_FILE_ERROR_MSG = 
			"Please select a document to upload";

//	private static final String NO_OME_OVERWRITE_ERROR_MSG =
//			"Documents with the name " + DashboardUtils.OME_FILENAME + 
//			" or " + DashboardUtils.PI_OME_FILENAME + 
//			" cannot to uploaded as supplemental documents.  " +
//			"Please upload the file under a different name.";

	private static final String ADDL_DOCS_LIST_FAIL_MSG = 
			"Unexpected problems obtaining the updated supplemental " +
			"documents for the datasets";

	private static final String OVERWRITE_WARNING_MSG_PROLOGUE = 
			"This will overwrite the supplemental documents: <ul>";
	private static final String OVERWRITE_WARNING_MSG_EPILOGUE =
			"</ul> Do you wish to proceed?";
	private static final String OVERWRITE_YES_TEXT = "Yes";
	private static final String OVERWRITE_NO_TEXT = "No";

	private static final String DELETE_BUTTON_TEXT = "Delete";

	private static final String DELETE_DOC_HTML_PROLOGUE =
			"This will deleted the supplemental document: <ul><li>";
	private static final String DELETE_DOC_HTML_EPILOGUE =
			"</li></ul> Do you wish to proceed?";
	private static final String DELETE_YES_TEXT = "Yes";
	private static final String DELETE_NO_TEXT = "No";

	private static final String DELETE_DOCS_FAIL_MSG =
			"Problems deleting supplemental document";

	private static final String UNEXPLAINED_FAIL_MSG = 
			"<h3>Upload failed.</h3>" + 
			"<p>Unexpectedly, no explanation of the failure was given</p>";
	private static final String EXPLAINED_FAIL_MSG_START = 
			"<h3>Upload failed.</h3>" +
			"<p><pre>\n";
	private static final String EXPLAINED_ERROR_MSG_START = 
			"<h3>There was an error uploading the document(s).</h3>" +
			"<p><pre>\n";
	private static final String EXPLAINED_FAIL_MSG_END = 
			"</pre></p>";

	// Replacement strings for empty or null values
	private static final String EMPTY_TABLE_TEXT = 
			"No supplemental documents";

	// Column header strings
	private static final String FILENAME_COLUMN_NAME = "Filename";
	private static final String UPLOAD_TIME_COLUMN_NAME = "Upload date";
	private static final String DATASETIDS_COLUMN_NAME = "Dataset";

	interface AddlDocsManagerPageUiBinder extends UiBinder<Widget, AddlDocsManagerPage> {
	}

	private static AddlDocsManagerPageUiBinder uiBinder = 
			GWT.create(AddlDocsManagerPageUiBinder.class);

	private static DashboardServicesInterfaceAsync service = 
			GWT.create(DashboardServicesInterface.class);

	@UiField HTML introHtml; 
	@UiField DataGrid<DashboardMetadata> addlDocsGrid;
	@UiField FormPanel uploadForm;
	@UiField FileUpload docUpload;
	@UiField Hidden timestampToken;
	@UiField Hidden datasetIdsToken;
	@UiField Hidden supplementalFlag;
	@UiField Button uploadButton;
	@UiField Button dismissButton;

	private ListDataProvider<DashboardMetadata> listProvider;
	private HashSet<DashboardDataset> cruiseSet;
	private TreeSet<String> datasetIds;
	private DashboardAskPopup askOverwritePopup;

	// The singleton instance of this page
	private static AddlDocsManagerPage singleton;

	/**
	 * Creates an empty metadata list page.  Do not call this constructor; 
	 * instead use the one of the showPage static methods to show the 
	 * singleton instance of this page with the additional documents for 
	 * a cruise. 
	 */
	AddlDocsManagerPage() {
        super(PagesEnum.MANAGE_DOCUMENTS.name());
		initWidget(uiBinder.createAndBindUi(this));
		singleton = this;

		buildMetadataListTable();

		setUsername(null);
		cruiseSet = new HashSet<DashboardDataset>();
		datasetIds = new TreeSet<String>();
		askOverwritePopup = null;

		clearTokens();

		header.setPageTitle(TITLE_TEXT);

		uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadForm.setMethod(FormPanel.METHOD_POST);
		uploadForm.setAction(GWT.getModuleBaseURL() + "MetadataUploadService");

		uploadButton.setText(UPLOAD_TEXT);
		uploadButton.setEnabled(false);
		uploadButton.setTitle(UPLOAD_HOVER_HELP);
        docUpload.getElement().setPropertyString("multiple", "multiple");
        docUpload.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                String files = docUpload.getFilename(); // getInputFileNames(uploadElement);
                boolean fileSelected = files != null && files.length() > 0;
                uploadButton.setEnabled(fileSelected);
                dismissButton.setText("Cancel");
            }
        });

		dismissButton.setText(DISMISS_TEXT);
	}

	/**
	 * Display this page in the RootLayoutPanel with the list of supplemental 
	 * documents in the given cruises.  Note that any uploaded documents 
	 * are added to all the cruises by replicating the documents.  
	 * Adds this page to the page history list.
	 * 
	 * @param cruiseList
	 * 		cruises to use 
	 */
	static void showPage(DashboardDatasetList cruiseList) {
        GWT.log("show AddlDocs");
		if ( singleton == null )
			singleton = new AddlDocsManagerPage();
        singleton.uploadForm.reset();
		singleton.updateAddlDocs(cruiseList);
        singleton.wasActuallyOk = false;
		UploadDashboard.updateCurrentPage(singleton, UploadDashboard.DO_PING);
	}

	/**
	 * Redisplays the last version of this page if the username
	 * associated with this page matches the given username.
	 */
	static void redisplayPage(String username) {
        GWT.log("redisplay AddlDocs:" + username);
		if ( (username == null) || username.isEmpty() || 
			 (singleton == null) || ! singleton.getUsername().equals(username) ) {
			DatasetListPage.showPage();
		}
		else {
			UploadDashboard.updateCurrentPage(singleton);
		}
	}

	/**
	 * Updates the this page with the given cruises and their 
	 * supplemental documents.
	 * 
	 * @param cruiseSet
	 * 		set of cruises to use 
	 */
	private void updateAddlDocs(DashboardDatasetList cruises) {
		// Update the username
		setUsername(cruises.getUsername());
		header.userInfoLabel.setText(WELCOME_INTRO + getUsername());
        header.addDatasetIds(cruises);

		// Update the cruises associated with this page
		cruiseSet.clear();
		cruiseSet.addAll(cruises.values());
		datasetIds.clear();
		datasetIds.addAll(cruises.keySet());

		// Update the HTML intro naming the cruises
		StringBuilder sb = new StringBuilder();
		sb.append(INTRO_HTML_PROLOGUE);
		for ( DashboardDataset dataset : cruises.values()) {
            String name = dataset.getUserDatasetName();
			sb.append("<li>" + SafeHtmlUtils.htmlEscape(name) + "</li>");
		}
		sb.append(INTRO_HTML_EPILOGUE);
		introHtml.setHTML(sb.toString());

		// Clear the hidden tokens just to be safe
		clearTokens();

		// Update the metadata shown by resetting the data in the data provider
		List<DashboardMetadata> addlDocsList = listProvider.getList();
		addlDocsList.clear();
		for ( DashboardDataset cruz : cruiseSet ) {
			for ( String docTitle : cruz.getAddlDocs() ) {
				String[] nameDate = DashboardMetadata.splitAddlDocsTitle(docTitle);
				DashboardMetadata mdata = new DashboardMetadata();
				mdata.setDatasetId(cruz.getDatasetId());
				mdata.setFilename(nameDate[0]);
				mdata.setUploadTimestamp(nameDate[1]);
				addlDocsList.add(mdata);
			}
		}
		addlDocsGrid.setRowCount(addlDocsList.size(), true);
		// Make sure the table is sorted according to the last specification
		ColumnSortEvent.fire(addlDocsGrid, addlDocsGrid.getColumnSortList());
		// No pager (not needed); just set the page size and refresh the view
		addlDocsGrid.setPageSize(DashboardUtils.MAX_ROWS_PER_GRID_PAGE);
	}

	/**
	 * Clears all the Hidden tokens on the page.
	 */
	private void clearTokens() {
		timestampToken.setValue("");
		datasetIdsToken.setValue("");
		supplementalFlag.setValue("");
        dismissButton.setText("Done");
        uploadButton.setEnabled(false);
	}

	/**
	 * Assigns all the Hidden tokens on the page.
	 */
	private void assignTokens() {
		String localTimestamp = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm Z").format(new Date());
		timestampToken.setValue(localTimestamp);
		datasetIdsToken.setValue(DashboardUtils.encodeStringArrayList(new ArrayList<String>(datasetIds)));
		supplementalFlag.setValue("true");
	}

	@UiHandler("dismissButton")
	void cancelOnClick(ClickEvent event) {
	    GWT.log("addl docs cancel.");
        showDatasetsPage();
	}
    
	static void showDatasetsPage() {
	    GWT.log("addl docs show datasets.");
//        if ( UploadDashboard.needsHistoryForcing()) {
//            DatasetListPage.showPage();
//        } else {
            DatasetListPage.showPage();
//        }
	}

	@UiHandler("uploadButton") 
	void uploadButtonOnClick(ClickEvent event) {
		// Make sure a file was selected
		String uploadFilename = DashboardUtils.baseName(docUpload.getFilename());
		if ( uploadFilename.isEmpty() ) {
			UploadDashboard.showMessage(NO_FILE_ERROR_MSG);
			return;
		}

// XXX TODO: OME_FILENAME
//		// Disallow any overwrite of an OME file
//		if ( uploadFilename.equals(DashboardUtils.OME_FILENAME) ||
//			 uploadFilename.equals(DashboardUtils.PI_OME_FILENAME) ) {
//			UploadDashboard.showMessage(NO_OME_OVERWRITE_ERROR_MSG);
//			return;
//		}

		// Check for any overwrites that will happen
		String message = OVERWRITE_WARNING_MSG_PROLOGUE;
		boolean willOverwrite = false;
		for ( DashboardDataset cruz : cruiseSet ) {
			for ( String addlDocTitle : cruz.getAddlDocs() ) {
				String[] nameTime = DashboardMetadata.splitAddlDocsTitle(addlDocTitle);
				if ( uploadFilename.equals(nameTime[0]) ) {
					message += "<li>" + SafeHtmlUtils.htmlEscape(nameTime[0]) + 
							"<br />&nbsp;&nbsp;(uploaded " + SafeHtmlUtils.htmlEscape(nameTime[1]) + 
							")<br />&nbsp;&nbsp;for dataset " + 
							SafeHtmlUtils.htmlEscape(cruz.getDatasetId()) + "</li>";
					willOverwrite = true;
				}
			}
		}

		// If an overwrite will occur, ask for confirmation
		if ( willOverwrite ) {
			message += OVERWRITE_WARNING_MSG_EPILOGUE;
			if ( askOverwritePopup == null ) {
				askOverwritePopup = new DashboardAskPopup(OVERWRITE_YES_TEXT, 
						OVERWRITE_NO_TEXT, new OAPAsyncCallback<Boolean>() {
					@Override
					public void onSuccess(Boolean result) {
						// Submit only if yes
						if ( result == true ) {
							assignTokens();
							uploadForm.submit();
						}
					}
					@Override
					public void customFailure(Throwable ex) {
                        Window.alert("Error from popup: " + ex.toString()); // Should never be called.
					}
				});
			}
			askOverwritePopup.askQuestion(message);
			return;
		} else {
    		assignTokens();
    		uploadForm.submit();
		}
	}

	@UiHandler("uploadForm")
	void uploadFormOnSubmit(SubmitEvent event) {
		UploadDashboard.showWaitCursor();
	}

	@UiHandler("uploadForm")
	void uploadFormOnSubmitComplete(SubmitCompleteEvent event) {
	    GWT.log("submit form complete");
		clearTokens();
		// Process the returned message
		processResultMsg(event.getResults());
		// Contact the server to obtain the latest set 
		// of supplemental documents for the current cruises
		service.getUpdatedDatasets(getUsername(), datasetIds, 
				new OAPAsyncCallback<DashboardDatasetList>() {
			@Override
			public void onSuccess(DashboardDatasetList cruiseList) {
				// Update the list shown in this page
				updateAddlDocs(cruiseList);
				UploadDashboard.showAutoCursor();
			}
			@Override
			public void customFailure(Throwable ex) {
			    GWT.log("addl docs upload custom failure:"+ex);
				UploadDashboard.showFailureMessage(ADDL_DOCS_LIST_FAIL_MSG, ex);
				UploadDashboard.showAutoCursor();
			}
		});
	}

    // deal with a Firefox history bug
    private boolean wasActuallyOk = false;
	/**
	 * Process the message returned from the upload of a dataset.
	 * 
	 * @param resultMsg
	 * 		message returned from the upload of a dataset
	 */
	private void processResultMsg(String resultMsg) {
	    GWT.log("upload result:"+resultMsg+".");
		if ( resultMsg == null ) {
            UploadDashboard.logToConsole("null resultMsg fail.");
			UploadDashboard.showMessage(UNEXPLAINED_FAIL_MSG);
			return;
		}
		resultMsg = resultMsg.trim();
		if ( resultMsg.startsWith(DashboardUtils.SUCCESS_HEADER_TAG) ) {
            uploadForm.reset();
            wasActuallyOk = true;
			// Do not show any messages on success;
			// depend on the updated list of documents to show success
		}
		else if ( resultMsg.startsWith(DashboardUtils.INVALID_FILE_HEADER_TAG)) {
			String errorMsg = resultMsg.substring(DashboardUtils.INVALID_FILE_HEADER_TAG.length()+1);
			UploadDashboard.showMessage(EXPLAINED_ERROR_MSG_START + 
					SafeHtmlUtils.htmlEscape(errorMsg) + EXPLAINED_FAIL_MSG_END 
					+ DashboardUtils.VIRUS_DETECTED);
		}
		else {
		    GWT.log("unknown response fail:"+resultMsg);
            GWT.log("isFirefox:"+UploadDashboard.isFirefox()+" and wasOk: " + wasActuallyOk);
            if ( UploadDashboard.needsHistoryForcing() && wasActuallyOk ) {
                DatasetListPage.showPage();
            } else {
    			// Unknown response, just display the entire message
    			UploadDashboard.showMessage(EXPLAINED_FAIL_MSG_START + 
					SafeHtmlUtils.htmlEscape(resultMsg) + EXPLAINED_FAIL_MSG_END);
            }
		}
	}

	/**
	 * Creates the table of selectable metadata documents
	 */
	private void buildMetadataListTable() {
		// Create the columns for this table
		Column<DashboardMetadata,String> deleteColumn = buildDeleteColumn();
		TextColumn<DashboardMetadata> filenameColumn = buildFilenameColumn();
		TextColumn<DashboardMetadata> uploadTimeColumn = buildUploadTimeColumn();
		TextColumn<DashboardMetadata> datasetIdColumn = buildDatasetIdColumn();
		
		// Add the columns, with headers, to the table
		addlDocsGrid.addColumn(deleteColumn, "");
		addlDocsGrid.addColumn(filenameColumn, FILENAME_COLUMN_NAME);
		addlDocsGrid.addColumn(uploadTimeColumn, UPLOAD_TIME_COLUMN_NAME);
		addlDocsGrid.addColumn(datasetIdColumn, DATASETIDS_COLUMN_NAME);

		// Set the minimum widths of the columns
		double tableWidth = 0.0;
		addlDocsGrid.setColumnWidth(deleteColumn, 
				UploadDashboard.NARROW_COLUMN_WIDTH, Style.Unit.EM);
		tableWidth += UploadDashboard.NARROW_COLUMN_WIDTH;
		addlDocsGrid.setColumnWidth(filenameColumn, 
				UploadDashboard.FILENAME_COLUMN_WIDTH, Style.Unit.EM);
		tableWidth += UploadDashboard.FILENAME_COLUMN_WIDTH;
		addlDocsGrid.setColumnWidth(uploadTimeColumn, 
				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		tableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;
		addlDocsGrid.setColumnWidth(datasetIdColumn, 
				UploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		tableWidth += UploadDashboard.NORMAL_COLUMN_WIDTH;

		// Set the minimum width of the full table
		addlDocsGrid.setMinimumTableWidth(tableWidth, Style.Unit.EM);

		// Create the data provider for this table
		listProvider = new ListDataProvider<DashboardMetadata>();
		listProvider.addDataDisplay(addlDocsGrid);

		// Make the columns sortable
		deleteColumn.setSortable(false);
		filenameColumn.setSortable(true);
		uploadTimeColumn.setSortable(true);
		datasetIdColumn.setSortable(true);

		// Add a column sorting handler for these columns
		ListHandler<DashboardMetadata> columnSortHandler = 
				new ListHandler<DashboardMetadata>(listProvider.getList());
		columnSortHandler.setComparator(filenameColumn, 
				DashboardMetadata.filenameComparator);
		columnSortHandler.setComparator(uploadTimeColumn, 
				DashboardMetadata.uploadTimestampComparator);
		columnSortHandler.setComparator(datasetIdColumn, 
				DashboardMetadata.datasetIdComparator);

		// Add the sort handler to the table, and sort by filename, then dataset by default
		addlDocsGrid.addColumnSortHandler(columnSortHandler);
		addlDocsGrid.getColumnSortList().push(datasetIdColumn);
		addlDocsGrid.getColumnSortList().push(filenameColumn);

		// Set the contents if there are no rows
		addlDocsGrid.setEmptyTableWidget(new Label(EMPTY_TABLE_TEXT));

		// Following recommended to improve efficiency with IE
		addlDocsGrid.setSkipRowHoverCheck(false);
		addlDocsGrid.setSkipRowHoverFloatElementCheck(false);
		addlDocsGrid.setSkipRowHoverStyleUpdate(false);
	}

	/**
	 * @return the upload filename column for the table
	 */
	private TextColumn<DashboardMetadata> buildFilenameColumn() {
		TextColumn<DashboardMetadata> filenameColumn = 
						new TextColumn<DashboardMetadata> () {
			@Override
			public String getValue(DashboardMetadata mdata) {
				return mdata.getFilename();
			}
		};
		return filenameColumn;
	}

	/**
	 * @return the upload timestamp column for the table
	 */
	private TextColumn<DashboardMetadata> buildUploadTimeColumn() {
		TextColumn<DashboardMetadata> uploadTimeColumn = 
						new TextColumn<DashboardMetadata> () {
			@Override
			public String getValue(DashboardMetadata mdata) {
				return mdata.getUploadTimestamp();
			}
		};
		return uploadTimeColumn;
	}

	/**
	 * @return the upload dataset ID column for the table
	 */
	private TextColumn<DashboardMetadata> buildDatasetIdColumn() {
		TextColumn<DashboardMetadata> datasetIdColumn = 
						new TextColumn<DashboardMetadata> () {
			@Override
			public String getValue(DashboardMetadata mdata) {
				return mdata.getDatasetId();
			}
		};
		return datasetIdColumn;
	}

	/**
	 * @return the delete column for the tables
	 */
	private Column<DashboardMetadata,String> buildDeleteColumn() {
		Column<DashboardMetadata,String> deleteColumn =
				new Column<DashboardMetadata,String>(new ButtonCell()) {
			@Override
			public String getValue(DashboardMetadata object) {
				return DELETE_BUTTON_TEXT;
			}
		};
		deleteColumn.setFieldUpdater(new FieldUpdater<DashboardMetadata,String>() {
			@Override
			public void update(int index, DashboardMetadata mdata, String value) {
				// Show the document name and have the user confirm the delete 
				final String deleteFilename = mdata.getFilename();
				final String deleteId = mdata.getDatasetId();
				String message = DELETE_DOC_HTML_PROLOGUE + 
						SafeHtmlUtils.htmlEscape(deleteFilename) + 
						"<br />&nbsp;&nbsp;(uploaded " + 
						SafeHtmlUtils.htmlEscape(mdata.getUploadTimestamp()) + 
						")<br />&nbsp;&nbsp;for dataset " + 
						SafeHtmlUtils.htmlEscape(deleteId) + 
						DELETE_DOC_HTML_EPILOGUE;
				new DashboardAskPopup(DELETE_YES_TEXT, DELETE_NO_TEXT, 
						new AsyncCallback<Boolean>() {
					@Override
					public void onSuccess(Boolean result) {
						// Only continue if yes returned; ignore if no or null
						if ( result == true ) {
							continueDelete(deleteFilename, deleteId);
						}
					}
					@Override
					public void onFailure(Throwable caught) {
                        Window.alert("Error from popup: " + caught.toString()); // Should never be called.
					}
				}).askQuestion(message);
			}
		});
		return deleteColumn;
	}

	/**
	 * Calls the server to delete an ancillary document from a cruise.
	 * 
	 * @param deleteFilename
	 * 		upload name of the document to delete
	 * @param deleteId
	 * 		delete the document from the cruise with this dataset ID
	 */
	private void continueDelete(String deleteFilename, String deleteId) {
		// Send the request to the server
		UploadDashboard.showWaitCursor();
		service.deleteAddlDoc(getUsername(), deleteFilename, deleteId, 
				datasetIds, new OAPAsyncCallback<DashboardDatasetList>() {
			@Override
			public void onSuccess(DashboardDatasetList cruiseList) {
				// Update the list shown in this page
				updateAddlDocs(cruiseList);
				UploadDashboard.showAutoCursor();
			}
			@Override
			public void customFailure(Throwable ex) {
				UploadDashboard.showFailureMessage(DELETE_DOCS_FAIL_MSG, ex);
				UploadDashboard.showAutoCursor();
			}
		});
	}

}