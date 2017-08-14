/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.client.IntegerParser;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;

import gov.noaa.pmel.dashboard.client.UploadDashboard.PagesEnum;
import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.ADCMessageList;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterfaceAsync;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.QCFlag;
import gov.noaa.pmel.dashboard.shared.TypesDatasetDataPair;

/**
 * Page for specifying the data column types in a DashboardDatasetData.
 * 
 * @author Karl Smith
 */
public class DataColumnSpecsPage extends CompositeWithUsername {

	private static final int DATA_COLUMN_WIDTH = 16;

	private static final String TITLE_TEXT = "Identify Data Columns";
	private static final String WELCOME_INTRO = "Logged in as ";
	private static final String LOGOUT_TEXT = "Logout";

	private static final String MESSAGES_TEXT = "Show errors/warnings";

	private static final String SUBMIT_TEXT = "Check Data";
	private static final String ENABLED_SUBMIT_HOVER_HELP = 
			"Submits the current data column types and checks the given data";
	private static final String DISABLED_SUBMIT_HOVER_HELP = 
			"This cruise has been submitted for QC.  Data column types cannot be modified.";

	private static final String CANCEL_TEXT = "Done";
	private static final String SAVE_BUTTON_TEXT = "Save";
	private static final String SAVE_BUTTON_HOVER_HELP = "Save column data type definitions";

	private static final String INTRO_HTML_PROLOGUE = "Dataset: <ul><li>";
	private static final String INTRO_HTML_EPILOGUE = "</li></ul>";

	private static final String PAGER_LABEL_TEXT = "Rows shown";

	private static final String NOT_CHECKED_MSG = "(data values not checked)";

	private static final String UNKNOWN_COLUMN_TYPE_PROLOGUE = 
			" data columns:<ul>";
	private static final String UNKNOWN_COLUMN_TYPE_EPILOGUE = 
			"</ul> are still <em>(unknown)</em> and need to be specified.";
	private static final String NO_LONGITUDE_ERROR_MSG = 
			"No data column has been identified as the longitude";
	private static final String NO_LATITUDE_ERROR_MSG =
			"No data column has been identified as the latitude";
	private static final String NO_DEPTH_ERROR_MSG =
			"No data column has been identified as the sample depth";
	private static final String NO_TIMESTAMP_ERROR_MSG =
			"No data columns have been identified which provide " +
			"the date and time of each measurement";
	private static final String NO_DATE_ERROR_MSG =
			"No data columns have been identified which provide " +
			"the date of each measurement";
	private static final String NO_TIME_ERROR_MSG =
			"No data columns have been identified which provide " +
			"the time of each measurement";
	private static final String MISSING_DATE_PIECE_ERROR_MSG =
			"The data columns identified do not completely specify " +
			"the date of each measurement";
	private static final String MISSING_TIME_PIECE_ERROR_MSG =
			"The data columns identified do not completely specify " +
			"the time of each measurement";
	private static final String MULTIPLE_COLUMN_TYPES_ERROR_MSG =
			"More than one column has the type: ";

	private static final String DEFAULT_SECONDS_WARNING_QUESTION = 
			"No data columns have been identified providing the seconds " +
			"for the time of each measurement.  It is strongly recommended " +
			"that seconds be provided; however, a default value of zero " +
			"seconds can be added to the data." +
			"<br />" +
			"Is this okay?";
	private static final String USE_DEFAULT_SECONDS_TEXT = "Yes";
	private static final String NO_DEFAULT_SECONDS_TEXT = "No";

	private static final String GET_COLUMN_SPECS_FAIL_MSG = 
			"Problems obtaining the data column types";
	private static final String SUBMIT_FAIL_MSG = 
			"Problems updating the data column types";
	private static final String MORE_DATA_FAIL_MSG = 
			"Problems obtaining more data from the dataset";
	private static final String SAVE_FAIL_MSG = 
			"Problems saving column definitions";

	private static final String SANITY_CHECK_FAIL_MSG = 
			"The data check failed, indicating very serious errors in the data.";
	private static final String SANITY_CHECK_ERROR_MSG = 
			"The data check found serious errors in the data.";
	private static final String SANITY_CHECK_WARNING_MSG = 
			"The data check found possible errors (warnings) in the data";
	private static final String SANITY_CHECK_SUCCESS_MSG =
			"The data check did not find any problems in the data";

	private static final String DATA_NEVER_CHECKED_HTML = 
			"<h3>Warning: Data has not been checked.</h3>" +
			"<p>You will need need to check the data in a dataset before you " +
			"can submit the dataset for QC.  This data check will identify " +
			"errors in the data and discover incorrectly identified or unknown " +
			"data columns.  Although this data check can be run any time before " +
			"submitting a dataset, we recommend running it immediately after " +
			"uploading a dataset so that any incorrectly identified or unknown " +
			"data columns are correctly identified for subsequent dataset " +
			"uploads.</p>" +
			"<p>Leave this page without checking the data?</p>";
	private static final String CHANGES_NOT_SAVED_HTML =
			"<h3>Warning: Column settings not saved.</h3>" +
			"<p>Changes to data column types, units, and missing values are not " +
			"saved until the data has been checked with this updated data column " +
			"information.</p>" +
			"<p>Leave this page and lose the changes you have made?</p>";
	private static final String RETURN_TO_CRUISE_LIST_TEXT = "Yes";
	private static final String STAY_ON_THIS_PAGE_TEXT = "No";

