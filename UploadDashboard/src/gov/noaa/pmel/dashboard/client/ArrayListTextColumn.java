/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.TextColumn;

import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.QCFlag;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;

/**
 * TextColumn for displaying the value at a given index 
 * of an ArrayList of Strings 
 */
 public class ArrayListTextColumn extends TextColumn<ArrayList<String>> {
    private static final String EMPTY_CELL = "---";
	private int colNum;
	private DashboardDataset cruise;
	private Map<Integer, ADCMessage> rowMsgMap;
	private Map<Integer, Severity> checkerFlagMap;
	/**
	 * Creates a TextColumn for an ArrayList of Strings that 
	 * displays the value at the given index in the ArrayList.
	 * @param colNum
	 * 		display data at this index of the ArrayList
	 */
	ArrayListTextColumn(int colNum, final DashboardDataset cruise, final Map<Integer, ADCMessage> msgMap) {
		super();
		this.colNum = colNum;
		this.cruise = cruise;
		this.rowMsgMap = msgMap;
        buildCellFlagMap(cruise);
	}
	/**
     * @param cruise2
     * @return
     */
    private void buildCellFlagMap(DashboardDataset theCruise) {
        checkerFlagMap = new HashMap<Integer, Severity>();
		TreeSet<QCFlag> checkerFlags = cruise.getCheckerFlags();
        for (QCFlag flag : checkerFlags) {
            Integer colIdx = flag.getColumnIndex();
            if ( colIdx != null && 
                 colIdx.intValue() == colNum-1 ) {
                Integer rowIdx = flag.getRowIndex();
                if ( rowIdx == null ) { continue; }
                if ( checkerFlagMap.containsKey(rowIdx)) {
                    Severity prior = checkerFlagMap.get(rowIdx);
                    if ( prior.compareTo(flag.getSeverity()) > 0) {
                        // don't downgrade row severity
                        continue;
                    }
                }
                checkerFlagMap.put(flag.getRowIndex(), flag.getSeverity());
            }
        }
    }
	@Override
	public String getValue(ArrayList<String> dataRow) {
		if ( (dataRow != null) && (0 <= colNum) && (colNum < dataRow.size()) ) {
            String cellValue = dataRow.get(colNum);
            if ( cellValue == null || "".equals(cellValue)) {
                cellValue = EMPTY_CELL;
            }
			return cellValue;
		}
		return "";
	}
	private static void closeTitle(SafeHtmlBuilder sb) {
		sb.appendHtmlConstant("</div>");
	}
	@Override
	public void render(Cell.Context ctx, ArrayList<String> obj, SafeHtmlBuilder sb) {
		Integer rowIdx = new Integer(ctx.getIndex());
        int rowNum = ctx.getIndex()+1;
        String cellValue = getValue(obj);
        String hideContents = EMPTY_CELL.equals(cellValue) ? " color: transparent; " : "";
		ADCMessage msg = rowMsgMap.get(rowIdx);
		boolean addedTitle = false;
		String title = "[ " + (rowNum) +" ]";
        Severity cellSeverity = Severity.ACCEPTABLE;
		if ( msg != null ) {
			title += " " + msg.getDetailedComment();
            Integer msgCol = msg.getColNumber();
            if ( msgCol != null && msgCol.intValue() == colNum ) {
                cellSeverity = msg.getSeverity();
            }
		}
		sb.appendHtmlConstant("<div title=\"" + title + "\">");
		addedTitle = true;
		if ( colNum == 0 ) {
			sb.appendHtmlConstant("<div style=\"color:" + 
					UploadDashboard.ROW_NUMBER_COLOR + ";text-align:right\">");
			sb.appendEscaped(cellValue);
			sb.appendHtmlConstant("</div>");
			if ( addedTitle ) closeTitle(sb);
			return;
		}
        if ( checkerFlagMap.containsKey(rowIdx)) {
            Severity flagSeverity = checkerFlagMap.get(rowIdx);
            if ( flagSeverity.compareTo(cellSeverity) < 0 ) {
                cellSeverity = flagSeverity;
            } else {
                GWT.log("QCFlag for cell less severe than message!");
            }
        }
        if ( cellSeverity.compareTo(Severity.ACCEPTABLE) < 0 ) {
            String highlghtColor = "";
            switch ( cellSeverity ) {
                case CRITICAL:
                    highlghtColor = UploadDashboard.CHECKER_CRITICAL_CELL_COLOR;
                    break;
                case ERROR:
                    highlghtColor = UploadDashboard.CHECKER_ERROR_CELL_COLOR;
                    break;
                case WARNING:
                    highlghtColor = UploadDashboard.CHECKER_WARNING_CELL_COLOR;
                    break;
                default:
            }
			sb.appendHtmlConstant("<div style=\"background-color:" + highlghtColor +
			                      ";font-weight:bold;" + hideContents + "\">");
            sb.appendEscaped(cellValue);
            sb.appendHtmlConstant("</div>");
    		if ( addedTitle ) closeTitle(sb);
            return;
        }
//		TreeSet<QCFlag> checkerFlags = cruise.getCheckerFlags();
//		QCFlag woceCell = new QCFlag(null, null, Severity.ERROR, colNum-1, rowIdx);
//		QCFlag woceRow = new QCFlag(null, null, Severity.ERROR, null, rowIdx);
////		QCFlag woceCol = new QCFlag(null, null, Severity.ERROR, colNum-1, null);
//		if ( checkerFlags.contains(woceCell)) { // || 
////			 checkerFlags.contains(woceRow) || 
////			 checkerFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.CHECKER_ERROR_COLOR + ";font-weight:bold;" + hideContents + "\">");
//			sb.appendEscaped(cellValue);
//			sb.appendHtmlConstant("</div>");
//			if ( addedTitle ) closeTitle(sb);
//			return;
//		}
//		TreeSet<QCFlag> userFlags = cruise.getUserFlags();
//		if ( userFlags.contains(woceCell)) { // || 
////			 userFlags.contains(woceRow) || 
////			 userFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.USER_ERROR_COLOR + ";font-weight:bold;" + hideContents + "\">");
//			sb.appendEscaped(cellValue);
//			sb.appendHtmlConstant("</div>");
//			if ( addedTitle ) closeTitle(sb);
//			return;
//		}
//		woceCell.setSeverity(Severity.WARNING);
//		woceRow.setSeverity(Severity.WARNING);
////		woceCol.setSeverity(Severity.WARNING);
//		if ( checkerFlags.contains(woceCell)) { // || 
////			 checkerFlags.contains(woceRow) || 
////			 checkerFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.CHECKER_WARNING_COLOR + ";font-weight:bold;" + hideContents + "\">");
//			sb.appendEscaped(cellValue);
//			sb.appendHtmlConstant("</div>");
//			if ( addedTitle ) closeTitle(sb);
//			return;
//		}
//		if ( userFlags.contains(woceCell)) { // || 
////			 userFlags.contains(woceRow) || 
////			 userFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.USER_WARNING_COLOR + ";font-weight:bold;" + hideContents + "\">");
//			sb.appendEscaped(cellValue);
//			sb.appendHtmlConstant("</div>");
//			if ( addedTitle ) closeTitle(sb);
//			return;
//		}
//		woceCell.setSeverity(Severity.CRITICAL);
//		woceRow.setSeverity(Severity.CRITICAL);
////		woceCol.setSeverity(Severity.CRITICAL);
//		if ( checkerFlags.contains(woceCell)) { // || 
////			 checkerFlags.contains(woceRow) || 
////			 checkerFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.CHECKER_ERROR_COLOR + ";font-weight:bold;" + hideContents + "\">");
//			sb.appendEscaped(cellValue);
//			sb.appendHtmlConstant("</div>");
//			if ( addedTitle ) closeTitle(sb);
//			return;
//		}
//		if ( userFlags.contains(woceCell)) { // || 
////			 userFlags.contains(woceRow) || 
////			 userFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.USER_ERROR_COLOR + ";font-weight:bold;" + hideContents + "\">");
//			sb.appendEscaped(cellValue);
//			sb.appendHtmlConstant("</div>");
//			if ( addedTitle ) closeTitle(sb);
//			return;
//		}
		if ( addedTitle ) closeTitle(sb);
		// Render normally
		super.render(ctx, obj, sb);
	}
}