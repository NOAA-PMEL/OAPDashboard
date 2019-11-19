/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import gov.noaa.pmel.dashboard.shared.FeatureType;

/**
 * @author kamb
 *
 */
public class ProfileTimeseriesUploadProcessor extends BasicFileUploadProcessor {

    public ProfileTimeseriesUploadProcessor(StandardUploadFields uploadFields) {
        super(FeatureType.PROFILE_TIMESERIES, uploadFields);
    }

//    @Override
//    public void doFeatureSpecificProcessing(List<FileItem> datafiles) {
//        throw new NotImplementedException("Profile-Timeseries Observations are not yet implemented.");
//    }

}
