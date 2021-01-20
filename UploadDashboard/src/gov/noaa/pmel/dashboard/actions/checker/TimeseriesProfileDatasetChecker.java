/**
 * 
 */
package gov.noaa.pmel.dashboard.actions.checker;

import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.CheckerMessageHandler;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;

/**
 * @author kamb
 *
 */
public class TimeseriesProfileDatasetChecker extends ProfileDatasetChecker {

    /**
     * @param userDataTypes
     * @param checkerMessageHandler
     * @throws IllegalArgumentException
     */
    public TimeseriesProfileDatasetChecker(KnownDataTypes userDataTypes, 
                                           CheckerMessageHandler checkerMessageHandler)
            throws IllegalArgumentException {
        super(userDataTypes, checkerMessageHandler);
    }

    @Override
	public StdUserDataArray standardizeDataset(DashboardDatasetData dataset,
                                    		   DsgMetadata metadata) throws IllegalArgumentException {
        return super.standardizeDataset(dataset, metadata);
    }
    
}
