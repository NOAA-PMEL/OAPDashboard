/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author kamb
 *
 */
public interface RecordOrientedFileReader extends Iterable<String[]>, AutoCloseable {

    static RecordOrientedFileReader getFileReader(String fileType, File uploadedFile) throws IOException {
        if ( fileType.contains("excel") 
                || fileType.contains("spreadsheet")
                || fileType.contains("ooxml")) {
               return ExcelFileReader.newInstance(uploadedFile, fileType);
        } else if ( ! ( fileType.contains("text") 
                        && ( fileType.contains("delimited")
                             || fileType.contains("separated")
                             || fileType.contains("csv")))) {
            throw new IllegalStateException("Unknown file type:"+ fileType);
        }
        return new CSVFileReader(uploadedFile, fileType);
    }
    static RecordOrientedFileReader getFileReader(String fileType, BufferedInputStream inputStream) throws IOException {
        if ( fileType.contains("excel") 
                || fileType.contains("spreadsheet")
                || fileType.contains("ooxml")) {
               return ExcelFileReader.newInstance(inputStream, fileType);
        } else if ( ! ( fileType.contains("text") 
                        && ( fileType.contains("delimited")
                             || fileType.contains("separated")
                             || fileType.contains("csv")))) {
            throw new IllegalStateException("Unknown file type:"+ fileType);
        }
        return new CSVFileReader(inputStream, fileType);
    }
    
    String getDelimiter();
    
    default String reconstitute(String[] rowData) {
        StringBuilder b = new StringBuilder(); 
        String delim = "";
        String fileDelim = getDelimiter();
        for (String cell : rowData) {
            b.append(delim).append(cell != null ? cell : "");
            delim = fileDelim;
        }
        return b.toString();
    }
    
}