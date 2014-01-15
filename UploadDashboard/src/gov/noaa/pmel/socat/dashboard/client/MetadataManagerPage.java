/**
 * 
 */
package gov.noaa.pmel.socat.dashboard.client;

import gov.noaa.pmel.socat.dashboard.client.SocatUploadDashboard.PagesEnum;
import gov.noaa.pmel.socat.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.socat.dashboard.shared.DashboardMetadataList;
import gov.noaa.pmel.socat.dashboard.shared.MetadataListService;
import gov.noaa.pmel.socat.dashboard.shared.MetadataListServiceAsync;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

/**
 * Page for associating metadata files to one or more cruises.  
 * Shows currently uploaded metadata files for a user and 
 * provides a connection for uploading new or updated metadata 
 * files.
 *  
 * @author Karl Smith
 */
public class MetadataManagerPage extends Composite {

	private static final String WELCOME_INTRO = "Logged in as: ";
	private static final String LOGOUT_TEXT = "Logout";

	private static final String INTRO_HTML_PROLOGUE = 
			"Metadata documents associated with the cruise: <b>";
	private static final String INTRO_HTML_EPILOGUE = 
			"</b>";

	private static final String UPLOAD_TEXT = "Upload Metadata";
	private static final String UPLOAD_HOVER_HELP = 
			"upload a file that will be added as a new metadata document, " +
			"or replace an existing metadata document, for this cruise";

	private static final String SET_OME_TEXT = "Set Selected as OME";
	private static final String SET_OME_HOVER_HELP = 
			"set the selected document as the OME metadata document";

	private static final String DELETE_TEXT = "Delete Selected Metadata";
	private static final String DELETE_HOVER_HELP =
			"delete the selected metadata documents from this cruise";

	private static final String DISMISS_TEXT = "Return to Cruise List";

	private static final String METADATA_LIST_FAIL_MSG = 
			"Unexpected problems obtaining the metadata documents " +
			"information for the cruise";
	
	private static final String NO_SELECTED_METADATA_TO_DELETE_MSG = 
			"No metadata documents are selected for deletion";

	private static final String DELETE_METADATA_HTML_PROLOGUE =
			"The following metadata document(s) will be deleted: <ul>";
	private static final String DELETE_METADATA_HTML_EPILOGUE =
			"</ul> Do you wish to proceed?";
	private static final String DELETE_YES_TEXT = "Yes";
	private static final String DELETE_NO_TEXT = "No";

	private static final String DELETE_METADATA_FAIL_MSG =
			"Problems deleting metadata documents";

	// Replacement strings for empty or null values
	private static final String EMPTY_TABLE_TEXT = 
			"No uploaded metadata documents";

	// Column header strings
	private static final String OME_STAR_COLUMN_NAME = "OME";
	private static final String FILENAME_COLUMN_NAME = "Filename";
	private static final String UPLOAD_TIME_COLUMN_NAME = "Uploaded on";
	private static final String OWNER_COLUMN_NAME = "Owner";

	private static final String IS_OME_STRING = "*";
	private static final String NOT_OME_STRING = " ";

	interface MetadataManagerPageUiBinder 
			extends UiBinder<Widget, MetadataManagerPage> {
	}

	private static MetadataManagerPageUiBinder uiBinder = 
			GWT.create(MetadataManagerPageUiBinder.class);

	private static MetadataListServiceAsync service = 
			GWT.create(MetadataListService.class);

	@UiField Label userInfoLabel;
	@UiField HTML introHtml; 
	@UiField Button logoutButton;
	@UiField Button uploadButton;
	@UiField Button setOmeButton;
	@UiField Button deleteButton;
	@UiField DataGrid<DashboardMetadata> metadataGrid;
	@UiField Button dismissButton;

	private String username;
	private ListDataProvider<DashboardMetadata> listProvider;
	private String expocode;
	private String omeFilename;
	private DashboardAskPopup askDeletePopup;

	// The singleton instance of this page
	private static MetadataManagerPage singleton;

	/**
	 * Creates an empty metadata list page.  Do not call this constructor; 
	 * instead use the one of the showPage static methods to show the 
	 * singleton instance of this page with the metadata for a cruise. 
	 */
	private MetadataManagerPage() {
		initWidget(uiBinder.createAndBindUi(this));
		buildMetadataListTable();
		username = "";
		expocode = "";
		omeFilename = "";
		askDeletePopup = null;

		logoutButton.setText(LOGOUT_TEXT);
		dismissButton.setText(DISMISS_TEXT);

		uploadButton.setText(UPLOAD_TEXT);
		uploadButton.setTitle(UPLOAD_HOVER_HELP);

		setOmeButton.setText(SET_OME_TEXT);
		setOmeButton.setTitle(SET_OME_HOVER_HELP);

		deleteButton.setText(DELETE_TEXT);
		deleteButton.setTitle(DELETE_HOVER_HELP);
	}

