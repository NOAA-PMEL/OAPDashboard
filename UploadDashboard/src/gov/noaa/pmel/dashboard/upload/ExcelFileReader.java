/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * @author kamb
 *
 */
public class ExcelFileReader implements RecordOrientedFileReader, Iterator<String[]> {
    

    private InputStream _inStream;
    private DataFormatter df;
    private Workbook wb;
    private Sheet sheet;
    private Iterator<Row> rows;
    private int rowIdx = 0;
    
    public ExcelFileReader(InputStream inStream) throws IOException {
        this._inStream = inStream.markSupported() ? inStream : new BufferedInputStream(inStream);
        df = new DataFormatter();
        wb = WorkbookFactory.create(_inStream);
    }
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
        
    /**
     * Extract all the rows at once into memory.
     * 
     * @param inStream
     * @return
     * @throws Exception
     * @throws IOException
     * 
     * @deprecated Prefer iterator;
     */
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

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<String[]> iterator() {
        sheet = wb.getSheetAt(0);
        rows = sheet.rowIterator();
        return this;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        if ( ! rows.hasNext()) {
            try { wb.close(); } 
            catch (IOException iox) {} // ignore
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    @Override
    public String[] next() {
        // TODO Auto-generated method stub
        Row row = rows.next();
//        int rowCellCount = row.getLastCellNum(); // this can be a lie
        
        List<String> rowList = new ArrayList<>();
        int cellIdx = 0;
        for ( Cell c : row ) {
            String cellValue = df.formatCellValue(c);
            if ( cellValue == null ) {
                System.out.println("Null at " + rowIdx + ":" + cellIdx);
            }
            rowList.add(cellValue != null ? cellValue : "");
            cellIdx+=1;
        }
        for (int i = rowList.size()-1; i>=0; i--) {
            if ( rowList.get(i).isEmpty()) {
                rowList.remove(i);
            }
        }
        rowIdx += 1;
        return rowList.toArray(new String[rowList.size()]);
    }
    
    /*
    private static List<String[]> tryDelimited(InputStream inStream) throws Exception, IOException {
    private static List<String[]> tryDelimited_usingUnivocity(InputStream inStream) throws Exception, IOException {
    private static List<String[]> tryDelimited_usingApacheCSV(InputStream inStream) throws Exception, IOException {
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
