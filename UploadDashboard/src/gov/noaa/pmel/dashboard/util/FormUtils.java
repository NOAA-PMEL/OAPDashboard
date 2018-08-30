/**
 * 
 */
package gov.noaa.pmel.dashboard.util;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;

/**
 * @author kamb
 *
 */
public class FormUtils {

    private static Logger logger = LogManager.getLogger(FormUtils.class);
    
    public static String getFormField(String fieldName, Map<String,List<FileItem>> paramMap) {
        return getFormField(fieldName, paramMap, false);
    }
    
    public static String getFormField(String fieldName, Map<String,List<FileItem>> paramMap, boolean allowMultipleValues) {
        String fieldValue = null;
        List<FileItem> itemList = paramMap.get(fieldName);
        if (itemList == null || itemList.isEmpty()) {
            logger.info("No upload form fields provided for:" + fieldName);
        } else if (itemList.size() == 1) {
            fieldValue = itemList.get(0).getString();
        } else if (itemList.size() >= 1 && allowMultipleValues) {
            fieldValue = itemList.get(0).getString();
        } else {
          throw new IllegalStateException("Unexpected Multiple form field values found for: " + fieldName);
        }
        return fieldValue;
    }
    
    public static String getRequiredFormField(String fieldName, Map<String,List<FileItem>> paramMap) throws NoSuchFieldException {
        return getRequiredFormField(fieldName, paramMap, false);
    }
    
    public static String getRequiredFormField(String fieldName, Map<String,List<FileItem>> paramMap, boolean allowMultipleValues) 
            throws NoSuchFieldException {
        String fieldValue = getFormField(fieldName, paramMap, allowMultipleValues);
        if ( fieldValue == null || fieldValue.trim().length() == 0 ) {
            throw new NoSuchFieldException(fieldName);
        }
        return fieldValue;
    }

}
