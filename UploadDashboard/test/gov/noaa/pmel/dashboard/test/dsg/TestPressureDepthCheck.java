/**
 * 
 */
package gov.noaa.pmel.dashboard.test.dsg;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import gov.noaa.pmel.dashboard.actions.checker.ProfileDatasetChecker;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

/**
 * @author kamb
 *
 */
public class TestPressureDepthCheck {

    static final KnownDataTypes KNOWN_USER_TYPES;
    static final KnownDataTypes KNOWN_DATAFILE_TYPES;

    static {
        KNOWN_USER_TYPES = new KnownDataTypes();
        KNOWN_USER_TYPES.addStandardTypesForUsers();
        KNOWN_DATAFILE_TYPES = new KnownDataTypes();
        KNOWN_DATAFILE_TYPES.addStandardTypesForDataFiles();
    }

    /**
     * 
     */
    public TestPressureDepthCheck() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String filename = "NCRMPCarbChem_2019_final.csv";
        String dataFileName = "/Users/kamb/workspace/oa_dashboard_test_data/people/nicoleb/" + filename;
        File dataFile = new File(dataFileName);
        try {
//            DashboardConfigStore config = DashboardConfigStore.get(false);
            KnownDataTypes knownTypes = KNOWN_DATAFILE_TYPES;
            InputStream inStream = new FileInputStream(dataFile);
            String dataFormat = DashboardUtils.COMMA_FORMAT_TAG;
            String owner = "kamb";
            String timestamp = "time";
            String submissionRecordId = "recordID";
            String specifiedDatasetId = submissionRecordId;
            String datasetIdColName = null;
            DashboardDatasetData dataset = DataFileHandler
                    .createDatasetsFromInput(inStream, dataFormat, owner, filename, timestamp, 
                                             submissionRecordId, specifiedDatasetId, datasetIdColName)
                    .get(submissionRecordId.toUpperCase());
            StdUserDataArray std = new StdUserDataArray(dataset, knownTypes);
            std.checkPressureAndDepth();
            std = new StdUserDataArray(dataset, knownTypes);
            ProfileDatasetChecker.checkForMissingPressureOrDepth(std);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }

}
