/**
 * 
 */
package gov.noaa.pmel.dashboard.server.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.poi.EmptyFileException;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.util.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import gov.noaa.pmel.dashboard.upload.FileUploadProcessor;

/**
 * @author kamb
 *
 */
public class FileTypeTest {

    static String checkTikaFile(File file) throws Exception {
        URL customConfFile = FileUploadProcessor.class.getResource("/config/tika-config.xml");
        TikaConfig tfig = new TikaConfig(customConfFile, FileUploadProcessor.class.getClassLoader());
        Tika tika = new Tika(tfig);
        String type = tika.detect(file);
        System.out.println("checkTikaFile: "+ type);
        return type;
    }
    
    static String checkTikaStream(BufferedInputStream instream) throws Exception {
        MediaType mt = null;
        TikaInputStream tis = TikaInputStream.get(instream);
        TikaConfig tika = new TikaConfig(FileUploadProcessor.class.getResource("/config/tika-config.xml"), FileUploadProcessor.class.getClassLoader());
        mt = tika.getDetector().detect(tis, new Metadata());
//        System.out.println("checkTikaStream 1: "+ mt.toString());
        return mt.toString();
    }
    
    static String checkPoiFileMagic(BufferedInputStream instream) throws IOException {
        InputStream bufIn = FileMagic.prepareToCheckMagic(instream);
        FileMagic fm = FileMagic.valueOf(bufIn);
        System.out.println("checkPoiMagic:"+fm.toString());
        return fm.toString();
    }
    
    static boolean checkHomegrownType(File file) throws IOException {
        return false;
    }
        
    static String getHomegrownType(File file) throws Exception {
//        System.out.print("==== Checking " + file + " as " + tikaType + "\t" );
        try ( BufferedInputStream instream = new BufferedInputStream(new FileInputStream(file))) {
            return getHomegrownType(instream);
        }
    }
    
    static enum Delimiter {
        comma(','),
        semi(';'),
        tab('\t'),
        pipe('|'),
        angle('<'),
        NONE('X');
        
        char _char;
        Delimiter(char c) {
            _char = c;
        }
        char character() { return _char; } 
    }
    
    static boolean isDelimited(BufferedInputStream instream) throws Exception {
        String homeType = getHomegrownType(instream);
        return isDelimited(homeType);
    }
    
    static boolean isDelimited(String type) {
        System.out.print(" " + type + " " );
        return type.contains("excel")
        || type.contains("spreadsheet")
//        || type.contains("ooxml")
        || ( type.contains("text")
            && ( type.contains("delimited")
                || type.contains("csv"))
            );
    }
            
    static String getHomegrownType(BufferedInputStream instream) throws Exception {
        int available = instream.available();
        int peekn = Math.min(available-5, 16384);
        byte[] bytes = IOUtils.peekFirstNBytes(instream, peekn);
        String peek = new String(bytes, Charset.forName("UTF8"));
        SortedMap<Integer, Delimiter> sortedCount = countChars(peek);
//        System.out.println("sort of " + peek.length()+"/"+peekn +": "+ sortedCount);
        if ( sortedCount.size() < 2 ) {
            return checkTikaStream(instream);
        }
        Iterator<Entry<Integer, Delimiter>> iterator = sortedCount.entrySet().iterator();
        Entry<Integer, Delimiter> first = iterator.next();
        Entry<Integer, Delimiter> second = iterator.next();
        if ( looksLikeDelimited(first, second, peekn)) {
            return "text/x-"+first.getValue().name()+"-delimited";
        } else {
            return checkTikaStream(instream);
        }
    }
    
    /**
     * @param first
     * @param second
     * @param length
     * @return
     */
    private static boolean looksLikeDelimited(Entry<Integer, Delimiter> first, 
                                              Entry<Integer, Delimiter> second,
                                              int length) {
//        System.out.print("comparing " + first + " against " + second + " on " + length + " : ");
        int nFirst = first.getKey().intValue();
        int nSecond = second.getKey().intValue();
        boolean looksLikeIt = ! first.getValue().equals(Delimiter.angle) 
                                && atLeast( nFirst, length, .1 ) 
                                && lessThan( nSecond, nFirst, .5);
//        System.out.println(looksLikeIt);
        return looksLikeIt;
    }
    
    private static final double DEFAULT_SLACK = .1; // +/- 10%
    
    static boolean atLeast(int smaller, int larger, double percentage) {
        return atLeast(smaller, larger, percentage, DEFAULT_SLACK);
    }
    static boolean atLeast(int smaller, int larger, double percentage, double slack) {
        try {
        double d = (double)smaller/larger; 
        return d >= (percentage - (slack*percentage));
        } catch (Error e) {
            return true;
        }
    }
    static boolean lessThan(int smaller, int larger, double percentage) {
        return lessThan(smaller, larger, percentage, DEFAULT_SLACK);
    }
    static boolean lessThan(int smaller, int larger, double percentage, double slack) {
        try {
            double d = (double)smaller/larger; 
            return d <= (percentage + (slack*percentage));
        } catch (Error e) {
            return true;
        }
    }
    
