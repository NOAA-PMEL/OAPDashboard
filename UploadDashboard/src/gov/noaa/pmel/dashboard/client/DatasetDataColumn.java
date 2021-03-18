/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.cell.client.AbstractInputCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.logging.client.ConsoleLogHandler;
import com.google.gwt.safehtml.shared.HtmlSanitizer;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.cellview.client.CellWidget;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ScrollPanel;

import gov.noaa.pmel.dashboard.client.DataTypeSelectorWidget.UpdateInformation;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.QCFlag;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;

/**
 * Class for creating a CompositeCell Header for a cruise data column.
 * The cell includes a selection box for specifying the column type 
 * with units, and a text input for specifying a missing value.
 * 
 * @author Karl Smith
 */
public class DatasetDataColumn {

	static final String DEFAULT_MISSING_VALUE = "(default missing values)";
    
    // List of known types
    private Map<String, DataColumnType> knownTypes;
	// List of all known user data column types and selected units
	private ArrayList<DataColumnType> knownTypeUnitList;
	// List of "<name> [ <unit> ]" strings for all the known user data column types and selected units
	private List<String> typeUnitStringList;
	// Dataset associated with this instance
	private DashboardDataset cruise;
	// Dataset data column index associated with this instance
	private int columnIndex;
	// Header associated with this instance
	private Header<DatasetDataColumn> columnHeader;
	// Flag that something in the column header has hasChanged
	private boolean hasChanged;
	
    private static Logger logger = Logger.getLogger("DatasetDataColumn");

	/**
	 * Specifies a data column of a DashboardDataset.
	 * 
	 * @param knownUserTypes
	 * 		list of all known data column types
	 * @param cruise
	 * 		cruise to associate with this instance
	 * @param column
	 * 		index of the cruise data column to associate with this instance
	 */
	DatasetDataColumn(ArrayList<DataColumnType> knownUserTypes, DashboardDataset cruise, int columnIndex) {
		knownTypeUnitList = new ArrayList<DataColumnType>(2 * knownUserTypes.size());
		knownTypes = new HashMap<>();
		for ( DataColumnType dataType : knownUserTypes ) {
            if ( knownTypes.put(dataType.getVarName(), dataType) != null ) {
                GWT.log("Duplicate varnamem for : " + dataType);
            }
			for (int k = 0; k < dataType.getUnits().size(); k++) {
				DataColumnType dctype = dataType.duplicate();
				dctype.setSelectedUnitIndex(k);
				knownTypeUnitList.add(dctype);
			}
		}
		typeUnitStringList = new ArrayList<String>(knownTypeUnitList.size());
		for ( DataColumnType dctype : knownTypeUnitList ) {
			String displayName = dctype.getDisplayName();
			String unit = dctype.getUnits().get(dctype.getSelectedUnitIndex());
			if ( DashboardUtils.STRING_MISSING_VALUE.equals(unit) ) {
				typeUnitStringList.add(displayName);
			}
			else {
				typeUnitStringList.add(displayName + " [ " + unit + " ]");
			}
		}
		this.cruise = cruise;
		this.columnIndex = columnIndex;
		this.columnHeader = createHeader(cruise.getDataColTypes().get(columnIndex), columnIndex+1);
		this.hasChanged = false;
        
        logger.addHandler(new ConsoleLogHandler());
        logger.setLevel(Level.ALL);
	}

	/**
	 * Returns the header for this cruise data column.  This header is a 
	 * CompositeCell consisting of a TextCell, for displaying the user-
	 * provided column name, a SelectionCell, for selecting the standard 
	 * column type, a TextCell, and a TextInputCell, for the user to 
	 * specify a missing value.
	 */
	Header<DatasetDataColumn> getHeader() {
		return columnHeader;
	}

