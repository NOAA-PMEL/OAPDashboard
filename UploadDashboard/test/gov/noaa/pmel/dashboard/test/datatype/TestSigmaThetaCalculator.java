/**
 * 
 */
package gov.noaa.pmel.dashboard.test.datatype;

import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;

/**
 * @author kamb
 *
 */
public class TestSigmaThetaCalculator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String datasetId = "PRISM102011"; // "HOGREEF64W32N";
			DashboardConfigStore dcfg = DashboardConfigStore.get(false);
			DataFileHandler dataFileHandler = dcfg.getDataFileHandler();
			KnownDataTypes knownDataTypes = dcfg.getKnownUserDataTypes();
			DashboardDatasetData ddd = dataFileHandler.getDatasetDataFromFiles(datasetId, 0, -1);
			StdUserDataArray stda = new StdUserDataArray(ddd, knownDataTypes);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
