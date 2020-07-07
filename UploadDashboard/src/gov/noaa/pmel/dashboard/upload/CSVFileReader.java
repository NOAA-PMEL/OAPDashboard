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
    private static final char[] commaSeparatedExpectation = new char[] {',', '\t', ';', '|'};
    private static final char[] tabSeparatedExpectation = new char[] {'\t', ',', ';', '|'};
    
//    public static CSVFileReader readerFor(File inputFile) {
//        return new CSVFileReader(inputFile);
//    }
//    
//    private File inputFile;
//    
//    private CSVFileReader(File inputFile) {
//        this.inputFile = inputFile;
////        this.dataReader = new BufferedReader(new InputStreamReader(inputStream)); 
//    }
    
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
                                    tabSeparatedExpectation : 
                                    commaSeparatedExpectation;
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
