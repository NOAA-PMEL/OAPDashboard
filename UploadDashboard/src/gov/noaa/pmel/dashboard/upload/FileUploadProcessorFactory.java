/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

/**
 * @author kamb
 *
 */
public class FileUploadProcessorFactory {

    private StandardUploadFields _stdFields;
    
    public FileUploadProcessorFactory(StandardUploadFields stdFields) {
        _stdFields = stdFields;
    }
    
    public static FileUploadProcessorFactory getFactory(StandardUploadFields stdFields) {
        return new FileUploadProcessorFactory(stdFields);
    }
    public FileUploadProcessor getProcessor() {
        FileUploadProcessor processor = null;
        switch (_stdFields.featureType()) {
            case UNSPECIFIED:
                processor = new FileTypeGuesserUploadProcessor(_stdFields);
                break;
            case TIMESERIES:
                processor = new TimeseriesUploadProcessor(_stdFields);
                break;
            case TRAJECTORY:
                processor = new TrajectoryUploadProcessor(_stdFields);
                break;
            case PROFILE:
                processor = new ProfileUploadProcessor(_stdFields);
                break;
            case PROFILE_TIMESERIES:
                processor = new ProfileTimeseriesUploadProcessor(_stdFields);
                break;
            case TRAJECTORY_PROFILE:
                processor = new TrajectoryProfileUploadProcessor(_stdFields);
                break;
            case OTHER:
                processor = new OpaqueFileUploadProcessor(_stdFields);
                break;
            default:
                throw new IllegalArgumentException("Unknown Feature Type: " + _stdFields.featureType());
        }
        return processor;
    }
    
}