	interface DataColumnSpecsPageUiBinder extends UiBinder<Widget, DataColumnSpecsPage> {
	}

	private static DataColumnSpecsPageUiBinder uiBinder = 
			GWT.create(DataColumnSpecsPageUiBinder.class);

	private static DashboardServicesInterfaceAsync service = 
			GWT.create(DashboardServicesInterface.class);

	@UiField InlineLabel titleLabel;
	@UiField InlineLabel userInfoLabel;
	@UiField Button logoutButton;
	@UiField HTML introHtml;
	@UiField ScrollPanel dgScroll;
	@UiField DataGrid<ArrayList<String>> dataGrid;
	@UiField Label pagerLabel;
	@UiField Label messagesLabel;
	@UiField SimplePager gridPager;
	@UiField Button messagesButton;
	@UiField Button submitButton;
	@UiField Button cancelButton;
	@UiField Button saveButton;
	
	private SingleSelectionModel<ArrayList<String>> selectionModel;
	private ADCMessageList datasetMessages;
	private Map<Integer, ADCMessage> rowMsgMap = new HashMap<Integer, ADCMessage>();

	// Popup to confirm continue with default zero seconds
	private DashboardAskPopup defaultSecondsPopup;
	// List of all known user data column types
	private ArrayList<DataColumnType> knownUserTypes;
	// Dataset associated with and updated by this page
	private DashboardDataset cruise;
	// List of DatasetDataColumn objects associated with the column Headers
	private ArrayList<DatasetDataColumn> cruiseDataCols;
	// Asynchronous data provider for the data grid 
	private AsyncDataProvider<ArrayList<String>> dataProvider;
	// Dialog warning that data has never been checked or changes have not been saved
	private DashboardAskPopup notCheckedPopup;
	// Flag indicating if the cruise data was ever checked
	private boolean cruiseNeverChecked;
	// Flag indicating if a logout generated the above warnings
	private boolean wasLoggingOut;
	// List of cruises to be assigned once this page is dismissed
	private ArrayList<String> expocodes;

	protected int selectedRow;

	protected int selectedColumn;

	// Singleton instance of this page
	private static DataColumnSpecsPage singleton = null;

