/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import gov.noaa.pmel.dashboard.shared.FeatureType;

/**
 * @author kamb
 *
 */
public class TimeseriesUploadProcessor extends BasicFileUploadProcessor {

    public TimeseriesUploadProcessor(StandardUploadFields uploadFields) {
        super(FeatureType.TIMESERIES, uploadFields);
    }

//    @Override
//    public void doFeatureSpecificProcessing(List<FileItem> datafiles) {
//        throw new NotImplementedException("Timeseries Observations are not yet implemented.");
//    }

}
