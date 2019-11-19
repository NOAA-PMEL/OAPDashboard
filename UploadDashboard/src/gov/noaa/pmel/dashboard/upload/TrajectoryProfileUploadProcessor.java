/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import gov.noaa.pmel.dashboard.shared.FeatureType;

/**
 * @author kamb
 *
 */
public class TrajectoryProfileUploadProcessor extends BasicFileUploadProcessor {

    public TrajectoryProfileUploadProcessor(StandardUploadFields uploadFields) {
        super(FeatureType.TRAJECTORY_PROFILE, uploadFields);
    }

//    @Override
//    public void doFeatureSpecificProcessing(List<FileItem> datafiles) {
//        throw new NotImplementedException("Trajectory-Profile Observations are not yet implemented.");
//    }

}