	/**
	 * Creates an empty cruise data column specification page.  
	 * Allows the user to update the data column types for a
	 * cruise when populated.
	 */
	DataColumnSpecsPage() {
		initWidget(uiBinder.createAndBindUi(this));
		singleton = this;

		setUsername(null);
		defaultSecondsPopup = null;
		notCheckedPopup = null;
		cruiseNeverChecked = false;
		wasLoggingOut = false;

		titleLabel.setText(TITLE_TEXT);
		logoutButton.setText(LOGOUT_TEXT);
		messagesButton.setText(MESSAGES_TEXT);
		pagerLabel.setText(PAGER_LABEL_TEXT);
		submitButton.setText(SUBMIT_TEXT);
		cancelButton.setText(CANCEL_TEXT);
		saveButton.setText(SAVE_BUTTON_TEXT);

		knownUserTypes = new ArrayList<DataColumnType>();
		cruise = new DashboardDataset();
		cruiseDataCols = new ArrayList<DatasetDataColumn>();
		expocodes = new ArrayList<String>();

		selectionModel = new SingleSelectionModel<ArrayList<String>>(new ProvidesKey<ArrayList<String>>() {
			@Override
			public Object getKey(ArrayList<String> item) {
				return item.get(0);
			}
		});
		dataGrid.setRowStyles(new RowStyles<ArrayList<String>>() {
			@Override
			public String getStyleNames(ArrayList<String> row, int rowIndex) {
				String rowStyle = "oa_row ";
				ADCMessage amsg = rowMsgMap.get(rowIndex);
				if ( amsg != null ) {
					switch (amsg.getSeverity()) {
						case WARNING:
							rowStyle += "oa_datagrid_checker_warning_row";
							break;
						case ERROR:
						case CRITICAL:
							rowStyle += "oa_datagrid_checker_error_row";
							break;
						default:
					}
				}
				return rowStyle;
			}
		});
		// Create the asynchronous data provider for the data grid
		dataProvider = new AsyncDataProvider<ArrayList<String>>() {
			@Override
			protected void onRangeChanged(HasData<ArrayList<String>> display) {
				// Ignore the call if there is no dataset assigned
				if ( cruise.getDatasetId().isEmpty() )
					return;
				UploadDashboard.showWaitCursor();
				// Get the data for the cruise from the server
				final Range range = display.getVisibleRange();
				service.getDataWithRowNum(getUsername(), cruise.getDatasetId(), 
						range.getStart(), range.getLength(), 
						new AsyncCallback<ArrayList<ArrayList<String>>>() {
					@Override
					public void onSuccess(ArrayList<ArrayList<String>> newData) {
						int actualStart;
						try {
							actualStart = IntegerParser.instance().parse(newData.get(0).get(0).trim()) - 1;
						} catch (ParseException e) {
							actualStart = -1;
						}
						if ( actualStart < 0 )
							actualStart = range.getStart();
						updateRowData(actualStart, newData);
						updateScroll();
						UploadDashboard.showAutoCursor();
					}
					@Override
					public void onFailure(Throwable ex) {
						UploadDashboard.showFailureMessage(MORE_DATA_FAIL_MSG, ex);
						UploadDashboard.showAutoCursor();
					}
				});
			}
		};
		dataProvider.addDataDisplay(dataGrid);
		// Assign the pager controlling which rows of the the data grid are shown
		gridPager.setDisplay(dataGrid);
		
		dataGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<ArrayList<String>>() {
			@Override
			public void onCellPreview(CellPreviewEvent<ArrayList<String>> event) {
				String eType = event.getNativeEvent().getType();
				if ( BrowserEvents.CLICK.equals(eType)) {
					selectedRow = event.getIndex();
					selectedColumn = event.getColumn();
				}
			}
		});
		dataGrid.addDomHandler(new DoubleClickHandler() {
			
			@Override
			public void onDoubleClick(DoubleClickEvent event) {
				ADCMessage amsg = rowMsgMap.get(selectedRow);
				if ( amsg == null ) { return; }
//				NativeEvent nevt = event.getNativeEvent();
				int left = event.getClientX();
				int top = event.getClientY();
				String msg = amsg.getDetailedComment();
				PopupMsg pu = new PopupMsg(msg);
				pu.setPopupPosition(left, top);
				pu.show();
			}
		}, DoubleClickEvent.getType());
	}

	Integer scrollToRow = null;
	Integer scrollToCol = null;
	
	private void updateScroll() {
		if ( scrollToRow != null ) {
			int rowIdx = scrollToRow.intValue();
			int colIdx = scrollToCol.intValue();
			TableRowElement row = dataGrid.getRowElement(rowIdx);
			TableCellElement cell = row.getCells().getItem(colIdx);
			cell.scrollIntoView();
//			int cellTop = dgScroll.getVerticalScrollPosition();
//			int cellHeight = cell.getScrollHeight();
//			int cellLeft = dgScroll.getHorizontalScrollPosition();
//			int cellWidth = cell.getScrollWidth();
////			cell.setScrollTop(cellTop - ( 2 * cellHeight ));
////			cell.setScrollLeft(cellLeft + ( 2 * cellWidth ));
//			int rowPos = rowIdx * cellHeight;
//			dgScroll.setVerticalScrollPosition(rowPos);
		}
		scrollToRow = null;
		scrollToCol = null;
		
//		TableCellElement idCol = row.getCells().getItem(0);
//		String id = String.valueOf(idCol.getInnerText());
//		ArrayList<String> al = new ArrayList<String>();
//		al.add(id);
//		selectionModel.setSelected(al, true);
	}
	
	/**
	 * Display the cruise data column specifications page for a cruise
	 * with the latest cruise data column specifications from the server.
	 * Adds this page to the page history.
	 * 
	 * @param username
	 * 		username for this page
	 * @param dataset
	 * 		show the specifications for this cruise
	 */
	static void showPage(String username, ArrayList<String> expocodes) {
		if ( singleton == null )
			singleton = new DataColumnSpecsPage();

		singleton.setUsername(username);
		singleton.expocodes.clear();
		singleton.expocodes.addAll(expocodes);
		UploadDashboard.showWaitCursor();
		service.getDataColumnSpecs(singleton.getUsername(), expocodes.get(0), 
								new AsyncCallback<TypesDatasetDataPair>() {
			@Override
			public void onSuccess(TypesDatasetDataPair cruiseSpecs) {
				if ( cruiseSpecs != null ) {
					UploadDashboard.updateCurrentPage(singleton);
					singleton.knownUserTypes.clear();
					if ( cruiseSpecs.getAllKnownTypes() != null )
						singleton.knownUserTypes.addAll(cruiseSpecs.getAllKnownTypes());
					singleton.updateDatasetMessages(cruiseSpecs.getMsgList());
					singleton.updateDatasetMessages(cruiseSpecs.getDatasetData());
					singleton.updateDatasetColumnSpecs(cruiseSpecs.getDatasetData());
					History.newItem(PagesEnum.IDENTIFY_COLUMNS.name(), false);
				}
				else {
					UploadDashboard.showMessage(GET_COLUMN_SPECS_FAIL_MSG + 
						" (unexpected null cruise column specificiations)");
				}
				UploadDashboard.showAutoCursor();
			}
			@Override
			public void onFailure(Throwable ex) {
				UploadDashboard.showFailureMessage(GET_COLUMN_SPECS_FAIL_MSG, ex);
				UploadDashboard.showAutoCursor();
			}
		});
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

	static void redisplayPage(String username, Integer rowNumber, Integer columnNumber) {
		if ( (username == null) || username.isEmpty() || 
			 (singleton == null) || ! singleton.getUsername().equals(username) ) {
			DatasetListPage.showPage();
		}
		else {
			UploadDashboard.updateCurrentPage(singleton);
			singleton.setView(rowNumber, columnNumber);
		}
	}
	
	void setView(Integer rowNumber, Integer columnNumber) {
		int showColumnIdx = columnNumber == null || columnNumber.equals(DashboardUtils.INT_MISSING_VALUE) ? 
								0 : columnNumber.intValue()-1;
		int showRowIdx = rowNumber == null || rowNumber.equals(DashboardUtils.INT_MISSING_VALUE) ? 
								0 : rowNumber.intValue()-1;
		int pageSize = gridPager.getPageSize();
		int showPage = showRowIdx / pageSize;
		int pageRow = showRowIdx % pageSize;
		int cPage = gridPager.getPage();
		if ( cPage != showPage ) {
			scrollToRow = new Integer(pageRow);
			scrollToCol = new Integer(showColumnIdx);
			gridPager.setPage(showPage);
		}
		
//			int dgHeight = singleton.dataGrid.getOffsetHeight();
//			int vpos = singleton.dgScroll.getVerticalScrollPosition();
//			int hpos = singleton.dgScroll.getHorizontalScrollPosition();
//			Window.alert("h:"+dgHeight+", v:"+vpos+", h:"+hpos);
	}

	/**
	 * Updates the data column specification page with the given
	 * column types and data.  Modifies cruiseSpecs in that the
	 * dashboard-generated sample number is inserted at the beginning
	 * of each row of data. 
	 * 
	 * @param cruiseSpecs
	 * 		current cruise data column type specifications and
	 * 		initial cruise data for display
	 */
	private void updateDatasetColumnSpecs(DashboardDatasetData cruiseSpecs) {
		userInfoLabel.setText(WELCOME_INTRO + getUsername());

		String status = cruiseSpecs.getDataCheckStatus();
		if ( status.equals(DashboardUtils.CHECK_STATUS_NOT_CHECKED) ||
			 status.equals(DashboardUtils.CHECK_STATUS_UNACCEPTABLE) ) {
			cruiseNeverChecked = true;
			messagesLabel.setText(NOT_CHECKED_MSG);
		}
		else {
			cruiseNeverChecked = false;
			String msgText;
			int numErrors = cruiseSpecs.getNumErrorRows();
			if ( numErrors == 0 )
				msgText = "no";
			else
				msgText = Integer.toString(numErrors);
			msgText += " errors; ";
			int numWarns = cruiseSpecs.getNumWarnRows();
			if ( numWarns == 0 )
				msgText += "no";
			else
				msgText += Integer.toString(numWarns);
			msgText += " warnings";
			messagesLabel.setText(msgText);
		}

		if ( Boolean.TRUE.equals(cruiseSpecs.isEditable()) ) {
			submitButton.setEnabled(true);
			submitButton.setTitle(ENABLED_SUBMIT_HOVER_HELP);
			saveButton.setEnabled(true);
			saveButton.setTitle(SAVE_BUTTON_HOVER_HELP);
		}
		else {
			submitButton.setEnabled(false);
			submitButton.setTitle(DISABLED_SUBMIT_HOVER_HELP);
			saveButton.setEnabled(false);
			saveButton.setTitle(DISABLED_SUBMIT_HOVER_HELP);
		}

		// Clear the dataset in case the data provider gets called while clearing
		cruise.setDatasetId(null);

		// Delete any existing columns and headers
		int k = dataGrid.getColumnCount();
		while ( k > 0 ) {
			k--;
			dataGrid.removeColumn(k);
		}
		// Clear the list of DatasetDataColumns
		cruiseDataCols.clear();

		// Assign the new cruise information needed by this page
		cruise.setNumDataRows(cruiseSpecs.getNumDataRows());
		cruise.setUserColNames(cruiseSpecs.getUserColNames());
		cruise.setDataColTypes(cruiseSpecs.getDataColTypes());

		TreeSet<QCFlag> woceSet = new TreeSet<QCFlag>();
		for ( QCFlag chkwoce : cruiseSpecs.getCheckerFlags() )
			woceSet.add(new QCFlag(null, null, chkwoce.getSeverity(), chkwoce.getColumnIndex(), chkwoce.getRowIndex()));
		cruise.setCheckerFlags(woceSet);

		woceSet.clear();
		for ( QCFlag uwoce : cruiseSpecs.getUserFlags() )
			woceSet.add(new QCFlag(null, null, uwoce.getSeverity(), null, uwoce.getRowIndex()));
		cruise.setUserFlags(woceSet);

		cruise.setSubmitStatus(cruiseSpecs.getSubmitStatus());
		cruise.setArchiveStatus(cruiseSpecs.getArchiveStatus());
		cruise.setDatasetId(cruiseSpecs.getDatasetId());

		introHtml.setHTML(INTRO_HTML_PROLOGUE +  
				SafeHtmlUtils.htmlEscape(cruise.getDatasetId()) + 
				INTRO_HTML_EPILOGUE);

		// Rebuild the data grid using the provided DatasetDataColumnSpecs
		if ( cruise.getDataColTypes().size() < 4 )
			throw new IllegalArgumentException(
					"Unexpected small number of data columns: " + 
					cruise.getDataColTypes().size());
		int minTableWidth = 2;
		// First column is the dashboard-generated sample number (no header)
		ArrayListTextColumn rowNumColumn = new ArrayListTextColumn(0, cruise, rowMsgMap);
		dataGrid.addColumn(rowNumColumn);
		dataGrid.setColumnWidth(rowNumColumn, UploadDashboard.NARROW_COLUMN_WIDTH, Style.Unit.EM);
		minTableWidth += UploadDashboard.NARROW_COLUMN_WIDTH;
		// Rest of the columns are actual data columns
		for (k = 0; k < cruise.getDataColTypes().size(); k++) {
			// TextColumn for displaying the data strings for this column
			ArrayListTextColumn dataColumn = new ArrayListTextColumn(k+1, cruise, rowMsgMap);
			// DatasetDataColumn for creating the Header cell for this column
			DatasetDataColumn cruiseColumn = new DatasetDataColumn(knownUserTypes, cruise, k);
			// Maintain a reference to the DatasetDataColumn object
			cruiseDataCols.add(cruiseColumn);
			// Add this data column and the header to the grid
			dataGrid.addColumn(dataColumn, cruiseColumn.getHeader());
			// Set the width of this column - all the same width
			dataGrid.setColumnWidth(dataColumn, DATA_COLUMN_WIDTH, Style.Unit.EM);
			// Add this width to the minimum table width
			minTableWidth += DATA_COLUMN_WIDTH;
		}
		// Set the minimum table width
		dataGrid.setMinimumTableWidth(minTableWidth, Style.Unit.EM);
		// Update the data provider with the data in the DatasetDataColumnSpecs
		dataProvider.updateRowCount(cruise.getNumDataRows(), true);
		// Just insert the row numbers into each data row (modifying cruiseSpecs)
		ArrayList<ArrayList<String>> dataWithRowNums = cruiseSpecs.getDataValues();
		ArrayList<Integer> rowNums = cruiseSpecs.getRowNums();
		k = 0;
		for ( ArrayList<String> dataRow : dataWithRowNums ) {
			String rowNumStr = rowNums.get(k).toString();
			dataRow.add(0, rowNumStr);
			k++;
		}
		
		dataProvider.updateRowData(0, cruiseSpecs.getDataValues());
		// Reset shown rows to the start of the data
		dataGrid.setPageStart(0);
		// Set the number of data rows to display in the grid.
		// This will refresh the view.
		dataGrid.setPageSize(DashboardUtils.MAX_ROWS_PER_GRID_PAGE);
	}

	private void updateDatasetMessages(DashboardDatasetData cruiseSpecs) {
		for ( QCFlag flag : cruiseSpecs.getUserFlags()) {
			Integer rowIdx = flag.getRowIndex();
			if ( DashboardUtils.INT_MISSING_VALUE.equals(rowIdx)) { continue; }
			ADCMessage m = new ADCMessage();
			m.setSeverity(flag.getSeverity());
			m.setColIndex(flag.getColumnIndex());
			m.setRowIndex(rowIdx);
			String msg = "User QC Flag set for row " + (rowIdx.intValue()+1);
			Integer colIdx = flag.getColumnIndex();
			if ( ! DashboardUtils.INT_MISSING_VALUE.equals(colIdx)) { 
				msg += " on column " + (colIdx.intValue()+1);
			}
			m.setDetailedComment(msg);
			rowMsgMap.put(rowIdx, m);
		}
	}
	protected void updateDatasetMessages(ADCMessageList msgList) {
		rowMsgMap.clear();
		datasetMessages = msgList;
		if ( datasetMessages != null ) {
			for (ADCMessage msg : datasetMessages) {
				rowMsgMap.put(msg.getRowIndex(), msg);
			}
		}
	}

	@UiHandler("logoutButton")
	void logoutOnClick(ClickEvent event) {
		// Check if any changes have been made
		boolean hasChanged = false;
		for ( DatasetDataColumn dataCol : cruiseDataCols ) {
			if ( dataCol.hasChanged() ) {
				hasChanged = true;
				break;
			}
		}
		if ( hasChanged ) {
			// Ask before logging out
			wasLoggingOut = true;
			if ( notCheckedPopup == null )
				makeNotCheckedPopup();
			notCheckedPopup.askQuestion(CHANGES_NOT_SAVED_HTML);
		}
		else {
			// No changes; just log out
			DashboardLogoutPage.showPage();
		}
	}

	@UiHandler("cancelButton")
	void cancelOnClick(ClickEvent event) {
		// Check if any changes have been made
		boolean hasChanged = false;
		for ( DatasetDataColumn dataCol : cruiseDataCols ) {
			if ( dataCol.hasChanged() ) {
				hasChanged = true;
				break;
			}
		}
		if ( hasChanged ) {
			// Ask before returning to the cruise list
			wasLoggingOut = false;
			if ( notCheckedPopup == null )
				makeNotCheckedPopup();
			notCheckedPopup.askQuestion(CHANGES_NOT_SAVED_HTML);
		}
		else if ( cruiseNeverChecked ) {
			// Ask before returning to the cruise list
			wasLoggingOut = false;
			if ( notCheckedPopup == null )
				makeNotCheckedPopup();
			notCheckedPopup.askQuestion(DATA_NEVER_CHECKED_HTML);
		}
		else {
			// No changes since last update
			// If only one cruise, done
			if ( expocodes.size() < 2 ) {
				DatasetListPage.showPage();
				return;
			}
			// Put up the wait cursor and send the rest of the cruises through the sanity checker
			UploadDashboard.showWaitCursor();
			expocodes.remove(0);
			service.updateDataColumns(getUsername(), expocodes, new AsyncCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					// Go to the list of cruises without comment; return to the normal cursor
					DatasetListPage.showPage();
					UploadDashboard.showAutoCursor();
					return;
				}
				@Override
				public void onFailure(Throwable caught) {
					// Go to the list of cruises without comment; return to the normal cursor
					DatasetListPage.showPage();
					UploadDashboard.showAutoCursor();
					return;
				}
			});
		}
	}

	/**
	 * Generate the question popup warning that the data has never been checked
	 */
	private void makeNotCheckedPopup() {
		notCheckedPopup = new DashboardAskPopup(RETURN_TO_CRUISE_LIST_TEXT, 
				STAY_ON_THIS_PAGE_TEXT, new AsyncCallback<Boolean>() {
			@Override
			public void onSuccess(Boolean result) {
				if ( result ) {
					if ( wasLoggingOut ) {
						wasLoggingOut = false;
						DashboardLogoutPage.showPage();
					}
					else {
						// Return to the latest cruise listing page, which may  
						// have been updated from previous actions on this page.
						DatasetListPage.showPage();
					}
				}
				else {
					// Just stay on this page
					wasLoggingOut = false;
				}
			}
			@Override
			public void onFailure(Throwable ex) {
				// never called
				;
			}
		});
	}

	@UiHandler("messagesButton") 
	void showMessagesOnClick(ClickEvent event) {
		DataMessagesPage.showPage(getUsername(), cruise.getDatasetId());
	}

	@UiHandler("submitButton")
	void submitOnClick(ClickEvent event) {
		if ( ! Boolean.TRUE.equals(cruise.isEditable()) ) {
			// Should never get here, but just in case
			UploadDashboard.showMessage(DISABLED_SUBMIT_HOVER_HELP);
			return;
		}

		// longitude given?
		boolean hasLongitude = false;
		// latitude given?
		boolean hasLatitude = false;
		// sample depth given?
		boolean hasDepth = false;
		// date/time given?
		boolean hasYear = false;
		boolean hasMonth = false;
		boolean hasDay = false;
		boolean hasHour = false;
		boolean hasMinute = false;
		boolean hasSecond = false;

		// list of data columns still given as unknown
		ArrayList<Integer> unknownIndices = new ArrayList<Integer>();

		// Check the column types 
		// Marking what is given no longer tries to guess what the sanity checker
		// can work with for time and date; only the possibility.
		// Similarly with aqueous CO2 and fCO2 recomputations.
		int k = 0;
		for ( DataColumnType colType : cruise.getDataColTypes() ) {
			if ( DashboardUtils.UNKNOWN.typeNameEquals(colType) ) {
				unknownIndices.add(k);
			}
			else if ( DashboardUtils.TIMESTAMP.typeNameEquals(colType) ) {
				hasYear = true;
				hasMonth = true;
				hasDay = true;
				hasHour = true;
				hasMinute = true;
				hasSecond = true;
			}
			else if ( DashboardUtils.DATE.typeNameEquals(colType) ) {
				hasYear = true;
				hasMonth = true;
				hasDay = true;
			}
			else if ( DashboardUtils.DAY_OF_YEAR.typeNameEquals(colType) ) {
				hasMonth = true;
				hasDay = true;
				// Day of year could be floating point; verification needed?
				hasHour = true;
				hasMinute = true;
				hasSecond = true;
			}
			else if ( DashboardUtils.SECOND_OF_DAY.typeNameEquals(colType) ) {
				hasHour = true;
				hasMinute = true;
				hasSecond = true;
			}
			else if ( DashboardUtils.YEAR.typeNameEquals(colType) ) {
				hasYear = true;
			}
			else if ( DashboardUtils.MONTH_OF_YEAR.typeNameEquals(colType) ) {
				hasMonth = true;
			}
			else if ( DashboardUtils.DAY_OF_MONTH.typeNameEquals(colType) ) {
				hasDay = true;
			}
			else if ( DashboardUtils.TIME_OF_DAY.typeNameEquals(colType) ) {
				hasHour = true;
				hasMinute = true;
				hasSecond = true;
			}
			else if ( DashboardUtils.HOUR_OF_DAY.typeNameEquals(colType) ) {
				hasHour = true;
			}
			else if ( DashboardUtils.MINUTE_OF_HOUR.typeNameEquals(colType) ) {
				hasMinute = true;
			}
			else if ( DashboardUtils.SECOND_OF_MINUTE.typeNameEquals(colType) ) {
				hasSecond = true;
			}
			else if ( DashboardUtils.LONGITUDE.typeNameEquals(colType) ) {
				hasLongitude = true;
			}
			else if ( DashboardUtils.LATITUDE.typeNameEquals(colType) ) {
				hasLatitude = true;
			}
			else if ( DashboardUtils.SAMPLE_DEPTH.typeNameEquals(colType) ) {
				hasDepth = true;
			}
			k++;
		}
		if ( unknownIndices.size() > 0 ) {
			// Unknown column data types found; put up error message and return
			ArrayList<String> colNames = cruise.getUserColNames();
			String errMsg = Integer.toString(unknownIndices.size()) + 
					UNKNOWN_COLUMN_TYPE_PROLOGUE;
			int cnt = 0;
			for ( int idx : unknownIndices ) {
				cnt++;
				if ( (cnt == 5) && (unknownIndices.size() > 5) ) {
					errMsg += "<li> ... </li>";
					break;
				}
				errMsg += "<li>" + SafeHtmlUtils.htmlEscape(colNames.get(idx)) + "</li>";
			}
			errMsg += UNKNOWN_COLUMN_TYPE_EPILOGUE;
			UploadDashboard.showMessage(errMsg);
			return;
		}
		if ( ! hasLongitude ) {
			// no longitude - error
			UploadDashboard.showMessage(NO_LONGITUDE_ERROR_MSG);
			return;
		}
		if ( ! hasLatitude ) {
			// no latitude - error
			UploadDashboard.showMessage(NO_LATITUDE_ERROR_MSG);
			return;
		}
		if ( ! hasDepth ) {
			// no sample depth - error
			UploadDashboard.showMessage(NO_DEPTH_ERROR_MSG);
			return;
		}
		if ( ! (hasYear || hasMonth || hasDay || hasHour || hasMinute) ) {
			// timestamp completely missing - error
			UploadDashboard.showMessage(NO_TIMESTAMP_ERROR_MSG);
			return;
		}
		if ( ! (hasYear || hasMonth || hasDay) ) {
			// date completely missing - error
			UploadDashboard.showMessage(NO_DATE_ERROR_MSG);
			return;
		}
		if ( ! (hasHour || hasMinute) ) {
			// time completely missing - error
			UploadDashboard.showMessage(NO_TIME_ERROR_MSG);
			return;
		}
		if ( ! (hasYear && hasMonth && hasDay) ) {
			// incomplete date given - error
			UploadDashboard.showMessage(MISSING_DATE_PIECE_ERROR_MSG);
			return;
		}
		if ( ! (hasHour && hasMinute) ) {
			// incomplete time given - error
			UploadDashboard.showMessage(MISSING_TIME_PIECE_ERROR_MSG);
			return;
		}

		// Make sure there is no more than one of each column types - except OTHER
		HashSet<String> typeSet = new HashSet<String>();
		TreeSet<String> duplicates = new TreeSet<String>();
		for ( DataColumnType colType : cruise.getDataColTypes() ) {
			if ( DashboardUtils.OTHER.typeNameEquals(colType) ) {
				// Multiple OTHER column types are allowed
				;
			}
			else if ( ! typeSet.add(colType.getDisplayName()) ) {
				duplicates.add(colType.getDisplayName());
			}
		}
		if ( duplicates.size() > 0 ) {
			String errMsg = MULTIPLE_COLUMN_TYPES_ERROR_MSG;
			int cnt = 0;
			for ( String displayName : duplicates ) {
				cnt++;
				if ( (cnt == 5) && (unknownIndices.size() > 5) ) {
					errMsg += "<li> ... </li>";
					break;
				}
				errMsg += "<li>" + displayName + "</li>";
			}
			UploadDashboard.showMessage(errMsg);
			return;
		}

		if ( ! hasSecond ) {
			// Warning about missing seconds, asking whether to continue
			if ( defaultSecondsPopup == null ) {
				defaultSecondsPopup = new DashboardAskPopup(USE_DEFAULT_SECONDS_TEXT,
						NO_DEFAULT_SECONDS_TEXT, new AsyncCallback<Boolean>() {
					@Override
					public void onSuccess(Boolean okay) {
						// Only continue if okay to use default zero for seconds
						if ( okay )
							doSubmit();
					}
					@Override
					public void onFailure(Throwable caught) {
						// never called
						;
					}
				});
			}
			defaultSecondsPopup.askQuestion(DEFAULT_SECONDS_WARNING_QUESTION);
			return;
		}

		// longitude, latitude, sea water co2, and some form of a timestamp 
		// is present so continue on  
		doSubmit();
	}

	private void doSubmit() {
		// Show the wait cursor
		UploadDashboard.showWaitCursor();
		// Submit the updated data column types to the server.
		// This update invokes the SanityChecker on the data and
		// the results are then reported back to this page.
		service.updateDataColumnSpecs(getUsername(), cruise, 
				new AsyncCallback<DashboardDatasetData>() {
			@Override
			public void onSuccess(DashboardDatasetData specs) {
				if ( specs == null ) {
					UploadDashboard.showMessage(SUBMIT_FAIL_MSG + 
							" (unexpected null cruise information returned)");
					// Show the normal cursor
					UploadDashboard.showAutoCursor();
					return;
				}
				updateDatasetColumnSpecs(specs);
				String status = specs.getDataCheckStatus();
				if ( status.equals(DashboardUtils.CHECK_STATUS_NOT_CHECKED) ||
					 status.equals(DashboardUtils.CHECK_STATUS_UNACCEPTABLE) ) {
					// the sanity checker had serious problems
					UploadDashboard.showMessage(SANITY_CHECK_FAIL_MSG);
				}
				else if ( status.startsWith(DashboardUtils.CHECK_STATUS_ERRORS_PREFIX) ) {
					// errors issued
					UploadDashboard.showMessage(SANITY_CHECK_ERROR_MSG);
				}
				else if ( status.startsWith(DashboardUtils.CHECK_STATUS_WARNINGS_PREFIX) ) {
					// warnings issued
					UploadDashboard.showMessage(SANITY_CHECK_WARNING_MSG);
				}
				else {
					// no problems
					UploadDashboard.showMessage(SANITY_CHECK_SUCCESS_MSG);
				}
				// Show the normal cursor
				UploadDashboard.showAutoCursor();
			}
			@Override
			public void onFailure(Throwable ex) {
				UploadDashboard.showFailureMessage(SUBMIT_FAIL_MSG, ex);
				// Show the normal cursor
				UploadDashboard.showAutoCursor();
			}
		});
	}

	@UiHandler("saveButton")
	void saveOnClick(ClickEvent event) {
		if ( ! Boolean.TRUE.equals(cruise.isEditable()) ) {
			// Should never get here, but just in case
			UploadDashboard.showMessage(DISABLED_SUBMIT_HOVER_HELP);
			return;
		}
		boolean hasChanged = false;
		for ( DatasetDataColumn dataCol : cruiseDataCols ) {
			if ( dataCol.hasChanged() ) {
				hasChanged = true;
				break;
			}
		}
		if ( hasChanged ) {
			doSave();
		} else {
			UploadDashboard.showMessage("There have been no changes to data column definitions.");
		}
	}

	private void doSave() {
		// Show the wait cursor
		UploadDashboard.showWaitCursor();
		// Submit the updated data column types to the server.
		// This update invokes the SanityChecker on the data and
		// the results are then reported back to this page.
		service.saveDataColumnSpecs(getUsername(), cruise, 
				new AsyncCallback<DashboardDatasetData>() {
			@Override
			public void onSuccess(DashboardDatasetData specs) {
				if ( specs == null ) {
					UploadDashboard.showMessage(SAVE_FAIL_MSG + " (unexpected null information returned)");
				} else {
					UploadDashboard.showMessage("Data column definitions saved.");
					updateDatasetColumnSpecs(specs);
				}
				UploadDashboard.showAutoCursor();
			}
			@Override
			public void onFailure(Throwable ex) {
				UploadDashboard.showFailureMessage(SAVE_FAIL_MSG, ex);
				// Show the normal cursor
				UploadDashboard.showAutoCursor();
			}
		});
		
	}