    static boolean roughly(int v1, int v2, double percentage, double slack) {
        double d = (double)(v1-v2)/v2;
//        System.out.println("morethan d " + d + " vs " + percentage + " with " + slack);
        double delta = slack * percentage;
        return d <= (percentage + delta) && d >= (percentage - delta);
    }

    static String getHomegrownDelimiter(BufferedInputStream instream) throws EmptyFileException, IOException {
        byte[] bytes = IOUtils.peekFirstNBytes(instream, 4096);
        String peak = new String(bytes, Charset.forName("UTF8"));
        char spacer = lookForSpacer(peak);
//        System.out.println("iFoundSpacer:"+spacer+".");
        return String.valueOf(spacer); 
    }
    
    /* Bad news!  Closes stream.
    static String checkUnivocityDelimiter(BufferedInputStream instream) throws IOException {
        CsvParserSettings settings = new CsvParserSettings();
        settings.detectFormatAutomatically();
        settings.setDelimiterDetectionEnabled(true, '\t', ';', ',', '|');
        
        settings.setCommentCollectionEnabled(true);
        settings.setNullValue("");
        settings.setEmptyValue("");
        settings.setSkipEmptyLines(true); // default
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(instream, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        CsvParser dataParser = new CsvParser(settings);
        BufferedReader dataReader = new BufferedReader(new InputStreamReader(bais));
        dataParser.beginParsing(dataReader);
        CsvFormat fileFormat = dataParser.getDetectedFormat();
        System.out.println("uniSpacer:"+fileFormat.getDelimiterString()+".");
        return fileFormat.getDelimiterString();
    }
    */
    
    static String getUnivocityDelimiter(InputStream instream) throws IOException {
        CsvParserSettings settings = new CsvParserSettings();
        settings.detectFormatAutomatically();
        settings.setDelimiterDetectionEnabled(true, '\t', ';', ',', '|');
        
        settings.setCommentCollectionEnabled(true);
        settings.setNullValue("");
        settings.setEmptyValue("");
        settings.setSkipEmptyLines(true); // default
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(instream, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        CsvParser dataParser = new CsvParser(settings);
        BufferedReader dataReader = new BufferedReader(new InputStreamReader(bais));
        dataParser.beginParsing(dataReader);
        CsvFormat fileFormat = dataParser.getDetectedFormat();
        System.out.println("uniFormat:"+fileFormat.toString());
        return fileFormat.getDelimiterString();
    }
    
    static boolean sacrificeStream(BufferedInputStream bis) throws IOException {
        int i = 0;
        int b;
        System.out.println(bis.available());
        do {
            b = bis.read();
        } while ( b >= 0 && i++ < 10);
        return b >= 0;
    }
    static boolean checkFirstLine(BufferedInputStream bis) throws IOException {
        int size = 25;
        byte[] first100 = new byte[size];
        bis.mark(size+1);
        int read = bis.read(first100, 0, first100.length);
        bis.reset();
        if ( read < 0 ) {
            System.out.println("**** Read from stream failed.");
            return false;
        } else {
//            System.out.println("Line:"+new String(first100)+". " + read);
            return true;
        }
    }
    
    static void checkFile(File txtFile) throws Exception {
        System.out.println("==== Checking file " + txtFile);
        try (FileInputStream fis = new FileInputStream(txtFile);
             BufferedInputStream bis = new BufferedInputStream(fis); ) {
           checkTikaFile(txtFile);
           checkFirstLine(bis);
           System.out.println("Tika stream type:" + checkTikaStream(bis));
           checkFirstLine(bis);
           checkPoiFileMagic(bis);
           checkFirstLine(bis);
           System.out.println("Homegrown type:" + getHomegrownType(bis));
           checkFirstLine(bis);
           System.out.println("Homegrown delimiter:[" + getHomegrownDelimiter(bis) + "]");
           checkFirstLine(bis);
           // works, leaves bis intact, passing in bis closes the stream!
           getUnivocityDelimiter(fis);
           checkFirstLine(bis);
           // does not work, reads all of bis.
//           checkUnivocityDelimiter(bis);
//           checkFirstLine(bis);
        }
        
    }
    
    static void checkExcelFile(File excelFile) throws Exception {
        System.out.println("==== Checking file " + excelFile);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(excelFile)); ) {
           checkTikaFile(excelFile);
           checkFirstLine(bis);
           checkTikaStream(bis);
           checkFirstLine(bis);
           checkPoiFileMagic(bis);
        }        
    }
    
    static void checkDelimitedFile(File otherFile, boolean isDelimited) throws Exception {
        try (FileInputStream fis = new FileInputStream(otherFile);
             BufferedInputStream bis = new BufferedInputStream(fis); ) {
            System.out.print("==== Checking file " + otherFile);
            boolean foundDelimited = isDelimited(bis);
            System.out.println(foundDelimited + ( foundDelimited == isDelimited ? " CORRECT" : " WRONG!"));
        }
    }
    
    private static char lookForSpacer(String peak) {
        SortedMap<Integer, Delimiter> sort = countChars(peak);
        return sort.values().iterator().next().character();
    }
    
    private static SortedMap<Integer, Delimiter> countChars(String text) {
        SortedMap<Integer, Delimiter> sort = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.intValue() - o1.intValue();
            }
        });
        sort.put(count(text, Delimiter.comma.character()), Delimiter.comma);
