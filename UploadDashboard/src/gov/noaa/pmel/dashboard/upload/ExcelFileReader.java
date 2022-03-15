/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;

import com.example.OoXmlStrictConverter;

import gov.noaa.pmel.dashboard.server.DataUploadService;
import gov.noaa.pmel.oads.util.StringUtils;

/**
 * @author kamb
 *
 */
public class ExcelFileReader implements RecordOrientedFileReader, Iterator<String[]>, AutoCloseable {
    
    static Logger logger = LogManager.getLogger(ExcelFileReader.class);
    
    private DataFormatter df;
    private FormulaEvaluator fe;
    private Workbook wb;
    private Sheet sheet;
    private Iterator<Row> rows;
    private int rowIdx = 0;
    
    private static final String OLD_EXCEL_TYPE = "application/vnd.ms-excel";
    private static final String OPEN_OFFICE_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    
    @SuppressWarnings("resource") // resource (workbook) closed by AutoCloseable
    public static ExcelFileReader newInstance(File uploadedFile, String fileType) throws IOException {
        Workbook workbook;
        boolean strict = ! OLD_EXCEL_TYPE.equals(fileType) && checkForStrict(uploadedFile);
        try (FileInputStream fis = new FileInputStream(uploadedFile); ) {
            if ( ! strict ) {
                workbook = WorkbookFactory.create(fis);
            } else {
                InputStream useStream = fis;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    OoXmlStrictConverter.Transform(useStream, baos);
                    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                    useStream = bais.markSupported() ? bais : new BufferedInputStream(bais);
                    workbook = WorkbookFactory.create(useStream);
                } catch (Exception e2) {
                    e2.printStackTrace();
                    throw new IllegalStateException("Failed to create ExcelFileReader:" + e2);
                } finally {
                    try { useStream.close(); }
                    catch (Throwable t) {
                        // ignore
                    }
                }
            }
        }
        return new ExcelFileReader(workbook);
    }
    
    @SuppressWarnings("resource") // resource (workbook) closed by AutoCloseable
    public static ExcelFileReader newInstance(BufferedInputStream inStream, String fileType) throws IOException {
        Workbook workbook;
        try {
            int available = inStream.available() - 64;
            int maxMark = Math.min(available, MAX_PEEK);
            inStream.mark(maxMark);
            boolean strict = ! OLD_EXCEL_TYPE.equals(fileType) && checkForStrict(inStream);
            inStream.reset();
            if ( ! strict ) {
                workbook = WorkbookFactory.create(inStream);
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream createStream = null;
                try {
                    OoXmlStrictConverter.Transform(inStream, baos);
                    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                    createStream = bais.markSupported() ? bais : new BufferedInputStream(bais);
                    workbook = WorkbookFactory.create(createStream);
                } catch (Exception e2) {
                    e2.printStackTrace();
                    throw new IllegalStateException("Failed to create ExcelFileReader:" + e2);
                } finally {
                    try { if ( createStream != null ) createStream.close(); }
                    catch (Throwable t) {
                        // ignore
                    }
                }
            }
        } finally {
            ;
        }
        return new ExcelFileReader(workbook);
    }
    
    static final int MAX_PEEK = DataUploadService.MAX_ALLOWED_UPLOAD_SIZE; // ~100MB
    static final String STRICT_NS_1 = "http://purl.oclc.org/ooxml/spreadsheetml/main";

    /**
     * @param inStream
     * @return
     * @throws IOException 
     * @throws XMLStreamException 
     */
    private static boolean checkForStrict(InputStream inStream) throws IOException {
        boolean isStrict = false;
        boolean stop = false;
        XMLInputFactory XIF = XMLInputFactory.newInstance();
        try {
            ZipInputStream zis = new ZipInputStream(inStream);
            ZipEntry ze;
            while( !isStrict && !stop && (ze = zis.getNextEntry()) != null) {
                try ( FilterInputStream filterIs = new FilterInputStream(zis) {
                    // because XMLEventReader will close the underlying stream when it is closed.
                    @Override
                    public void close() throws IOException {
                        // so don't close the underlying stream
                    }
                }; ) {
                    String zeName = ze.getName();
                    logger.debug("ZipEntry " + zeName + " size: " + ze.getSize());
                    if ( "xl/workbook.xml".equals(zeName)) {
                        logger.info("Processing workbook.xml, then stopping.");
                        stop = true;
                    } else {
                        continue;
                    }
                    if(isXml(ze.getName())) {
                        try {
                            XMLEventReader xer = XIF.createXMLEventReader(filterIs);
                            int pos = -1;
                            while(xer.hasNext() && pos < 0) {
                                XMLEvent xe = xer.nextEvent();
                                if ( xe.isStartElement()) {
                                    pos+=1;
                                    logger.debug(pos + ":" + xe.toString());
                                    StartElement se = xe.asStartElement();
                                    QName qn = se.getName();
                                    String ns = qn.getNamespaceURI();
                                    if ( STRICT_NS_1.equals(ns)) {
                                        logger.debug("Found strict at pos : " + pos);
                                        isStrict = true;
                                        break;
                                    }
                                }
                            }
                        } catch (XMLStreamException xsx) {
                            throw new IOException("Exception parsing document XML:"+xsx.getMessage(), xsx);
                        }
                    }
                }
            }
        } catch (ZipException ze) {
            logger.warn(ze);
        }
        logger.info("Found strict: " + isStrict);
        return isStrict;
    }
    private static boolean checkForStrict(File zipFile) throws IOException {
        boolean isStrict = false;
        boolean stop = false;
        XMLInputFactory XIF = XMLInputFactory.newInstance();
        try (ZipFile zippy = new ZipFile(zipFile); ) {
            Enumeration<? extends ZipEntry> zes = zippy.entries();
            while( !isStrict && !stop &&  zes.hasMoreElements()) {
                ZipEntry ze = zes.nextElement();
                String zeName = ze.getName();
                logger.debug("ZipEntry " + zeName + " size: " + ze.getSize());
                if ( "xl/workbook.xml".equals(zeName)) {
                    logger.info("Processing workbook.xml, then stopping.");
                    stop = true;
                } else {
                    continue;
                }
                if(isXml(ze.getName())) {
                    try {
                        XMLEventReader xer = XIF.createXMLEventReader(zippy.getInputStream(ze));
                        int pos = -1;
                        while(xer.hasNext() && pos < 0) {
                            XMLEvent xe = xer.nextEvent();
                            if ( xe.isStartElement()) {
                                pos+=1;
                                logger.debug(pos + ":" + xe.toString());
                                StartElement se = xe.asStartElement();
                                QName qn = se.getName();
                                String ns = qn.getNamespaceURI();
                                if ( STRICT_NS_1.equals(ns)) {
                                    logger.debug("Found strict at pos : " + pos);
                                    isStrict = true;
                                    break;
                                }
                            }
                        }
                    } catch (XMLStreamException xsx) {
                        throw new IOException("Exception parsing document XML:"+xsx.getMessage(), xsx);
                    }
                }
            }
        } catch (ZipException ze) {
            logger.warn(ze);
        }
        logger.info("Found strict: " + isStrict);
        return isStrict;
    }
    private static boolean isXml(final String fileName) {
        if ( ! StringUtils.emptyOrNull(fileName)) {
            int pos = fileName.lastIndexOf(".");
            if(pos != -1) {
                String ext = fileName.substring(pos + 1).toLowerCase();
                return ext.equals("xml") || ext.equals("vml") || ext.equals("rels");
            }
        }
        return false;
    }

    private ExcelFileReader(Workbook workbook) throws IOException {
        this.wb = workbook;
        df = new DataFormatter();
        fe = workbook.getCreationHelper().createFormulaEvaluator();
    }
    
    public void finalize() {
        try {
            logger.debug("EFR finalized");
            if ( wb != null )
                wb.close();
        } catch (Exception ex) {
            System.err.println("ExcelFileReader exception on finalize: " + ex);
        }
    }
    
