/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import gov.noaa.pmel.tws.util.StringUtils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;

/**
 * @author kamb
 *
 */
public class ExcelFileReader {
    

//    private static List<String[]> extractFileRows(InputStream inStream) throws Exception, IOException {
//        List<String[]> rows;
//        ByteArrayOutputStream copyOut = new ByteArrayOutputStream();
//        IOUtils.copy(inStream, copyOut);
//        InputStream inCopy = new ByteArrayInputStream(copyOut.toByteArray());
//        try ( InputStream bufIn = FileMagic.prepareToCheckMagic(inCopy); ) {
//              FileMagic fm = FileMagic.valueOf(bufIn);
//            switch (fm) {
//                case OLE2:
//                case OOXML:
//                    rows = extractExcelRows(bufIn);
//                    break;
//                case UNKNOWN:
//                    rows = tryDelimited(inCopy);
//                    break;
//                default:
//                    throw new IOException("Cannot parse input stream of type: "+fm);
//            }
//        }
//        return rows;
//    }
        
    public static List<String[]> extractExcelRows(InputStream inStream) throws Exception, IOException {
        List<String[]> rows = new ArrayList<>();
        DataFormatter df = new DataFormatter();
        try ( Workbook wb = WorkbookFactory.create(inStream); ) {
            Sheet sheet = wb.getSheetAt(0);
            int rowNum = 0;
            for (Row row : sheet) {
                rowNum += 1;
                int rowCellCount = row.getLastCellNum();
                String[] rowValues = new String[rowCellCount];
                int cellIdx = 0;
                for ( Cell c : row ) {
                    String cellValue = df.formatCellValue(c);
                    rowValues[cellIdx++] = cellValue;
                }
                rows.add(rowValues);
            }
        }
        return rows;
    }
    
    /*
    private static List<String[]> tryDelimited(InputStream inStream) throws Exception, IOException {
        List<String[]> rows = new ArrayList<>();
//        byte[] peak = IOUtils.peekFirstNBytes(inStream, 512);
            byte[] bytes = IOUtils.peekFirstNBytes(inStream, 4096);
            String peak = new String(bytes, Charset.forName("UTF8"));
            char spacer = lookForSpacer(peak);
            CSVFormat format = CSVFormat.EXCEL.withIgnoreSurroundingSpaces()
                    .withIgnoreEmptyLines()
                    .withQuote('"')
//                        .withTrailingDelimiter()
//                        .withCommentMarker('#')
                    .withDelimiter(spacer);
    		try ( InputStreamReader isr = new InputStreamReader(inStream);
    		        CSVParser dataParser = new CSVParser(isr, format); ) {
                int rowNum = 0;
                for (CSVRecord record : dataParser) {
                    rowNum += 1;
                    int itemNo;
                    String numCell = record.get(0);
                    try {
                        itemNo = Integer.parseInt(numCell);
                    } catch (Exception ex) {
                        System.err.println("Not a valid metadata row at " + rowNum + " with cell 0 value: " + numCell);
                        continue;
                    }
                    String rowName = record.get(1);
                    if ( rowName == null) {
                        System.err.println("Null name cell at row: "+ rowNum + "[#"+itemNo+"]");
                        continue;
                    }
                    String vcell = record.get(2);
                    String rowValue = vcell;
                    OadsRow orow = new OadsRow(itemNo,
                                               rowName,
                                               rowValue);
//                    System.out.println(rowNum + ": " + orow);
                    rows.add(orow);
                }
    		} catch (Exception ex) {
    		    ex.printStackTrace();
                throw ex;
    		}
        return rows;
    }
    /**
     * @param peak
     * @return
     * /
    private static char lookForSpacer(String peak) {
        SortedMap<Integer, Character> sort = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.intValue() - o1.intValue();
            }
        });
        sort.put(count(peak, ','), new Character(','));
        sort.put(count(peak, ';'), new Character(';'));
        sort.put(count(peak, '\t'), new Character('\t'));
        sort.put(count(peak, '|'), new Character('|'));
        return sort.values().iterator().next().charValue();
    }

    /**
     * @param peak
     * @param c
     * @return
     * /
    private static Integer count(String peak, char c) {
        int count = 0;
        for (int i = 0; i < peak.length(); i++) {
            if ( peak.charAt(i) == c) {
                count += 1;
            }
        }
        return new Integer(count);
    }
    
    private static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/");
        String dateStr = sdf.format(date);
        return dateStr;
    }
    */
}