	/**
	 * Display this page in the RootLayoutPanel with the latest 
	 * metadata list obtained from the server for this indicated cruise.  
	 * Adds this page to the page history list.
	 * 
	 * @param cruiseExpocode
	 * 		manage the metadata for the cruise with this expocode 
	 */
	static void showPage(final String cruiseExpocode) {
		service.getMetadataList(DashboardLoginPage.getUsername(), 
								DashboardLoginPage.getPasshash(),
								cruiseExpocode,
								new AsyncCallback<DashboardMetadataList>() {
			@Override
			public void onSuccess(DashboardMetadataList mdataList) {
				if ( DashboardLoginPage.getUsername()
										.equals(mdataList.getUsername()) ) {
					if ( singleton == null )
						singleton = new MetadataManagerPage();
					SocatUploadDashboard.updateCurrentPage(singleton);
					singleton.updateMetadataList(cruiseExpocode, mdataList);
					History.newItem(PagesEnum.METADATA_MANAGER.name(), false);
				}
				else {
					SocatUploadDashboard.showMessage(METADATA_LIST_FAIL_MSG + 
							" (unexpected invalid metadata list)");
				}
			}
			@Override
			public void onFailure(Throwable ex) {
				SocatUploadDashboard.showFailureMessage(METADATA_LIST_FAIL_MSG, ex);
			}
		});
	}

	/**
	 * Redisplays the last version of this page if the username
	 * associated with this page matches the current login username.
	 * 
	 * @param addToHistory 
	 * 		if true, adds this page to the page history 
	 */
	static void redisplayPage(boolean addToHistory) {
		// If never show before, or if the username does not match the 
		// current login username, show the login page instead
		if ( (singleton == null) || 
			 ! singleton.username.equals(DashboardLoginPage.getUsername()) ) {
			DashboardLoginPage.showPage(true);
		}
		else {
			SocatUploadDashboard.updateCurrentPage(singleton);
			if ( addToHistory )
				History.newItem(PagesEnum.METADATA_MANAGER.name(), false);
		}
	}

	/**
	 * Updates the this page with the current username, 
	 * cruise expocode, and metadata files associated 
	 * with the cruise.
	 * 
	 * @param cruiseExpocode
	 * 		expocode of the cruise  
	 * @param mdataList
	 * 		metadata documents for the cruise
	 */
	private void updateMetadataList(String cruiseExpocode, 
									DashboardMetadataList mdataList) {
		// Update the username
		username = DashboardLoginPage.getUsername();
		userInfoLabel.setText(WELCOME_INTRO + username);

		// Update the expocode of the cruise associated with this page
		expocode = cruiseExpocode;

		// Update the HTML intro naming the cruise
		introHtml.setHTML(INTRO_HTML_PROLOGUE + 
				SafeHtmlUtils.htmlEscape(cruiseExpocode) +
				INTRO_HTML_EPILOGUE);

		// record the name of the OME file in the metadata list 
		omeFilename = mdataList.getOmeFilename();
		// Update the metadata shown by resetting the data in the data provider
		List<DashboardMetadata> metadataList = listProvider.getList();
		metadataList.clear();
		if ( mdataList != null ) {
			// This list includes the OME metadata filename
			metadataList.addAll(mdataList.values());
		}
		metadataGrid.setRowCount(metadataList.size());
		// Make sure the table is sorted according to the last specification
		ColumnSortEvent.fire(metadataGrid, metadataGrid.getColumnSortList());
	}

	@UiHandler("logoutButton")
	void logoutOnClick(ClickEvent event) {
		DashboardLogoutPage.showPage();
	}

	@UiHandler("dismissButton")
	void cancelOnClick(ClickEvent event) {
		// Change to the latest cruise listing page.
		CruiseListPage.showPage(false);
	}

	@UiHandler("uploadButton")
	void uploadOnClick(ClickEvent event) {
		// Get all the metdata filenames for this cruise
		TreeSet<String> mdataNames = new TreeSet<String>();
		for ( DashboardMetadata mdata : listProvider.getList() ) {
			mdataNames.add(mdata.getFilename());
		}
		// Show the metadata upload page for this cruise
		MetadataUploadPage.showPage(expocode, omeFilename, mdataNames);
	}

