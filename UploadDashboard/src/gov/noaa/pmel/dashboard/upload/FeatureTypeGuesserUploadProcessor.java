/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;


import org.apache.commons.lang3.NotImplementedException;

/**
 * @author kamb
 *
 */
public class FeatureTypeGuesserUploadProcessor extends FileUploadProcessor {

    public FeatureTypeGuesserUploadProcessor(StandardUploadFields uploadFields) {
        super(uploadFields);
    }

    @Override
    public void processUploadedFile(boolean isUpdateRequest) {
        throw new NotImplementedException("Observation type guessing is not yet implemented.");
    }

}
