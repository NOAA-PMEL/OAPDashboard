/**
 * 
 */
package gov.noaa.pmel.dashboard.test.handlers;

import java.io.File;
import java.nio.file.Path;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import gov.noaa.pmel.dashboard.handlers.Bagger;

/**
 * @author kamb
 *
 */
public class TestBagger {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String destDirName = "bag";
        String zipFilePath = "test-data/BE3G7H849_bagit.zip";
        Path destPath = new File(destDirName).toPath();
        try {
            Bagger.unzip(zipFilePath, destDirName);
            Bag theBag = new BagReader().read(destPath);
            BagVerifier bv = new BagVerifier();
            bv.isComplete(theBag, true);
            bv.isValid(theBag, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }

}