	@UiHandler("setOmeButton")
	void setOmeOnClick(ClickEvent event) {
		// TODO:
	}

	@UiHandler("deleteButton")
	void deleteOnClick(ClickEvent event) {
		// Get the list of selected metadata documents
		final TreeSet<String> mdataNames = new TreeSet<String>();
		for ( DashboardMetadata mdata : listProvider.getList() ) {
			if ( mdata.isSelected() ) {
				mdataNames.add(mdata.getFilename());
			}
		}
		if ( mdataNames.size() < 1 ) {
			SocatUploadDashboard.showMessage(NO_SELECTED_METADATA_TO_DELETE_MSG);
			return;
		}

		// Present the list of documents that will be deleted
		// and ask the user to confirm
		String message = DELETE_METADATA_HTML_PROLOGUE;
		boolean first = true;
		for ( String name : mdataNames ) {
			if ( first )
				first = false;
			else
				message += "<li>" + SafeHtmlUtils.htmlEscape(name) + "</li>";
			message += name;
		}
		message += DELETE_METADATA_HTML_EPILOGUE;
		if ( askDeletePopup == null ) {
			askDeletePopup = new DashboardAskPopup(DELETE_YES_TEXT, DELETE_NO_TEXT, 
					new AsyncCallback<Boolean>() {
				@Override
				public void onSuccess(Boolean result) {
					// Only continue if yes returned; ignore if no or null
					if ( result == true )
						continueDelete(mdataNames);
				}
				@Override
				public void onFailure(Throwable caught) {
					// Never called
					;
				}
			});
		}
		askDeletePopup.askQuestion(message);
	}

	private void continueDelete(TreeSet<String> mdataNames) {
		// Send the request to the server
		service.removeMetadata(DashboardLoginPage.getUsername(), 
				DashboardLoginPage.getPasshash(),
				expocode, mdataNames,
				new AsyncCallback<DashboardMetadataList>() {
			@Override
			public void onSuccess(DashboardMetadataList mdataList) {
				if ( DashboardLoginPage.getUsername()
						.equals(mdataList.getUsername()) ) {
					// Update the list shown in this page
					updateMetadataList(expocode, mdataList);
				}
				else {
					SocatUploadDashboard.showMessage(DELETE_METADATA_FAIL_MSG + 
							" (unexpected invalid metadata list)");
				}
			}
			@Override
			public void onFailure(Throwable ex) {
				SocatUploadDashboard.showFailureMessage(DELETE_METADATA_FAIL_MSG, ex);
			}
		});
	}