//	/**
//	 * TextColumn for displaying the value at a given index 
//	 * of an ArrayList of Strings 
//	 */
//	private class ArrayListTextColumn extends TextColumn<ArrayList<String>> {
//		private int colNum;
//		/**
//		 * Creates a TextColumn for an ArrayList of Strings that 
//		 * displays the value at the given index in the ArrayList.
//		 * @param colNum
//		 * 		display data at this index of the ArrayList
//		 */
//		ArrayListTextColumn(int colNum) {
//			super();
//			this.colNum = colNum;
//		}
//		@Override
//		public String getValue(ArrayList<String> dataRow) {
//			if ( (dataRow != null) && (0 <= colNum) && (colNum < dataRow.size()) )
//				return dataRow.get(colNum);
//			return "";
//		}
//		private void closeTitle(SafeHtmlBuilder sb) {
//			sb.appendHtmlConstant("</div>");
//		}
//		@Override
//		public void render(Cell.Context ctx, ArrayList<String> obj, SafeHtmlBuilder sb) {
//			Integer rowIdx = ctx.getIndex();
//			ADCMessage msg = rowMsgMap.get(rowIdx);
//			boolean addedTitle = false;
//			if ( msg != null ) {
//				sb.appendHtmlConstant("<div title=\"" + msg.getDetailedComment() + "\">");
//				addedTitle = true;
//			}
//			if ( colNum == 0 ) {
//				sb.appendHtmlConstant("<div style=\"color:" + 
//						UploadDashboard.ROW_NUMBER_COLOR + ";text-align:right\">");
//				sb.appendEscaped(getValue(obj));
//				sb.appendHtmlConstant("</div>");
//				if ( addedTitle ) closeTitle(sb);
//				return;
//			}
//			TreeSet<QCFlag> checkerFlags = cruise.getCheckerFlags();
//			QCFlag woceCell = new QCFlag(null, null, Severity.ERROR, colNum-1, rowIdx);
//			QCFlag woceRow = new QCFlag(null, null, Severity.ERROR, null, rowIdx);
//			QCFlag woceCol = new QCFlag(null, null, Severity.ERROR, colNum-1, null);
//			if ( checkerFlags.contains(woceCell) || 
//				 checkerFlags.contains(woceRow) || 
//				 checkerFlags.contains(woceCol) ) {
//				sb.appendHtmlConstant("<div style=\"background-color:" + 
//						UploadDashboard.CHECKER_ERROR_COLOR + ";font-weight:bold;\">");
//				sb.appendEscaped(getValue(obj));
//				sb.appendHtmlConstant("</div>");
//				if ( addedTitle ) closeTitle(sb);
//				return;
//			}
//			TreeSet<QCFlag> userFlags = cruise.getUserFlags();
//			if ( userFlags.contains(woceCell) || 
//				 userFlags.contains(woceRow) || 
//				 userFlags.contains(woceCol) ) {
//				sb.appendHtmlConstant("<div style=\"background-color:" + 
//						UploadDashboard.USER_ERROR_COLOR + ";\">");
//				sb.appendEscaped(getValue(obj));
//				sb.appendHtmlConstant("</div>");
//				if ( addedTitle ) closeTitle(sb);
//				return;
//			}
//			woceCell.setSeverity(Severity.WARNING);
//			woceRow.setSeverity(Severity.WARNING);
//			woceCol.setSeverity(Severity.WARNING);
//			if ( checkerFlags.contains(woceCell) || 
//				 checkerFlags.contains(woceRow) || 
//				 checkerFlags.contains(woceCol) ) {
//				sb.appendHtmlConstant("<div style=\"background-color:" + 
//						UploadDashboard.CHECKER_WARNING_COLOR + ";font-weight:bold;\">");
//				sb.appendEscaped(getValue(obj));
//				sb.appendHtmlConstant("</div>");
//				if ( addedTitle ) closeTitle(sb);
//				return;
//			}
//			if ( userFlags.contains(woceCell) || 
//				 userFlags.contains(woceRow) || 
//				 userFlags.contains(woceCol) ) {
//				sb.appendHtmlConstant("<div style=\"background-color:" + 
//						UploadDashboard.USER_WARNING_COLOR + ";\">");
//				sb.appendEscaped(getValue(obj));
//				sb.appendHtmlConstant("</div>");
//				if ( addedTitle ) closeTitle(sb);
//				return;
//			}
//			// Render normally
//			super.render(ctx, obj, sb);
//			// Shouldn't happen, but ...
//			if ( addedTitle ) closeTitle(sb);
//		}
//	}

}
