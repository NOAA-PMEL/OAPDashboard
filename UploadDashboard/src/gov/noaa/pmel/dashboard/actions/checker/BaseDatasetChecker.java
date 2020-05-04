/**
 * 
 */
package gov.noaa.pmel.dashboard.actions.checker;

import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.CheckerMessageHandler;
import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;

/**
 * @author kamb
 *
 */
public class BaseDatasetChecker {

    protected CheckerMessageHandler msgHandler;
    protected KnownDataTypes knownUserDataTypes;

	class RowColumn {
		int row;
		int column;

		public RowColumn(Integer rowIndex, Integer columnIndex) {
			if ( rowIndex == null )
				row = DashboardUtils.INT_MISSING_VALUE.intValue();
			else
				row = rowIndex.intValue();
			if ( columnIndex == null )
				column = DashboardUtils.INT_MISSING_VALUE.intValue();
			else
				column = columnIndex.intValue();
		}

		@Override
		public int hashCode() {
			return 37 * row + column;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj )
				return true;
			if ( obj == null )
				return false;
			if ( ! ( obj instanceof RowColumn ) )
				return false;
			RowColumn other = (RowColumn) obj;
			if ( row != other.row )
				return false;
			if ( column != other.column )
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "RowColumn[row=" + row + ", column=" + column + "]";
		}
	}

    /**
         * @param dataset
         */
    protected static void checkForUnknownColumns(StdUserDataArray stdUserData) {
        int colIdx = 0;
        String[] userNames = stdUserData.getUserColumnNames();
        for (DashDataType<?> dtype : stdUserData.getDataTypes()) {
            if ( dtype.typeNameEquals("unknown")) {
                ADCMessage msg = new ADCMessage();
                msg.setSeverity(Severity.WARNING);
                msg.setGeneralComment("unknown column");
                msg.setDetailedComment("Data column " + userNames[colIdx] + "[" + (colIdx+1) + "] is of unknown type.");
                msg.setColIndex(colIdx);
                stdUserData.addStandardizationMessage(msg);
            }
            colIdx += 1;
        }
    }

    /**
     * 
     */
    public BaseDatasetChecker() {
        super();
    }

}