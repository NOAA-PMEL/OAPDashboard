/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.util.FormUtils;

/**
 * @author kamb
 *
 */
public class StandardUploadProcessor extends FileUploadProcessor {

    public StandardUploadProcessor(StandardUploadFields _uploadFields) {
        super(_uploadFields);
    }

    @Override
    public void doFeatureSpecificProcessing(List<FileItem> datafiles) throws UploadProcessingException {
        String action = _uploadFields.dataAction();
        String username = _uploadFields.username();
        String timestamp = _uploadFields.timestamp();
        String encoding = _uploadFields.fileDataEncoding();
        String dataFormat = FormUtils.getFormField("dataformat", _uploadFields.parameterMap());
        String specifiedDatasetId = FormUtils.getFormField(DATASET_ID_FIELD_NAME, _uploadFields.parameterMap());
        String datasetIdColName = FormUtils.getFormField(DATASET_ID_COLUMN_FIELD_NAME, _uploadFields.parameterMap());
        DataFileHandler datasetHandler = _configStore.getDataFileHandler();
        
        for ( FileItem item : datafiles ) {
            // Get the datasets from this file
            TreeMap<String,DashboardDatasetData> datasetsMap = null;
            String filename = item.getName();
            
            try ( InputStream itemStream = item.getInputStream();
                    BufferedInputStream bufStream = new BufferedInputStream(itemStream);
                    BufferedReader cruiseReader = 
                        new BufferedReader( new InputStreamReader(bufStream)); ) {
//                checkFileFormat(bufStream);
                datasetsMap = datasetHandler.createDatasetsFromInput(cruiseReader, dataFormat, 
                                                                     username, filename, timestamp, 
                                                                     specifiedDatasetId, datasetIdColName);
            } catch (IllegalStateException ex) {
                _messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
                _messages.add(ex.getMessage());
                _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                item.delete();
                continue;
            } catch (Exception ex) {
                // Mark as a failed file, and go on to the next
                _messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
                _messages.add("There was an error processing the data file.");
                _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                item.delete();
                continue;
            }
            File rawFile = null;
            try {
                rawFile = saveRawFile(item);
            } catch (Exception ex) {
                throw new UploadProcessingException("Unable to save uploaded file.", ex);
            }
            
            // done with the uploaded data file
            item.delete();

            // Process all the datasets created from this file
            String datasetId = null;
            for ( DashboardDatasetData datasetData : datasetsMap.values() ) {
                datasetData.setFileType(_fileType.name());
                datasetData.setFeatureType(_featureType.name());
                datasetData.setUploadedFile(rawFile.getPath());
                // Check if the dataset already exists
                datasetId = datasetData.getDatasetId();
                boolean datasetExists = datasetHandler.dataFileExists(datasetId);
                boolean appended = false;
                if ( datasetExists ) {
                    String owner = "";
                    String status = "";
                    try {
                        // Read the original dataset info to get the current owner and submit status
                        DashboardDataset oldDataset = datasetHandler.getDatasetFromInfoFile(datasetId);
                        owner = oldDataset.getOwner();
                        status = oldDataset.getSubmitStatus();
                    } catch ( Exception ex ) {
                        // Some problem with the properties file
                        ;
                    }
                    // If only create new datasets, add error message and skip the dataset
                    if ( DashboardUtils.NEW_DATASETS_REQUEST_TAG.equals(action) ) {
                        _messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                filename + " ; " + datasetId + " ; " + owner + " ; " + status);
                        continue;
                    }
                    // Make sure this user has permission to modify this dataset
                    try {
                        datasetHandler.verifyOkayToDeleteDataset(datasetId, username);
                    } catch ( Exception ex ) {
                        _messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                filename + " ; " + datasetId + " ; " + owner + " ; " + status);
                        continue;
                    }
                    if ( DashboardUtils.APPEND_DATASETS_REQUEST_TAG.equals(action) ) {
                        // Get all the data from the existing dataset
                        DashboardDatasetData oldDataset;
                        try {
                            oldDataset = datasetHandler.getDatasetDataFromFiles(datasetId, 0, -1);
                        } catch ( Exception ex ) {
                            _messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
                                    filename + " ; " + datasetId);
                            _messages.add(ex.getMessage());
                            _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                            continue;
                        }
                        // If append to dataset, at this time insist on the column names being the same
                        if ( ! datasetData.getUserColNames().equals(oldDataset.getUserColNames()) ) {
                            _messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
                            _messages.add("Data column names for existing dataset " + datasetId);
                            _messages.add("    " + oldDataset.getUserColNames().toString());
                            _messages.add("do not match those in uploaded file " + filename);
                            _messages.add("    " + datasetData.getUserColNames());
                            _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                            continue;
                        }
                        // Update information on the existing dataset to reflect updated data
                        // leave the original owner and any archive date
                        oldDataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_NOT_CHECKED);
                        oldDataset.setSubmitStatus(DashboardUtils.STATUS_NOT_SUBMITTED);
                        oldDataset.setArchiveStatus(DashboardUtils.ARCHIVE_STATUS_NOT_SUBMITTED);
                        oldDataset.setUploadFilename(filename);
                        oldDataset.setUploadTimestamp(timestamp);
                        oldDataset.setVersion(_configStore.getUploadVersion());
                        // Add the add to the dataset
                        int rowNum = oldDataset.getNumDataRows();
                        for ( ArrayList<String> datavals : datasetData.getDataValues() ) {
                            rowNum++;
                            oldDataset.getDataValues().add(datavals);
                            oldDataset.getRowNums().add(rowNum);
                        }
                        oldDataset.setNumDataRows(rowNum);
                        // Replace the reference to the uploaded dataset with this appended dataset
                        datasetData = oldDataset;
                        appended = true;
                    }
                }
                // At this point, datasetData is the dataset to save, regardless of new, overwrite, or append
    
                try {
                    MetadataFileHandler mdataHandler = _configStore.getMetadataFileHandler();
                    mdataHandler.createEmptyOADSMetadataFile(datasetId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                // Add any existing documents for this cruise
                ArrayList<DashboardMetadata> mdataList = 
                        _configStore.getMetadataFileHandler().getMetadataFiles(datasetId);
                TreeSet<String> addlDocs = new TreeSet<String>();
                for ( DashboardMetadata mdata : mdataList ) {
                    if ( DashboardUtils.autoExtractedMdFilename(datasetId).equals(mdata.getFilename())) {
                        // Ignore the auto-extracted XML stub file
                    }
                    else if ( DashboardUtils.metadataFilename(datasetId).equals(mdata.getFilename())) {
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
                    datasetHandler.saveDatasetInfoToFile(datasetData, "Dataset info " + commitMsg);
                    datasetHandler.saveDatasetDataToFile(datasetData, "Dataset data " + commitMsg);
                } catch (IllegalArgumentException ex) {
                    _messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
                            filename + " ; " + datasetId);
                    _messages.add(ex.getMessage());
                    _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                    continue;
                }
    
                // Success
                _messages.add(DashboardUtils.SUCCESS_HEADER_TAG + " " + datasetId);
                _successes.add(datasetId);
            }
        }
        // Update the list of cruises for the user
        try {
            _configStore.getUserFileHandler().addDatasetsToListing(_successes, username);
        } catch (IllegalArgumentException ex) {
            throw new UploadProcessingException("Unexpected error updating list of datasets \n" + ex.getMessage(), ex);
        }
    }

}
