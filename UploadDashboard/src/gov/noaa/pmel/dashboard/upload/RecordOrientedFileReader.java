/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author kamb
 *
 */
public interface RecordOrientedFileReader extends Iterable<String[]> {

    static RecordOrientedFileReader getFileReader(String fileType, InputStream inputStream) throws IOException {
        if ( fileType.contains("excel") 
                || fileType.contains("spreadsheet")
                || fileType.contains("ooxml")) {
               return ExcelFileReader.newInstance(inputStream);
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