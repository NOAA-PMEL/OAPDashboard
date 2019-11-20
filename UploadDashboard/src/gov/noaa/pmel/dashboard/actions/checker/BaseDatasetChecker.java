/**
 * 
 */
package gov.noaa.pmel.dashboard.actions.checker;

import gov.noaa.pmel.dashboard.actions.DatasetChecker;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.CheckerMessageHandler;
import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;

/**
 * @author kamb
 *
 */
public class BaseDatasetChecker {

    protected CheckerMessageHandler msgHandler;
    protected KnownDataTypes knownUserDataTypes;

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