	/**
	 * @return
	 * 		if the values shown in this header have changed
	 */
	boolean hasChanged() {
		return hasChanged;
	}

//    private CellWidget<String> selectorCellWidget;
//    private CellWidget<String> getSelectorCellWidget() {
//        UploadDashboard.logToConsole("selectorCellWidget:"+selectorCellWidget);
//        return selectorCellWidget;
//    }
	/**
	 * Creates the header for this cruise data column.
	 */
	private Header<DatasetDataColumn> createHeader(final DataColumnType selectedType, 
	                                               final int columnNumber) {

        String columnStyle = "";
        TreeSet<QCFlag> checkerFlags = cruise.getCheckerFlags();
        for ( QCFlag flag : checkerFlags ) {
            Integer flagCol = flag.getColumnIndex();
            Integer flagRow = flag.getRowIndex();
            if ( ( flagRow == null || flagRow.intValue() == DashboardUtils.INT_MISSING_VALUE.intValue() ) 
                    && flagCol != null && flagCol.intValue() == (columnNumber-1) ) {
                Severity s = flag.getSeverity();
                columnStyle = "dataColumnTypeWarning";
            }
        }
		// Create the TextCell giving the column name given by the user
		HasCell<DatasetDataColumn,String> userNameCell = new HasCell<DatasetDataColumn,String>() {
			@Override
			public TextCell getCell() {
				// Return a TextCell which is rendered as a block-level element
				return new TextCell() {
					@Override
					public void render(Cell.Context context, String value, SafeHtmlBuilder sb) {
						super.render(context, "[" + columnNumber + "] " + value, sb);
						sb.appendHtmlConstant("<br />");
					}
				};
			}
			@Override
			public FieldUpdater<DatasetDataColumn,String> getFieldUpdater() {
				return null;
			}
			@Override
			public String getValue(DatasetDataColumn dataCol) {
				return dataCol.cruise.getUserColNames().get(dataCol.columnIndex);
			}
		};

		// Create the SelectionCell listing the known standard headers
		HasCell<DatasetDataColumn,String> stdNameCell = new HasCell<DatasetDataColumn,String>() {
			private ClickableTextCell theCell = null;
//            private CellWidget<String> selectorCellWidget;
            DataTypeSelectorWidget dts = new DataTypeSelectorWidget(knownTypes, 
                                                columnIndex, cruise.getUserColNames().get(columnIndex),
                                                new AsyncCallback<UpdateInformation>() {
                @Override
                public void onSuccess(UpdateInformation info) {
                    GWT.log("DatatTypeSelector returned: " + info.newType);
					hasChanged = true;
                    DataColumnType newType = info.newType;
                    cruise.getDataColTypes().set(columnIndex, info.newType);
                    String displayString = newType.getDisplayName();   
//                    if ( newType.getSelectedUnitIndex() != null) {   // defaults to Integer(0)!
                    ArrayList<String> typeUnits = newType.getUnits();
                    GWT.log("typeUnits:"+typeUnits);
                    if ( typeUnits != null && typeUnits.size() > 0 && typeUnits.get(0).trim().length() > 0 ) {
                        displayString += " ["+newType.getUnits().get(newType.getSelectedUnitIndex().intValue()) + "]";
                    }
                    info.updater.update(displayString);
                }
                @Override
                public void onFailure(Throwable caught) {
                    GWT.log(caught.toString(), caught);
                }
            });
			@Override
			public ClickableTextCell getCell() {
                if ( theCell == null ) {
				// Create a list of all the standard column headers with units;
				// render as a block-level element
                    theCell = 
                              new ClickableTextCell() {
//                            new StyledSuggestBoxCell(typeUnitStringList, "dataColumnSelectionCell") {
    					@Override
    					public void render(Cell.Context context, String value, SafeHtmlBuilder sb) {
//                            GWT.log("render: " + value);
//    						super.render(context, value, sb);
//                            String ctxStr = "context:"+context.getColumn() + ", " + context.getIndex();
//                            if ( value == null ) {
//                                ctxStr = "null value " + ctxStr;
//                            }
//                            GWT.log(ctxStr);
                            if ( value != null ) {
                            sb.appendHtmlConstant("<div class=\"dataColumnType_div\">")
                              .appendHtmlConstant("<img src=\"images/angle-down.svg\" class=\"dataColumnType_icon\" />")
                              .appendHtmlConstant("<div id=\"dcn_"+context.getColumn()+"\" class=\"dataColumnType_name\">"+value+"</div>")
                              .appendHtmlConstant("</div>");
                            }
////    						sb.appendHtmlConstant("<br />");
    					}
                        @Override
                        public void onBrowserEvent(Cell.Context context, Element parent, 
                                                   String value, NativeEvent event, 
                                                   ValueUpdater<String> valueUpdater) {
                            GWT.log("browser event value: " + value);
                            int clickX = event.getClientX();
                            int clickY = event.getClientY();
                            int pageWidth = Window.getClientWidth();
//                            GWT.log("eX: " + clickX + ", eY: " + clickY + ", page: " + pageWidth);
                            int offsetX = 0;
                            int offsetY = 0;
                            int fudgeX = 10;
                            int fudgeY = parent.getOffsetParent().getOffsetHeight();
                            Element selector = (Element)parent.getChild(0);
                            ScrollPanel dgScroll = DataColumnSpecsPage.getDataGrid().getScrollPanel();
                            int scroll = dgScroll.getHorizontalScrollPosition();
                            GWT.log("scroll: " + scroll);
//                            GWT.log("selector scrollLeft: " + selector.getScrollLeft() +
//                                    ", scrollWidth: " + selector.getScrollWidth() +
//                                    ", dataGrid: scrollLeft: " + DataColumnSpecsPage.getDataGrid().getElement().getScrollLeft() +
//                                    ", dataGrid: scrollWidth: " + DataColumnSpecsPage.getDataGrid().getElement().getScrollWidth()
//                            );
                            fudgeY -= selector.getOffsetHeight();
                            int selectorWidth = selector.getOffsetWidth();
                            int iconWidth = 20; // XXX
                            Element e = parent;
                            for (int i = 0; i < 8; i++ ) {
                                offsetX += e.getOffsetLeft();
                                offsetY += e.getOffsetTop();
//                                GWT.log("e: " + e + ", offX: " + offsetX + ", offY: " + offsetY);
                                e = e.getOffsetParent(); 
                            }
                            UploadDashboard.logToConsole("eX: " + clickX + ", eY: " + clickY + ", page: " + pageWidth+ ", selector: " + selectorWidth );
//                            UploadDashboard.logToConsole("fudgeX: " + fudgeX + ", fudgeY: " + fudgeY);
                            int adjustedX = offsetX - scroll;
                            UploadDashboard.logToConsole("offX: " + offsetX + ", offY: " + offsetY + ", adjustedX: " + adjustedX);
                            if ( clickX < adjustedX + selectorWidth - iconWidth ) { // ignore
                                GWT.log("ignoring click!");
                                return; 
                            }
                            int popupWidth = DataTypeSelectorWidget.WIDTH; // XXX FeedbackPopupWidth CHANGE_ME!
                            int showX = adjustedX + fudgeX;
//                            int showX = clickX - selectorWidth;
                            if ( showX < 25 ) {
                                GWT.log("adjusting showX up.");
                                showX = 25;
                            }
                            int showY = offsetY + fudgeY;
                            
                            if ( showX + popupWidth > pageWidth - 75 ) {
                                showX = pageWidth - (popupWidth + 75);
                                GWT.log("adjusting showX to: "+ showX);
                            }
                            GWT.log("showX:"+showX + ", showY: " + showY);
                            dts.show(cruise.getDataColTypes().get(columnIndex), valueUpdater, showX, showY);
//                            int shownWidth = dts.getPopupPanel().getOffsetWidth();
//                            UploadDashboard.logToConsole("shownWidth:"+shownWidth);
//                            super.onBrowserEvent(context, parent, value, event, valueUpdater);
                        }
    				};
                }
//                selectorCellWidget = new CellWidget<String>(theCell);
                return theCell;
			}
			@Override
			public FieldUpdater<DatasetDataColumn,String> getFieldUpdater() {
				return new FieldUpdater<DatasetDataColumn,String>() {
					@Override
					public void update(int index, DatasetDataColumn dataCol, String value) {
						// Note: index is the row index of the cell in a table 
						// column where it is normally used; not of use here.

//                        Window.alert("You clicked " + value);
                        GWT.log("update value " + value + " for idx: " + index + " col: " + dataCol + " widget "+ theCell); // selectorCellWidget);
                        String id = "dcn_"+columnNumber;
                        GWT.log("Looking for " + id);
                        Element nameCell = Document.get().getElementById(id);
                        GWT.log("nameCell:"+nameCell);
                        nameCell.setInnerHTML(value);
//                        selectorCellWidget.getCell().setValue(arg0, arg1, arg2);
//                        DataColumnSpecsPage.showDataTypeSelector(selectorCellWidget);
//                        if ( true )
//                            return;
						// Ignore this callback if value is null
//						if ( value == null )
//							return;

//						// Find the data type and units corresponding to header
//						int idx = typeUnitStringList.indexOf(value);
//						// Ignore this callback if value is not found - should not happen
//						if ( idx < 0 )
//							return;
//						DataColumnType newType = knownTypeUnitList.get(idx).duplicate();
//
//						// Assign the data type and units
//						ArrayList<DataColumnType> cruiseColTypes = dataCol.cruise.getDataColTypes();
//						DataColumnType oldType = cruiseColTypes.get(dataCol.columnIndex);
//						newType.setSelectedMissingValue(oldType.getSelectedMissingValue());
//						if ( newType.equals(oldType) )
//							return;
//						hasChanged = true;
//						cruiseColTypes.set(dataCol.columnIndex, newType);
    				}
				};
			}
			@Override
			public String getValue(DatasetDataColumn dataCol) {
//                GWT.log("getValue: " + dataCol.columnIndex);
				// Find this column type with units
				DataColumnType dctype = dataCol.cruise.getDataColTypes().get(dataCol.columnIndex);
				// Ignore the missing value for this comparison
				String missVal = dctype.getSelectedMissingValue();
				dctype.setSelectedMissingValue(null);
				int idx = knownTypeUnitList.indexOf(dctype);
				dctype.setSelectedMissingValue(missVal);
				if ( idx < 0 ) {
					// Not a recognized column type with units; set to unknown
					idx = knownTypeUnitList.indexOf(DashboardUtils.UNKNOWN);
					if ( idx < 0 )
						throw new RuntimeException("Unexpected failure to find the UNASSIGNED data column");
				}
				// Return the header for this column type with units
				return typeUnitStringList.get(idx);
			}
		};
//        UploadDashboard.logToConsole("stdCell:"+stdNameCell.getCell());
//        selectorCellWidget = new CellWidget<String>(stdNameCell.getCell());
//        selectorCellWidget.addHandler(new ClickHandler() {
//            @Override
//            public void onClick(ClickEvent event) {
//                UploadDashboard.logToConsole(event.toString());
//            }
//        }, new GwtEvent.Type<ClickHandler>());
//        UploadDashboard.logToConsole("selectorWidget:"+selectorCellWidget);

		// Create the TextInputCell allowing the user to specify the missing value
		HasCell<DatasetDataColumn,String> missValCell = new HasCell<DatasetDataColumn,String>() {
			@Override
			public TextInputCell getCell() {
				return new TextInputCell();
				// TODO: capture start-edit events to erase DEFAULT_MISSING_VALUE
			}
			@Override
			public FieldUpdater<DatasetDataColumn,String> getFieldUpdater() {
				return new FieldUpdater<DatasetDataColumn,String>() {
					@Override
					public void update(int index, DatasetDataColumn dataCol, String value) {
						if ( value == null ) {
							// ignore this callback if the value is null
							return;
						}
						// Set this missing value
						value = value.trim();
						if ( value.equals(DEFAULT_MISSING_VALUE) )
							value = "";
						DataColumnType dctype = dataCol.cruise.getDataColTypes().get(dataCol.columnIndex);
						String oldValue = dctype.getSelectedMissingValue();
						if ( value.equals(oldValue) )
							return;
						dctype.setSelectedMissingValue(value);
						hasChanged = true;
					}
				};
			}
			@Override
			public String getValue(DatasetDataColumn dataCol) {
				DataColumnType dctype = dataCol.cruise.getDataColTypes().get(dataCol.columnIndex);
				String value = dctype.getSelectedMissingValue();
				if ( (value == null) || value.isEmpty() )
					value = DEFAULT_MISSING_VALUE;
				return value;
			}
		};
		// Create the CompositeCell to be used for the header				
		CompositeCell<DatasetDataColumn> compCell = 
			new CompositeCell<DatasetDataColumn>(
				new ArrayList<HasCell<DatasetDataColumn,?>>(
					Arrays.asList(userNameCell, stdNameCell, missValCell)));

		// Create and return the Header
		Header<DatasetDataColumn> headerCell = new Header<DatasetDataColumn>(compCell) {
			@Override
			public DatasetDataColumn getValue() {
				return DatasetDataColumn.this;
			}
		};
//        CompositeCell<DatasetDataColumn> headerComp = (CompositeCell<DatasetDataColumn>)headerCell.getCell();
//        List<HasCell<DatasetDataColumn,?>> compCells = headerComp.getHasCells();
//        HasCell<DatasetDataColumn,String> compNameHasCell = (HasCell<DatasetDataColumn, String>) compCells.get(1);
//        StyledSuggestBoxCell compNameCell = (StyledSuggestBoxCell)compNameHasCell.getCell();
//        compNameCell.addStyle(columnStyle);
		return headerCell;
	}
}
