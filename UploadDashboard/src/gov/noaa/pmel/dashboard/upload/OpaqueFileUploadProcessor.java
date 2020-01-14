/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FileType;
import gov.noaa.pmel.dashboard.util.FormUtils;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * @author kamb
 *
 */
public class OpaqueFileUploadProcessor extends FileUploadProcessor {

    private static Logger logger = LogManager.getLogger(OpaqueFileUploadProcessor.class);
    
    public OpaqueFileUploadProcessor(StandardUploadFields _uploadFields) {
        super(_uploadFields);
    }

    @Override
    public void doFeatureSpecificProcessing(List<FileItem> datafiles) throws UploadProcessingException {
        boolean multiFileUpload = false;
        String username = _uploadFields.username();
        String datasetId = FormUtils.getFormField("datasetId", _uploadFields.parameterMap());
        String action = _uploadFields.dataAction();
        DataFileHandler datasetHandler = _configStore.getDataFileHandler();

        if ( datafiles.size() > 1 ) {
            multiFileUpload = true;
        }
        for ( FileItem item : datafiles ) {
            String filename = item.getName();
            logger.info("processing OPAQUE upload file " + filename);
            datasetId = getDatasetId(datasetId, filename, multiFileUpload);
            OpaqueDataset pseudoDataset = createPseudoDataset(datasetId, item, _uploadFields);
//            try {
//                saveRawFile(item);
//            } catch (Exception ex) {
//                // TODO: log error, notify admin?
//                ex.printStackTrace();
//            }
                // Check if the dataset already exists
                String itemDatasetId = pseudoDataset.getDatasetId();
                boolean datasetExists = datasetHandler.dataFileExists(itemDatasetId);
                if ( datasetExists ) {
                    String owner = "";
                    String status = "";
                    try {
                        // Read the original dataset info to get the current owner and submit status
                        DashboardDataset oldDataset = datasetHandler.getDatasetFromInfoFile(itemDatasetId);
                        owner = oldDataset.getOwner();
                        status = oldDataset.getSubmitStatus();
                    } catch ( Exception ex ) {
                        // Some problem with the properties file
                        ;
                    }
                    // If only create new datasets, add error message and skip the dataset
                    if ( DashboardUtils.NEW_DATASETS_REQUEST_TAG.equals(action) ) {
                        _messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                filename + " ; " + itemDatasetId + " ; " + owner + " ; " + status);
                        continue;
                    }
                    // Make sure this user has permission to modify this dataset
                    try {
                        datasetHandler.verifyOkayToDeleteDataset(itemDatasetId, username);
                    } catch ( Exception ex ) {
                        _messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                filename + " ; " + itemDatasetId + " ; " + owner + " ; " + status);
                        continue;
                    }
                } 
                try {
                    File datasetDir = datasetHandler.datasetDataFile(itemDatasetId).getParentFile();
                    File uploadedFile = saveOpaqueFileData(pseudoDataset, datasetDir);
                    pseudoDataset.setUploadedFile(uploadedFile.getPath());
                    datasetHandler.saveDatasetInfoToFile(pseudoDataset, "save opaque data info");
                    generateEmptyMetadataFile(itemDatasetId);
                    _successes.add(itemDatasetId);
                    // datasetHandler.saveDatasetDataToFile(pseudoDataset, "save opaque data data");
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
        return _rawFileHandler.writeItem(pseudoDataset.getFileItem(), datasetDir);
    }

    private OpaqueDataset createPseudoDataset(String itemId, FileItem item, StandardUploadFields _uploadFields) {
        OpaqueDataset odd = new OpaqueDataset(itemId);
        odd.setUploadFilename(item.getName());
        odd.setUploadTimestamp(_uploadFields.timestamp());
        odd.setOwner(_uploadFields.username());
        odd.setFileItem(item);
        odd.setFeatureType(_uploadFields.featureType().name());
        odd.setFileType(FileType.OTHER.name());
        return odd;
    }

    private int idCounter = 0;
    private String getDatasetId(String datasetId, String name, boolean multiFileUpload) {
        return StringUtils.emptyOrNull(datasetId) ? name :
                multiFileUpload ? datasetId + ++idCounter : datasetId;
    }

}
