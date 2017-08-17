/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

import com.google.gwt.cell.client.Cell;
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
	private int colNum;
	private DashboardDataset cruise;
	private Map<Integer, ADCMessage> rowMsgMap;
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
	}
	protected String getMessagesText(Cell.Context ctx, ArrayList<String> obj) {
		StringBuilder sb = new StringBuilder();
//		TreeSet<QCFlag> checkerFlags = cruise.getCheckerFlags();
//		Integer rowIdx = ctx.getIndex();
//		QCFlag woceCell = new QCFlag(null, null, Severity.ERROR, colNum-1, rowIdx);
//		QCFlag woceRow = new QCFlag(null, null, Severity.ERROR, null, rowIdx);
//		QCFlag woceCol = new QCFlag(null, null, Severity.ERROR, colNum-1, null);
//		if ( checkerFlags.contains(woceCell) || 
//			 checkerFlags.contains(woceRow) || 
//			 checkerFlags.contains(woceCol) ) {
//		sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.CHECKER_ERROR_COLOR + ";font-weight:bold;\">");
//			sb.appendEscaped(getValue(obj));
//			sb.appendHtmlConstant("</div>");
//			return;
//		}
//		TreeSet<QCFlag> userFlags = cruise.getUserFlags();
//		if ( userFlags.contains(woceCell) || 
//			 userFlags.contains(woceRow) || 
//			 userFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.USER_ERROR_COLOR + ";\">");
//			sb.appendEscaped(getValue(obj));
//			sb.appendHtmlConstant("</div>");
//			return;
//		}
//		woceCell.setSeverity(Severity.WARNING);
//		woceRow.setSeverity(Severity.WARNING);
//		woceCol.setSeverity(Severity.WARNING);
//		if ( checkerFlags.contains(woceCell) || 
//			 checkerFlags.contains(woceRow) || 
//			 checkerFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.CHECKER_WARNING_COLOR + ";font-weight:bold;\">");
//			sb.appendEscaped(getValue(obj));
//			sb.appendHtmlConstant("</div>");
//			return;
//		}
//		if ( userFlags.contains(woceCell) || 
//			 userFlags.contains(woceRow) || 
//			 userFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.USER_WARNING_COLOR + ";\">");
//			sb.appendEscaped(getValue(obj));
//			sb.appendHtmlConstant("</div>");
//			return;
//		}
		return sb.toString();
	}
	
	@Override
	public String getValue(ArrayList<String> dataRow) {
		if ( (dataRow != null) && (0 <= colNum) && (colNum < dataRow.size()) )
			return dataRow.get(colNum);
		return "";
	}
	private void closeTitle(SafeHtmlBuilder sb) {
		sb.appendHtmlConstant("</div>");
	}
	@Override
	public void render(Cell.Context ctx, ArrayList<String> obj, SafeHtmlBuilder sb) {
		Integer rowIdx = ctx.getIndex();
		ADCMessage msg = rowMsgMap.get(rowIdx);
		boolean addedTitle = false;
		if ( msg != null ) {
			sb.appendHtmlConstant("<div title=\"" + msg.getDetailedComment() + "\">");
			addedTitle = true;
		}
		if ( colNum == 0 ) {
			sb.appendHtmlConstant("<div style=\"color:" + 
					UploadDashboard.ROW_NUMBER_COLOR + ";text-align:right\">");
			sb.appendEscaped(getValue(obj));
			sb.appendHtmlConstant("</div>");
			if ( addedTitle ) closeTitle(sb);
			return;
		}
		TreeSet<QCFlag> checkerFlags = cruise.getCheckerFlags();
		QCFlag woceCell = new QCFlag(null, null, Severity.ERROR, colNum-1, rowIdx);
		QCFlag woceRow = new QCFlag(null, null, Severity.ERROR, null, rowIdx);
		QCFlag woceCol = new QCFlag(null, null, Severity.ERROR, colNum-1, null);
		if ( checkerFlags.contains(woceCell) || 
//			 checkerFlags.contains(woceRow) || 
			 checkerFlags.contains(woceCol) ) {
			sb.appendHtmlConstant("<div style=\"background-color:" + 
					UploadDashboard.CHECKER_ERROR_COLOR + ";font-weight:bold;\">");
			sb.appendEscaped(getValue(obj));
			sb.appendHtmlConstant("</div>");
			if ( addedTitle ) closeTitle(sb);
			return;
		}
		TreeSet<QCFlag> userFlags = cruise.getUserFlags();
		if ( userFlags.contains(woceCell) || 
//			 userFlags.contains(woceRow) || 
			 userFlags.contains(woceCol) ) {
			sb.appendHtmlConstant("<div style=\"background-color:" + 
					UploadDashboard.USER_ERROR_COLOR + ";\">");
			sb.appendEscaped(getValue(obj));
			sb.appendHtmlConstant("</div>");
			if ( addedTitle ) closeTitle(sb);
			return;
		}
		woceCell.setSeverity(Severity.WARNING);
		woceRow.setSeverity(Severity.WARNING);
		woceCol.setSeverity(Severity.WARNING);
		if ( checkerFlags.contains(woceCell) || 
//			 checkerFlags.contains(woceRow) || 
			 checkerFlags.contains(woceCol) ) {
			sb.appendHtmlConstant("<div style=\"background-color:" + 
					UploadDashboard.CHECKER_WARNING_COLOR + ";font-weight:bold;\">");
			sb.appendEscaped(getValue(obj));
			sb.appendHtmlConstant("</div>");
			if ( addedTitle ) closeTitle(sb);
			return;
		}
		if ( userFlags.contains(woceCell) || 
//			 userFlags.contains(woceRow) || 
			 userFlags.contains(woceCol) ) {
			sb.appendHtmlConstant("<div style=\"background-color:" + 
					UploadDashboard.USER_WARNING_COLOR + ";\">");
			sb.appendEscaped(getValue(obj));
			sb.appendHtmlConstant("</div>");
			if ( addedTitle ) closeTitle(sb);
			return;
		}
		// Render normally
		super.render(ctx, obj, sb);
		// Shouldn't happen, but ...
		if ( addedTitle ) closeTitle(sb);
	}
