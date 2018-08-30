/**
 * 
 */
package gov.noaa.pmel.dashboard.actions.checker;

import gov.noaa.pmel.dashboard.actions.DatasetChecker;
import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;

/**
 * @author kamb
 *
 */
public class OpaqueDatasetChecker implements DatasetChecker {

    /**
     * 
     */
    public OpaqueDatasetChecker() {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.actions.DatasetChecker#standardizeDataset(gov.noaa.pmel.dashboard.shared.DashboardDatasetData)
     */
    @Override
    public StdUserDataArray standardizeDataset(DashboardDatasetData dataset) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.actions.DatasetChecker#standardizeDataset(gov.noaa.pmel.dashboard.shared.DashboardDatasetData, gov.noaa.pmel.dashboard.dsg.DsgMetadata)
     */
    @Override
    public StdUserDataArray standardizeDataset(DashboardDatasetData dataset, DsgMetadata metadata) {
        // TODO Auto-generated method stub
        return null;
    }

}
