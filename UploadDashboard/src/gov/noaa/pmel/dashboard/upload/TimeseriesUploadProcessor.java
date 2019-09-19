/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;


/**
 * @author kamb
 *
 */
public class TimeseriesUploadProcessor extends BasicFileUploadProcessor {

    public TimeseriesUploadProcessor(StandardUploadFields uploadFields) {
        super(uploadFields);
    }

//    @Override
//    public void doFeatureSpecificProcessing(List<FileItem> datafiles) {
//        throw new NotImplementedException("Timeseries Observations are not yet implemented.");
//    }

}
