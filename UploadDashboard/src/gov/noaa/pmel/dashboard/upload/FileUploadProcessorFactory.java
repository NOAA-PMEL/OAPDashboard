/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

/**
 * @author kamb
 *
 */
public class FileUploadProcessorFactory {

    public static FileUploadProcessorFactory getFactory() {
        return new FileUploadProcessorFactory();
    }
    public FileUploadProcessor getProcessor(StandardUploadFields stdFields) {
        FileUploadProcessor processor = null;
        switch (stdFields.featureType()) {
            case UNSPECIFIED:
                processor = new FileTypeGuesserUploadProcessor(stdFields);
                break;
            case TIMESERIES:
                processor = new TimeseriesUploadProcessor(stdFields);
                break;
            case TRAJECTORY:
                processor = new TrajectoryUploadProcessor(stdFields);
                break;
            case PROFILE:
                processor = new ProfileUploadProcessor(stdFields);
                break;
            case PROFILE_TIMESERIES:
                processor = new ProfileTimeseriesUploadProcessor(stdFields);
                break;
            case TRAJECTORY_PROFILE:
                processor = new TrajectoryProfileUploadProcessor(stdFields);
                break;
            case OTHER:
                processor = new OpaqueFileUploadProcessor(stdFields);
                break;
            default:
                throw new IllegalArgumentException("Unknown Feature Type: " + stdFields.featureType());
        }
        return processor;
    }
    
}