//		if ( colNum == 0 ) {
//			sb.appendHtmlConstant("<div style=\"color:" + 
//					UploadDashboard.ROW_NUMBER_COLOR + ";text-align:right\">");
//			sb.appendEscaped(getValue(obj));
//			sb.appendHtmlConstant("</div>");
//			return;
//		}
//		TreeSet<QCFlag> checkerFlags = cruise.getCheckerFlags();
//		Integer rowIdx = ctx.getIndex();
//		QCFlag woceCell = new QCFlag(null, null, Severity.ERROR, colNum-1, rowIdx);
//		QCFlag woceRow = new QCFlag(null, null, Severity.ERROR, null, rowIdx);
//		QCFlag woceCol = new QCFlag(null, null, Severity.ERROR, colNum-1, null);
//		if ( checkerFlags.contains(woceCell) || 
//			 checkerFlags.contains(woceRow) || 
//			 checkerFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.CHECKER_ERROR_COLOR + ";font-weight:bold;\">");
//			sb.appendEscaped(getValue(obj));
//			sb.appendHtmlConstant("</div>");
//			return;
//		}
//		TreeSet<QCFlag> userFlags = cruise.getUserFlags();
//		if ( userFlags.contains(woceCell) || 
//			 userFlags.contains(woceRow) || 
//			 userFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.USER_ERROR_COLOR + ";\">");
//			sb.appendEscaped(getValue(obj));
//			sb.appendHtmlConstant("</div>");
//			return;
//		}
//		woceCell.setSeverity(Severity.WARNING);
//		woceRow.setSeverity(Severity.WARNING);
//		woceCol.setSeverity(Severity.WARNING);
//		if ( checkerFlags.contains(woceCell) || 
//			 checkerFlags.contains(woceRow) || 
//			 checkerFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.CHECKER_WARNING_COLOR + ";font-weight:bold;\">");
//			sb.appendEscaped(getValue(obj));
//			sb.appendHtmlConstant("</div>");
//			return;
//		}
//		if ( userFlags.contains(woceCell) || 
//			 userFlags.contains(woceRow) || 
//			 userFlags.contains(woceCol) ) {
//			sb.appendHtmlConstant("<div style=\"background-color:" + 
//					UploadDashboard.USER_WARNING_COLOR + ";\">");
//			sb.appendEscaped(getValue(obj));
//			sb.appendHtmlConstant("</div>");
//			return;
//		}
//		// Render normally
//		super.render(ctx, obj, sb);
}