//        sort.put(count(text, '.'), new Character('.'));
        sort.put(count(text, Delimiter.semi.character()), Delimiter.semi);
        sort.put(count(text, Delimiter.tab.character()), Delimiter.tab);
        sort.put(count(text, Delimiter.pipe.character()), Delimiter.pipe);
        sort.put(count(text, Delimiter.angle.character()), Delimiter.angle);
        sort.put(new Integer(0), Delimiter.NONE);
        return sort;
    }

    /**
     * @param peak
     * @param c
     * @return
     */
    private static Integer count(String peak, char c) {
        int count = 0;
        for (int i = 0; i < peak.length(); i++) {
            if ( peak.charAt(i) == c) {
                count += 1;
            }
        }
        return new Integer(count);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            File testdata = new File("/Users/kamb/workspace/oa_dashboard_test_data");
            File typecheck = new File(testdata, "typechecker");
            boolean checkDelim = false;
            if ( checkDelim ) {
                checkDelimitedFile(new File(typecheck, "0117971_map.jpg"), false);
                checkDelimitedFile(new File(typecheck, "0117971_mapjpg.png"), false);
                checkDelimitedFile(new File(typecheck, "0117971_mapjpg.xls"), false);
    //            checkDelimitedFile(new File(typecheck, "33RO20150410.exc.csv"));
                checkDelimitedFile(new File(typecheck, "33RO20150410.short.csv"), true);
    //            checkDelimitedFile(new File(typecheck, "33RO20150410.short.csv.really"));
    //            checkDelimitedFile(new File(typecheck, "33RO20150410.stripped.csv"));
    //            checkDelimitedFile(new File(typecheck, "33RO20150410.stripped_error.csv"));
                checkDelimitedFile(new File(typecheck, "58HJ20120807.exc.pipe"), true);
                checkDelimitedFile(new File(typecheck, "58HJ20120807.exc.semi"), true);
                checkDelimitedFile(new File(typecheck, "33RO20150410.exccsv.xlsx"), true);
                checkDelimitedFile(new File(typecheck, "Standard_World_Time_Zones.pdf"), false);
                checkDelimitedFile(new File(typecheck, "Standard_World_Time_Zones.png"), false);
                checkDelimitedFile(new File(typecheck, "EffectiveJava3.epub"), false);
                checkDelimitedFile(new File(typecheck, "GOULD_12_04_data_report.pdf"), false);
                checkDelimitedFile(new File(typecheck, "ME_DOCUMENT.json"), false);
                checkDelimitedFile(new File(typecheck, "OS_PAPA_2018PA012_D_ADCP-30min.nc"), false);
                checkDelimitedFile(new File(typecheck, "SDIS_UD3KMEN5_bagit-sha256.txt"), false);
                checkDelimitedFile(new File(typecheck, "SDIS_UD3KMEN5_bagit.zip"), false);
                checkDelimitedFile(new File(typecheck, "StandardUploadProcessor.java"), false);
                checkDelimitedFile(new File(typecheck, "UMM-S_V1.2_20180530.docx"), false);
                checkDelimitedFile(new File(typecheck, "WCOA2013_Hydro-data_06-20-2016-test-no-extension"), true);
                checkDelimitedFile(new File(typecheck, "WCOA2013_Hydro-data_06-20-2016.csv.uneven_rows"), true);
                checkDelimitedFile(new File(typecheck, "jh100918-fixed.xls"), true);
                checkDelimitedFile(new File(typecheck, "jh100918-fixed.xlsx"), true);
                checkDelimitedFile(new File(typecheck, "jh100918-fixedxls.csv"), true);
                checkDelimitedFile(new File(typecheck, "oap_metadata_33RO20160505.xml"), false);
                checkDelimitedFile(new File(typecheck, "t2n180w_5day.ascii"), false);
                checkDelimitedFile(new File(typecheck, "word.doc"), false);
            } else {
                checkFile(new File(typecheck, "0117971_map.jpg"));
                checkFile(new File(typecheck, "0117971_mapjpg.png"));
                checkFile(new File(typecheck, "0117971_mapjpg.xls"));
    //            checkFile(new File(typecheck, "33RO20150410.exc.csv"));
                checkFile(new File(typecheck, "33RO20150410.short.csv"));
    //            checkFile(new File(typecheck, "33RO20150410.short.csv.really"));
    //            checkFile(new File(typecheck, "33RO20150410.stripped.csv"));
    //            checkFile(new File(typecheck, "33RO20150410.stripped_error.csv"));
                checkFile(new File(typecheck, "58HJ20120807.exc.pipe"));
                checkFile(new File(typecheck, "58HJ20120807.exc.semi"));
                checkFile(new File(typecheck, "33RO20150410.exccsv.xlsx"));
                checkFile(new File(typecheck, "Standard_World_Time_Zones.pdf"));
                checkFile(new File(typecheck, "Standard_World_Time_Zones.png"));
                checkFile(new File(typecheck, "EffectiveJava3.epub"));
                checkFile(new File(typecheck, "GOULD_12_04_data_report.pdf"));
                checkFile(new File(typecheck, "ME_DOCUMENT.json"));
                checkFile(new File(typecheck, "OS_PAPA_2018PA012_D_ADCP-30min.nc"));
                checkFile(new File(typecheck, "SDIS_UD3KMEN5_bagit-sha256.txt"));
                checkFile(new File(typecheck, "SDIS_UD3KMEN5_bagit.zip"));
                checkFile(new File(typecheck, "StandardUploadProcessor.java"));
                checkFile(new File(typecheck, "UMM-S_V1.2_20180530.docx"));
                checkFile(new File(typecheck, "WCOA2013_Hydro-data_06-20-2016-test-no-extension"));
                checkFile(new File(typecheck, "WCOA2013_Hydro-data_06-20-2016.csv.uneven_rows"));
                checkFile(new File(typecheck, "jh100918-fixed.xls"));
                checkFile(new File(typecheck, "jh100918-fixed.xlsx"));
                checkFile(new File(typecheck, "jh100918-fixedxls.csv"));
                checkFile(new File(typecheck, "oap_metadata_33RO20160505.xml"));
                checkFile(new File(typecheck, "t2n180w_5day.ascii"));
                checkFile(new File(typecheck, "word.doc"));
            }
//            File wcoa = new File(testdata, "WCOA_2013");
//            File glo = new File(testdata, "GLODAP");
//            checkFile(new File(wcoa, "WCOA2013_Hydro-data_06-20-2016-1.csv"));
//            checkFile(new File(wcoa, "WCOA2013_Hydro-data_06-20-2016-1.csv"));
////            checkTextFile(new File(wcoa, "WCOA2013_Hydro-data_06-20-2016.csv.trailers"));
////            checkTextFile(new File(wcoa, "WCOA2013_Hydro-data_06-20-2016.csv.uneven_rows"));
//            checkFile(new File(glo, "58HJ20120807.exc.csv"));
//            getHomegrownType(new File(glo, "58HJ20120807.exc.tab"));
//            getHomegrownType(new File(glo, "58HJ20120807.exc.semi"));
//            getHomegrownType(new File(glo, "58HJ20120807.exc.pipe"));
////            getHomegrownType(new File(glo, "58HJ20120807.exc.dollar"));
//            getHomegrownType(new File(wcoa, "WCOA2013_Hydro-data_06-20-2016.xlsx"));
//            getHomegrownType(new File(testdata, "PRISM022008_OADS.xml"));
//            getHomegrownType(new File(testdata, "33RO20150410.short.csv"));
//            getHomegrownType(new File(testdata, "33LG20120408/GOULD_12_04_data_report.pdf"));
//            checkExcelFile(new File(wcoa, "WCOA2013_Hydro-data_06-20-2016-test-no-extension"));
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }

    private static boolean fileIsText(String fileType) {
        return fileType.contains("text");
    }

    public static String getFileType(BufferedInputStream instream) throws Exception {
        return getHomegrownType(instream);
    }
    
    /**
     * @param rawFile
     * @return
     * @throws Exception 
     */
    public static String getFileType(File rawFile) throws Exception {
        return getHomegrownType(rawFile);
    }
    
//    private static boolean fileIsReadable(String fileType) {
//        return fileType.contains("excel") 
//                || fileType.contains("spreadsheet")
//                || fileIsText(fileType);
//    }
    public static boolean fileIsDelimited(String fileType) {
        if ( fileType == null ) { return false; }
        fileType = fileType.toLowerCase();
        return fileType.contains("excel")
                || fileType.contains("spreadsheet")
                || ( fileType.contains("text")
                    && ( fileType.contains("delimited")
                        || fileType.contains("csv"))
               );
    }
}
