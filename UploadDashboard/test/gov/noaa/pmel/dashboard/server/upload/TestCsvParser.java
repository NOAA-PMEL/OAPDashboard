/**
 * 
 */
package gov.noaa.pmel.dashboard.server.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import gov.noaa.pmel.dashboard.upload.CSVFileReader;

/**
 * @author kamb
 *
 */
public class TestCsvParser {

    public static void testFile(File csvFile) throws FileNotFoundException, IOException {
        try ( InputStream fis = new FileInputStream(csvFile); ) {
            CSVFileReader reader = new CSVFileReader(fis, "csv");
            int nrow = 0;
            for ( String[] row : reader) {
                System.out.println(nrow++ + ":"+row.length);
            }
        }
    }
        
    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            File dataDir = new File("/Users/kamb/workspace/oa_dashboard_test_data/NCEI/Adrienne/data.nodc.noaa.gov/ncei/ocads/data/0100065");
            File file = new File(dataDir, "BTM_64W_32N_Jul06_Mar07.csv");
            testFile(file);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }

}