//    public ExcelFileReader(InputStream inStream) throws IOException {
//        this._inStream = inStream.markSupported() ? inStream : new BufferedInputStream(inStream);
//        df = new DataFormatter();
//        wb = WorkbookFactory.create(_inStream);
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
            for (Row row : sheet) {
                int rowCellCount = row.getLastCellNum();
                String[] rowValues = new String[rowCellCount];
                int cellIdx = 0;
                for ( Cell cell : row ) {
                    String cellValue;
                    if ( cell.getCellType() == CellType.FORMULA ) {
                        switch (cell.getCachedFormulaResultType()) {
                            case BOOLEAN:
                                cellValue = String.valueOf(cell.getBooleanCellValue());
                                break;
                            case NUMERIC:
                                cellValue = String.valueOf(cell.getNumericCellValue());
                                break;
                            case STRING:
                                cellValue = String.valueOf(cell.getRichStringCellValue());
                                break;
                            default:
                                cellValue = df.formatCellValue(cell);
                        }                    
                    } else {
                        cellValue = df.formatCellValue(cell);
                    }
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
        // for ( Cell cell : row ) {   // cell iterator doesn't return empty cells
        int definedCellCount = row.getPhysicalNumberOfCells();
        int rowCellCount = row.getLastCellNum();
        logger.trace("Row " + row.getRowNum() + " cell count: " + rowCellCount + ", defined:" + definedCellCount);
        for ( int cellIdx = 0; cellIdx < rowCellCount; cellIdx ++ ) {
            String cellValue;
            Cell cell = row.getCell(cellIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            try { // some types of cells (Date-type in particular) throw an exception on getCellType
                CellType type = cell.getCellType();
                if ( type == CellType.FORMULA ) {
                    switch (cell.getCachedFormulaResultType()) {
                        case BOOLEAN:
                            cellValue = String.valueOf(cell.getBooleanCellValue());
                            break;
                        case NUMERIC:
//                            cellValue = String.valueOf(cell.getNumericCellValue());
                            cellValue = df.formatCellValue(cell, fe);
                            break;
                        case STRING:
                            cellValue = String.valueOf(cell.getRichStringCellValue());
                            break;
                        default:
                            cellValue = df.formatCellValue(cell);
                    }                    
                } else {
                    cellValue = df.formatCellValue(cell);
                }
            } catch (Exception ex) { // if exception thrown, just try to get the raw cell value.
                System.err.println(ex);
                XSSFCell xsc = (XSSFCell)cell;
                cellValue = xsc.getRawValue();
            }

            if ( cellValue == null ) {
                logger.warn("Null at " + rowIdx + ":" + cellIdx);
                cellValue = "";
            }
            rowList.add(cellValue != null ? cellValue : "");
        }
        // Remove trailing empty cells. Depending on how the excel file is set up, 
        // there are sometimes extraneous empty cells returned at the end of a row.
        // If they are actually appropriately empty cells at the end of the row,
        // they will be added back in during the parse/extract dataset phase.
        // This is determined by the row size being less than the number of header columns.
        for (int i = rowList.size()-1; i>=0; i--) {
            if ( rowList.get(i).isEmpty()) {
                rowList.remove(i);
            } else {
                break;  // stop when a non-empty cell is found.
            }
        }
        rowIdx += 1;
        return rowList.toArray(new String[rowList.size()]);
    }

    /* (non-Javadoc)
     * @see gov.noaa.pmel.dashboard.upload.RecordOrientedFileReader#getDelimiter()
     */
    @Override
    public String getDelimiter() {
        return ", ";
    }
    /* (non-Javadoc)
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        try {
            if ( wb != null )
                wb.close();
        } catch (Throwable t) {
            // ignore
        }
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
    public static void main(String[] args) {
        try {
            boolean useStream = false;
            String[] files = new String[] { 
//                    "/Users/kamb/workspace/oa_dashboard_test_data/people/julian/WOAC_metadata_jh100918.xlsx",
//                    "/Users/kamb/workspace/oa_dashboard_test_data/A02_HLY0803.xlsx",
//                    "/Users/kamb/workspace/oa_dashboard_test_data/A02_HLY0803-loose.xlsx",
//                    "/Users/kamb/workspace/oa_dashboard_test_data/A02_HLY0803.xlsx.loose",
                    "/Users/kamb/workspace/oa_dashboard_test_data/WCOA/W03_WCOA2012R_djg_v2.xlsx",
//                    "/Users/kamb/workspace/oa_dashboard_test_data/33GG20171112-from_GDRIVE.xlsx", 
//                    "/Users/kamb/workspace/oa_dashboard_test_data/people/julian/WOAC_metadata-fixed-strict.xlsx",
//                    "/Users/kamb/workspace/oa_dashboard_test_data/WCOA/WCOA11-01-06-2015_data-full.xlsx",
//                    "/Users/kamb/workspace/OADashboard/test-data/WCOA11-01-06-2015_data-full.xlsx", // invalid mark
//                    "/Users/kamb/workspace/OADashboard/test-data/WCOA11-01-06-2015_data-full-resaved.xlsx",
//                    "/Users/kamb/workspace/OADashboard/test-data/WCOA11-fileItem.xlsx",
//                    "/Users/kamb/workspace/OADashboard/test-data/strict-test.xlsx"
                    };
//            if ( useStream ) {
                for (String file : files ) {
                    System.out.println("File:"+file);
                    try ( BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(file));
                          ExcelFileReader efr = newInstance(inStream, OPEN_OFFICE_TYPE); ) {
                        System.out.println(file+":"+efr);
                        int rowIdx = 0;
                        for (String[] row : efr ) {
                            System.out.println(row);
                            rowIdx += 1;
//                            break;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
//            } else {
                for (String file : files ) {
                    try {
                        File zipFile = new File(file);
                        boolean isStrict = checkForStrict(zipFile);
                        System.out.println(zipFile + " is Strict : " + isStrict);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
//            }
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }
}
