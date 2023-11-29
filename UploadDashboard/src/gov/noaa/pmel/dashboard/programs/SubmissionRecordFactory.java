/**
 * 
 */
package gov.noaa.pmel.dashboard.programs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TreeSet;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.handlers.UserFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.server.util.UIDGen;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardDataset.DashboardDatasetBuilder;
import gov.noaa.pmel.tws.util.FileUtils;

/**
 * @author kamb
 *
 */
public class SubmissionRecordFactory {

    public static String CreateNewEmptyRecord(String forOwner) throws IOException {
        String newSubmissionRecordId = UIDGen.genId();
        DashboardConfigStore cfg = DashboardConfigStore.get(false);
        DataFileHandler dfh = cfg.getDataFileHandler();
        DashboardDatasetBuilder newSubmissionBuilder = DashboardDataset.builder();
        newSubmissionBuilder
            .recordId(newSubmissionRecordId)
//            .featureType(srcDataset.getFeatureType().name())
//            .userObservationType(srcDataset.getUserObservationType())
            .owner(forOwner)
            .uploadTimestamp(DashboardServerUtils.formatUTC(new Date())); // XXX
        
//        .checkerFlags(new TreeSet<>())
//        .numDataRows(0)
//        .numErrorRows(0)
//        .numWarnRows(0)
//        .uploadedFile("")
//        .uploadFilename("")
//        .recordId(newSubmissionRecord)
//        .userColNames(new ArrayList<>())
//        .dataColTypes(new ArrayList<>())
        
//        MetadataFileHandler mfh = cfg.getMetadataFileHandler();
//        File srcMetadata = mfh.getMetadataFile(srcSubmissionRecordId);
//        if ( srcMetadata != null && srcMetadata.exists()) {
//            File destMetadata = mfh.getMetadataFile(newSubmissionRecord);
//            // XXX TODO: We need to add to source control!
//            FileUtils.copyFile(srcMetadata, destMetadata);
//            clonedDatasetBuilder.mdStatus("Cloned from " + srcSubmissionRecordId);
//        }
//        if ( copyAssociatedFiles ) {
//            ArrayList<DashboardMetadata> metaFiles = mfh.getMetadataFiles(srcSubmissionRecordId);
//            TreeSet<String> addlDocs = new TreeSet<>();
//            for ( DashboardMetadata mdInfo : metaFiles ) {
//                String commitMessage = "Cloning file " + mdInfo.getFilename() + " from record " + srcSubmissionRecordId + " into " + newSubmissionRecord;
//                DashboardMetadata newMetadataInfo = mfh.copyMetadataFile(newSubmissionRecord, mdInfo, true);
//                mfh.saveMetadataInfo(newMetadataInfo, commitMessage, true);
//                addlDocs.add(newMetadataInfo.getAddlDocsTitle());
//            }
//            if ( ! addlDocs.isEmpty()) {
//                clonedDatasetBuilder.addlDocs(addlDocs);
//            }
//        }
        dfh.saveDatasetInfoToFile(newSubmissionBuilder.build(), "New empty submission record: " + newSubmissionRecordId);
        UserFileHandler ufh = cfg.getUserFileHandler();
        ufh.addDatasetsToListing(newSubmissionRecordId, forOwner);
        return newSubmissionRecordId;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
