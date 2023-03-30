/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.util.FormUtils;
import gov.noaa.pmel.tws.util.FileUtils;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * @author kamb
 *
 */
public class NewOpaqueFileUploadProcessor extends FileUploadProcessor {

    private static Logger logger = LogManager.getLogger(NewOpaqueFileUploadProcessor.class);
    
    public NewOpaqueFileUploadProcessor(StandardUploadFields _uploadFields) {
        super(_uploadFields);
    }

    @Override
    public void processUploadedFile(boolean isUpdateRequest) throws UploadProcessingException {
        String datasetId = FormUtils.getFormField("datasetId", _uploadFields.parameterMap());
        List<File> datafiles = _uploadFields.dataFiles();

        // Currently only allowing upload of 1 file.
//        boolean multiFileUpload = false;
//        if ( datafiles.size() > 1 ) {
//            multiFileUpload = true;
//        }
        for ( File item : datafiles ) { // Note: Only allow one at the moment
            String filename = item.getName();
            logger.info("processing " + _uploadFields.fileType() + " upload file " + filename);
            datasetId = submissionRecordId; // getDatasetId(datasetId, filename, multiFileUpload);
            OpaqueDataset pseudoDataset = createPseudoDataset(isUpdateRequest, datasetId, item); // , _uploadedFile, _uploadFields);
            String userDatasetName = StringUtils.emptyOrNull(specifiedDatasetId) ?
                                      _uploadedFile.getName() :
                                      specifiedDatasetId;
            pseudoDataset.setUserDatasetName(userDatasetName );;
                // Check if the dataset already exists
                String itemDatasetId = pseudoDataset.getDatasetId();
                boolean datasetDataDirExists = _dataFileHandler.datasetDataDirExists(itemDatasetId);
                if ( isUpdateRequest ) { 
                    DashboardDataset oldDataset = _dataFileHandler.getDatasetFromInfoFile(datasetId);
                    String owner = oldDataset.getOwner();
                    String status = oldDataset.getSubmitStatus();
                    String mdStatus = oldDataset.getMdStatus();
                    if ( "Validated".equals(mdStatus)) {
                        mdStatus = "Check Metadata";
                    }
                    pseudoDataset.setMdStatus(mdStatus);
                    pseudoDataset.setMdTimestamp(oldDataset.getMdTimestamp());
                    if ( datasetDataDirExists ) {
                        // If only create new datasets, add error message and skip the dataset
                        if ( DashboardUtils.NEW_DATASETS_REQUEST_TAG.equals(action) ) {
                            _messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                    filename + " ; " + itemDatasetId + " ; " + owner + " ; " + status);
                            continue;
                        }
                        // Make sure this user has permission to modify this dataset
                        try {
                            _dataFileHandler.verifyOkayToDeleteDataset(itemDatasetId, username);
                        } catch ( Exception ex ) {
                            _messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                    filename + " ; " + itemDatasetId + " ; " + owner + " ; " + status);
                            continue;
                        }
                        String uploadedFilePath = oldDataset.getUploadedFile();
                        if ( ! StringUtils.emptyOrNull(uploadedFilePath)) {
                            File previousFile = new File(uploadedFilePath);
                            if ( ! _uploadedFile.getName().equals(previousFile.getName())) {
                                if ( !previousFile.delete()) {
                                    logger.warn("Failed to delete previous file:" + uploadedFilePath);
                                }
                            }
                        }
                    }
                }
                try {
                    File datasetDir = _dataFileHandler.datasetDataFile(itemDatasetId).getParentFile();
                    File uploadedFile = saveOpaqueFileData(pseudoDataset, datasetDir);
                    pseudoDataset.setUploadedFile(uploadedFile.getPath());
                    _dataFileHandler.saveDatasetInfoToFile(pseudoDataset, "save opaque data info");
                    _successes.add(itemDatasetId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                if ( ! _successes.isEmpty()) {
                    _messages.add(DashboardUtils.SUCCESS_HEADER_TAG + " " + datasetId);
                }
                
                // Update the list of cruises for the user
                try {
                    _configStore.getUserFileHandler().addDatasetsToListing(_successes, username);
                } catch (IllegalArgumentException ex) {
                    throw new UploadProcessingException("Unexpected error updating list of datasets \n" + ex.getMessage(), ex);
                }
    
        }
    }
    
    private File saveOpaqueFileData(OpaqueDataset pseudoDataset, File datasetDir) throws Exception {
        // don't need to do this. -- Why not?
        File datasetFile = new File(datasetDir, _uploadedFile.getName());
        FileUtils.copyFile(_uploadedFile, datasetFile);
        return datasetFile;
    }

    private OpaqueDataset createPseudoDataset(boolean isUpdateRequest, String itemId, File item) { 
        OpaqueDataset odd = new OpaqueDataset(itemId);
        odd.setUploadFilename(item.getName());
        odd.setUploadTimestamp(_uploadFields.timestamp());
        odd.setRecordId(itemId);
        odd.setUploadedFile(_uploadedFile.getPath());
        odd.setOwner(_uploadFields.username());
//        odd.setFileItem(item);
        odd.setFeatureType(_uploadFields.featureType().name());
        odd.setUserObservationType(_uploadFields.observationType());
        odd.setFileType(_uploadFields.fileType().name());
        if (isUpdateRequest) {
            DashboardDataset existgDataset = _dataFileHandler.getDatasetFromInfoFile(itemId);
            odd.setMdStatus(existgDataset.getMdStatus());
        }
        return odd;
    }

//    private int idCounter = 0;
//    private String getDatasetId(String datasetId, String name, boolean multiFileUpload) {
//        return StringUtils.emptyOrNull(datasetId) ? name :
//                multiFileUpload ? datasetId + ++idCounter : datasetId;
//    }
}
