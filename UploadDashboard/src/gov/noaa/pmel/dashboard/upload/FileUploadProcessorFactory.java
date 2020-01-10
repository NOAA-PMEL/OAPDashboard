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
    public static FileUploadProcessor getProcessor(StandardUploadFields stdFields) {
        FileUploadProcessor processor = null;
        switch (stdFields.fileType()) {
            case DELIMITED:
                switch (stdFields.featureType()) {
                    case UNSPECIFIED:
                        processor = new FeatureTypeGuesserUploadProcessor(stdFields);
                        break;
                    case TIMESERIES:
                    case TRAJECTORY:
                    case PROFILE:
                    case TIMESERIES_PROFILE:
                    case TRAJECTORY_PROFILE:
                    case OTHER:
                        processor =  new StandardUploadProcessor(stdFields);
                        break;
//                    case OTHER:
//                        processor = new OpaqueFileUploadProcessor(stdFields);
//                        break;
                    default:
                        throw new IllegalArgumentException("Unknown Feature Type: " + stdFields.featureType());
                }
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
