/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.tomcat.util.http.fileupload.FileItem;

/**
 * @author kamb
 *
 */
public class TimeseriesUploadProcessor extends FileUploadProcessor {

    public TimeseriesUploadProcessor(StandardUploadFields uploadFields) {
        super(uploadFields);
    }

    @Override
    public void doFeatureSpecificProcessing(List<FileItem> datafiles) {
        throw new NotImplementedException("Timeseries Observations are not yet implemented.");
    }

}