	/**
	 * Creates the table of selectable metadata documents
	 */
	private void buildMetadataListTable() {
		// Create the columns for this table
		Column<DashboardMetadata,Boolean> selectedColumn = buildSelectedColumn();
		TextColumn<DashboardMetadata> omeStarColumn = buildOmeStarColumn();
		TextColumn<DashboardMetadata> filenameColumn = buildFilenameColumn();
		TextColumn<DashboardMetadata> uploadTimeColumn = buildUploadTimeColumn();
		TextColumn<DashboardMetadata> ownerColumn = buildOwnerColumn();
		
		// Add the columns, with headers, to the table
		metadataGrid.addColumn(selectedColumn, "");
		metadataGrid.addColumn(omeStarColumn, OME_STAR_COLUMN_NAME);
		metadataGrid.addColumn(filenameColumn, FILENAME_COLUMN_NAME);
		metadataGrid.addColumn(uploadTimeColumn, UPLOAD_TIME_COLUMN_NAME);
		metadataGrid.addColumn(ownerColumn, OWNER_COLUMN_NAME);

		// Set the minimum widths of the columns
		double tableWidth = 0.0;
		metadataGrid.setColumnWidth(selectedColumn, 
				SocatUploadDashboard.CHECKBOX_COLUMN_WIDTH, Style.Unit.EM);
		tableWidth += SocatUploadDashboard.CHECKBOX_COLUMN_WIDTH;
		metadataGrid.setColumnWidth(omeStarColumn,
				SocatUploadDashboard.NARROW_COLUMN_WIDTH, Style.Unit.EM);
		tableWidth += SocatUploadDashboard.NARROW_COLUMN_WIDTH;
		metadataGrid.setColumnWidth(filenameColumn, 
				SocatUploadDashboard.FILENAME_COLUMN_WIDTH, Style.Unit.EM);
		tableWidth += SocatUploadDashboard.FILENAME_COLUMN_WIDTH;
		metadataGrid.setColumnWidth(uploadTimeColumn, 
				SocatUploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		tableWidth += SocatUploadDashboard.NORMAL_COLUMN_WIDTH;
		metadataGrid.setColumnWidth(ownerColumn, 
				SocatUploadDashboard.NORMAL_COLUMN_WIDTH, Style.Unit.EM);
		tableWidth += SocatUploadDashboard.NORMAL_COLUMN_WIDTH;

		// Set the minimum width of the full table
		metadataGrid.setMinimumTableWidth(tableWidth, Style.Unit.EM);

		// Create the data provider for this table
		listProvider = new ListDataProvider<DashboardMetadata>();
		listProvider.addDataDisplay(metadataGrid);

		// Make the columns sortable
		selectedColumn.setSortable(true);
		omeStarColumn.setSortable(true);
		filenameColumn.setSortable(true);
		uploadTimeColumn.setSortable(true);
		ownerColumn.setSortable(true);

		// Add a column sorting handler for these columns
		ListHandler<DashboardMetadata> columnSortHandler = 
				new ListHandler<DashboardMetadata>(listProvider.getList());
		columnSortHandler.setComparator(selectedColumn,
				DashboardMetadata.selectedComparator);
		columnSortHandler.setComparator(omeStarColumn, new Comparator<DashboardMetadata>() {
			@Override
			public int compare(DashboardMetadata m1, DashboardMetadata m2) {
				String s1;
				if ( omeFilename.equals(m1.getFilename()) )
					s1 = IS_OME_STRING;
				else 
					s1 = NOT_OME_STRING;
				String s2;
				if ( omeFilename.equals(m2.getFilename()) )
					s2 = IS_OME_STRING;
				else
					s2 = NOT_OME_STRING;
				return s1.compareTo(s2);
			}
		});
		columnSortHandler.setComparator(filenameColumn, 
				DashboardMetadata.filenameComparator);
		columnSortHandler.setComparator(uploadTimeColumn, 
				DashboardMetadata.uploadTimestampComparator);
		columnSortHandler.setComparator(ownerColumn, 
				DashboardMetadata.ownerComparator);

		// Add the sort handler to the table, and sort by filename by default
		metadataGrid.addColumnSortHandler(columnSortHandler);
		metadataGrid.getColumnSortList().push(filenameColumn);

		// Set the contents if there are no rows
		metadataGrid.setEmptyTableWidget(new Label(EMPTY_TABLE_TEXT));

		// Following recommended to improve efficiency with IE
		metadataGrid.setSkipRowHoverCheck(false);
		metadataGrid.setSkipRowHoverFloatElementCheck(false);
		metadataGrid.setSkipRowHoverStyleUpdate(false);
	}

	/**
	 * Creates the selection column for the table
	 */
	private Column<DashboardMetadata,Boolean> buildSelectedColumn() {
		Column<DashboardMetadata,Boolean> selectedColumn = 
				new Column<DashboardMetadata,Boolean>(new CheckboxCell()) {
			@Override
			public Boolean getValue(DashboardMetadata mdata) {
				return mdata.isSelected();
			}
		};
		selectedColumn.setFieldUpdater(
					new FieldUpdater<DashboardMetadata,Boolean>() {
			@Override
			public void update(int index, 
					DashboardMetadata mdata, Boolean value) {
				if ( value == null ) {
					mdata.setSelected(false);
				}
				else {
					mdata.setSelected(value);
				}
			}
		});
		return selectedColumn;
	}

	/**
	 * Creates the OME star column for the table
	 */
	private TextColumn<DashboardMetadata> buildOmeStarColumn() {
		TextColumn<DashboardMetadata> starColumn = 
						new TextColumn<DashboardMetadata> () {
			@Override
			public String getValue(DashboardMetadata mdata) {
				if ( omeFilename.equals(mdata.getFilename()) )
					return IS_OME_STRING;
				return NOT_OME_STRING;
			}
		};
		return starColumn;
	}

	/**
	 * Creates the upload filename column for the table
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
	 * Creates the upload timestamp column for the table
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
	 * Creates the owner column for the table
	 */
	private TextColumn<DashboardMetadata> buildOwnerColumn() {
		TextColumn<DashboardMetadata> ownerColumn = 
						new TextColumn<DashboardMetadata> () {
			@Override
			public String getValue(DashboardMetadata mdata) {
				return mdata.getOwner();
			}
		};
		return ownerColumn;
	}

}