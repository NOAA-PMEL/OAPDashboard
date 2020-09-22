/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import gov.noaa.pmel.tws.util.Logging;

/**
 * @author kamb
 *
 */
public class CSVFileReader implements RecordOrientedFileReader {

    private static Logger logger = Logging.getLogger(CSVFileReader.class);
    
    private BufferedReader dataReader;
    private String detectedFileType;
    private CsvParser dataParser;
    private CsvFormat fileFormat;
    
    public CSVFileReader(InputStream inputStream, String fileType) {
        this.dataReader = new BufferedReader(new InputStreamReader(inputStream)); 
        this.detectedFileType = fileType;
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<String[]> iterator() {
        CsvParserSettings settings = new CsvParserSettings();
        settings.detectFormatAutomatically();
        char[] detectionChars = detectedFileType.contains("tab") ? 
                                    new char[] {'\t', ',', ';', '|'} : 
                                    new char[] {',', '\t', ';', '|'}; 
        settings.setDelimiterDetectionEnabled(true, detectionChars);
        
        settings.setCommentCollectionEnabled(true);
        settings.setNullValue("");
        settings.setEmptyValue("");
        settings.setSkipEmptyLines(true); // default
        
        dataParser = new CsvParser(settings);
        dataParser.beginParsing(dataReader);
        fileFormat = dataParser.getDetectedFormat();
        logger.info("Detected file format: " + fileFormat);
        return dataParser.iterate(dataReader).iterator();
    }
    
    @Override
    public String getDelimiter() {
        return fileFormat.getDelimiterString();
    }
    
}
