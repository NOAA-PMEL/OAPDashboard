/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.File;
import java.io.IOException;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

/**
 * @author kamb
 *
 */
public class FileUploadProcessorFactory {

    public static FileUploadProcessorFactory getFactory() {
        return new FileUploadProcessorFactory();
    }
//    public static FileUploadProcessor getProcessor(StandardUploadFields stdFields) {
//        String fileType = stdFields.checkedFileType();
//        if ( fileIsReadable(fileType)) {
//            return new GeneralizedUploadProcessor(stdFields);
//        } else {
//            return new OpaqueFileUploadProcessor(stdFields);
//        }
//    }
//    public static FileUploadProcessor _getProcessor(StandardUploadFields stdFields) {
//        FileUploadProcessor processor = null;
//        switch (stdFields.fileType()) {
//            case DELIMITED:
//                switch (stdFields.featureType()) {
//                    case UNSPECIFIED:
//                        processor = new FeatureTypeGuesserUploadProcessor(stdFields);
//                        break;
//                    case TIMESERIES:
//                    case TRAJECTORY:
//                    case PROFILE:
//                    case TIMESERIES_PROFILE:
//                    case TRAJECTORY_PROFILE:
//                    case OTHER:
//                        processor =  new StandardUploadProcessor(stdFields);
//                        break;
////                    case OTHER:
////                        processor = new OpaqueFileUploadProcessor(stdFields);
////                        break;
//                    default:
//                        throw new IllegalArgumentException("Unknown Feature Type: " + stdFields.featureType());
//                }
//                break;
//            case OTHER:
//                processor = new OpaqueFileUploadProcessor(stdFields);
//                break;
//            default:
//                throw new IllegalArgumentException("Unknown File Type: " + stdFields.fileType());
//        }
//                
//        return processor;
//    }
    
    private static String checkFileType(File file) {
        try {
            Tika tika = new Tika();
            String type = tika.detect(file);
            return type;
        } catch (Exception ex) {
            return "tika/error";
        }
    }
    private static MediaType checkFileType(TikaInputStream tis, Metadata metadata) throws IOException {
        MediaType mt = null;
        TikaConfig tika = TikaConfig.getDefaultConfig();
        mt = tika.getDetector().detect(tis, metadata);
        return mt;
    }

    /**
     * @param itemType
     * @param fileType
     * @return
     */
    private static boolean fileIsReadable(String fileType) {
        return fileType.contains("excel")
                || fileType.contains("spreadsheet")
                || fileType.contains("ooxml")
                || ( fileType.contains("text")
                    && ( fileType.contains("delimited")
                        || fileType.contains("csv")));
                
    }

}
