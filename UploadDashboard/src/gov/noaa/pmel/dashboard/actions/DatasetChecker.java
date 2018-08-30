/**
 * 
 */
package gov.noaa.pmel.dashboard.actions;

import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;

/**
 * @author kamb
 *
 */
public interface DatasetChecker {
    
    public StdUserDataArray standardizeDataset(DashboardDatasetData dataset);
    public StdUserDataArray standardizeDataset(DashboardDatasetData dataset, DsgMetadata metadata);
}
