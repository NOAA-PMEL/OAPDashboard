/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.oads.util.StringUtils;
import gov.noaa.pmel.tws.util.FileUtils;
import gov.noaa.pmel.tws.util.Logging;

/**
 * @author kamb
 *
 */
public class NewGeneralizedUploadProcessor extends FileUploadProcessor {

    
    private static Logger logger = Logging.getLogger(NewGeneralizedUploadProcessor.class);
    
    public NewGeneralizedUploadProcessor(StandardUploadFields uploadFields) {
        super(uploadFields);
    }

    @Override
    public void processUploadedFile(boolean isUpdateRequest) throws UploadProcessingException {
            String filename = _uploadedFile.getName();
//            String itemType = item.getContentType();
            // Get the datasets from this file
//            TreeMap<String,DashboardDatasetData> datasetsMap = null;
            
            datasetIdColName = null; // we no longer deal with this
            specifiedDatasetId = StringUtils.emptyOrNull(specifiedDatasetId) ? 
                                    filename :
                                    specifiedDatasetId; 
            DashboardDatasetData datasetData;
            try ( InputStream inStream = new FileInputStream(_uploadedFile); ) {
                RecordOrientedFileReader recordReader = 
                        RecordOrientedFileReader.getFileReader(_uploadFields.checkedFileType(), inStream);
                datasetData = DataFileHandler.createSingleDatasetFromInput(recordReader, dataFormat, 
                                                                      username, filename, timestamp, 
                                                                      submissionRecordId,
                                                                      specifiedDatasetId, 
                                                                      datasetIdColName);
            } catch (IllegalStateException ex) {
                logger.warn(ex);
                throw ex;
            } catch (IOException ex) {
                // Mark as a failed file, and go on to the next
                _messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
                _messages.add("There was an error processing the data file: " + ex.getMessage() + ".");
                _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                return;
            }

            // Process all the datasets created from this file
            String datasetId = null;
//            for ( DashboardDatasetData datasetData : datasetsMap.values() ) {
            try {
//                File datasetFile = _dataFileHandler.datasetDataFile(submissionRecordId); // , _uploadedFile.getName());
                datasetData.setFileType(fileType.name());
                datasetData.setFeatureType(_featureType.name());
                datasetData.setUserObservationType(observationType);
                datasetData.setUploadedFile(_uploadedFile.getPath());
                datasetData.setUserDatasetName(specifiedDatasetId);
                // Check if the dataset already exists
                datasetId = datasetData.getDatasetId();
                boolean datasetExists = _dataFileHandler.dataFileExists(datasetId); // , datasetFile.getName());
                boolean appended = false;
                if ( isUpdateRequest ) {
                    DashboardDataset oldDataset = _dataFileHandler.getDatasetFromInfoFile(datasetId);
                    String owner = oldDataset.getOwner();
                    String status = oldDataset.getSubmitStatus();
                    if ( datasetExists ) {  // replacing uploaded file for same submission record
                        // If only create new datasets, add error message and skip the dataset
                        if ( DashboardUtils.NEW_DATASETS_REQUEST_TAG.equals(action) ) {
                            _messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                    filename + " ; " + datasetId + " ; " + owner + " ; " + status);
                            throw new UploadProcessingException("Request to create new dataset, but dataset exists for " 
                                                                + filename + " ; " + datasetId + " ; " + owner );
    //                        continue;
                        }
                        // Make sure this user has permission to modify this dataset
                        try {
                            _dataFileHandler.verifyOkayToDeleteDataset(datasetId, username);
                        } catch ( Exception ex ) {
                            _messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                    filename + " ; " + datasetId + " ; " + owner + " ; " + status);
                            throw new UploadProcessingException(ex);
    //                        continue;
                        }
                        
                        String uploadedFilename = oldDataset.getUploadFilename();
                        if ( ! StringUtils.emptyOrNull(uploadedFilename)) {
                            File previousFile = _dataFileHandler.datasetDataFile(datasetId, uploadedFilename);
    //                        if ( ! _uploadedFile.getName().equals(previousFile.getName())) {
                            if ( previousFile.exists()) {
                                if ( !previousFile.delete()) {
                                    logger.warn("Failed to delete previous file:" + uploadedFilename);
                                }
                            }
                        }
    
    //                    if ( DashboardUtils.APPEND_DATASETS_REQUEST_TAG.equals(action) ) {
    //                        // Get all the data from the existing dataset
    //                        DashboardDatasetData oldDataset;
    //                        try {
    //                            oldDataset = _dataFileHandler.getDatasetDataFromFiles(datasetId, 0, -1);
    //                        } catch ( Exception ex ) {
    //                            _messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
    //                                    filename + " ; " + datasetId);
    //                            _messages.add(ex.getMessage());
    //                            _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
    //                            throw new UploadProcessingException(ex);
    ////                            continue;
    //                        }
    //                        // If append to dataset, at this time insist on the column names being the same
    //                        if ( ! datasetData.getUserColNames().equals(oldDataset.getUserColNames()) ) {
    //                            _messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
    //                            _messages.add("Data column names for existing dataset " + datasetId);
    //                            _messages.add("    " + oldDataset.getUserColNames().toString());
    //                            _messages.add("do not match those in uploaded file " + filename);
    //                            _messages.add("    " + datasetData.getUserColNames());
    //                            _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
    //                            throw new UploadProcessingException("Uploaded file headers do not match existing dataset headers.");
    ////                            continue;
    //                        }
    //                        // Update information on the existing dataset to reflect updated data
    //                        // leave the original owner and any archive date
    //                        oldDataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_NOT_CHECKED);
    //                        oldDataset.setSubmitStatus(DashboardUtils.STATUS_NOT_SUBMITTED);
    //                        oldDataset.setArchiveStatus(DashboardUtils.ARCHIVE_STATUS_NOT_SUBMITTED);
    //                        oldDataset.setUploadFilename(filename);
    //                        oldDataset.setUploadTimestamp(timestamp);
    //                        oldDataset.setVersion(_configStore.getUploadVersion());
    //                        // Add the add to the dataset
    //                        int rowNum = oldDataset.getNumDataRows();
    //                        for ( ArrayList<String> datavals : datasetData.getDataValues() ) {
    //                            rowNum++;
    //                            oldDataset.getDataValues().add(datavals);
    //                            oldDataset.getRowNums().add(rowNum);
    //                        }
    //                        oldDataset.setNumDataRows(rowNum);
    //                        // Replace the reference to the uploaded dataset with this appended dataset
    //                        datasetData = oldDataset;
    //                        appended = true;
    //                    }
                    } else { // isUpdate, but data file doesn't exist.  Must be upload to cloned submission
                        String mdStatus = oldDataset.getMdStatus();
                        datasetData.setMdStatus(mdStatus);
                    }
                } else { // not update request
                    // At this point, datasetData is the dataset to save, regardless of new, overwrite, or append
                    try {
                        MetadataFileHandler mdataHandler = _configStore.getMetadataFileHandler();
                        mdataHandler.createEmptyOADSMetadataFile(datasetId);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                    
                // Add any existing documents for this cruise
                ArrayList<DashboardMetadata> mdataList = 
                        _configStore.getMetadataFileHandler().getMetadataFiles(datasetId);
                TreeSet<String> addlDocs = new TreeSet<String>();
                for ( DashboardMetadata mdata : mdataList ) {
                    if ( MetadataFileHandler.autoExtractedMdFilename(datasetId).equals(mdata.getFilename())) {
                        // Ignore the auto-extracted XML stub file
                    }
                    else if ( MetadataFileHandler.metadataFilename(datasetId).equals(mdata.getFilename())) {
                        datasetData.setMdTimestamp(mdata.getUploadTimestamp());                 
                    }
                    else {
                        addlDocs.add(mdata.getAddlDocsTitle());
                    }
                }
                datasetData.setAddlDocs(addlDocs);
        
                // Save the cruise file and commit it to version control
                try {
                    String commitMsg;
                    if ( appended )
                        commitMsg = "file for " + datasetId + " appended to by " + 
                                username + " from uploaded file " + filename;
                    else if ( datasetExists )
                        commitMsg = "file for " + datasetId + " updated by " + 
                                username + " from uploaded file " + filename;
                    else
                        commitMsg = "file for " + datasetId + " created by " + 
                                username + " from uploaded file " + filename;           
                    _dataFileHandler.saveDatasetInfoToFile(datasetData, "Dataset info " + commitMsg);
                    _dataFileHandler.saveDatasetDataToFile(datasetData, "Dataset data " + commitMsg);
                    try {
                        File uploadDataFile = _dataFileHandler.datasetDataFile(datasetId, _uploadedFile.getName());
                        Files.copy(_uploadedFile.toPath(), 
                                   uploadDataFile.toPath());
                    } catch (IOException iox) {
                        iox.printStackTrace();
                    }
                    
                    // Success
                    _messages.add(DashboardUtils.SUCCESS_HEADER_TAG + " " + datasetId);
                    _successes.add(datasetId);
                
//                } catch (IOException iox) {
//                    _messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
//                            filename + " ; " + datasetId);
//                    _messages.add("Failed to save data file.");
//                    _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
//                    throw new UploadProcessingException(iox);
                } catch (IllegalArgumentException ex) {
                    _messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
                            filename + " ; " + datasetId);
                    _messages.add(ex.getMessage());
                    _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                    throw new UploadProcessingException(ex);
//                    continue;
                }
        
//                // Success
//                _messages.add(DashboardUtils.SUCCESS_HEADER_TAG + " " + datasetId);
//                _successes.add(datasetId);
            } catch (UploadProcessingException upx) {
                logger.warn(upx);
            }
//        }
        // Update the list of cruises for the user
        try {
            _configStore.getUserFileHandler().addDatasetsToListing(_successes, username);
        } catch (IllegalArgumentException ex) {
            throw new UploadProcessingException("Unexpected error updating list of datasets \n" + ex.getMessage(), ex);
        }
    }
}
