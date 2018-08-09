/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.util.FormUtils;

/**
 * @author kamb
 *
 */
public class TrajectoryUploadProcessor extends FileUploadProcessor {

    public TrajectoryUploadProcessor(StandardUploadFields uploadFields) {
        super(uploadFields);
    }

    @Override
    public void doFeatureSpecificProcessing(List<FileItem> datafiles) throws UploadProcessingException {
        String action = uploadFields.dataAction();
        String username = uploadFields.username();
        String timestamp = uploadFields.timestamp();
        String encoding = uploadFields.fileDataEncoding();
        String dataFormat = FormUtils.getFormField("dataformat", uploadFields.parameterMap());
        String datasetIdColName = FormUtils.getFormField("datasetIdColName", uploadFields.parameterMap());
        DataFileHandler datasetHandler = configStore.getDataFileHandler();
        TreeSet<String> successes = new TreeSet<String>();
        
        for ( FileItem item : datafiles ) {
            // Get the datasets from this file
            TreeMap<String,DashboardDatasetData> datasetsMap = null;
            String filename = item.getName();
            
            try ( BufferedReader cruiseReader = new BufferedReader( new InputStreamReader(item.getInputStream(), encoding)); ) {
                datasetsMap = datasetHandler.createDatasetsFromInput(cruiseReader, dataFormat, username, filename, timestamp, datasetIdColName);
            } catch (Exception ex) {
                // Mark as a failed file, and go on to the next
                messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
                messages.add(ex.getMessage());
                messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                item.delete();
                continue;
            }

            try {
                saveRawFile(item);
            } catch (Exception ex) {
                // TODO: log error, notify admin?
                ex.printStackTrace();
            }
            
            // done with the uploaded data file
            item.delete();

            // Process all the datasets created from this file
            String datasetId = null;
            for ( DashboardDatasetData datasetData : datasetsMap.values() ) {
                datasetData.setFeatureType(FeatureType.TRAJECTORY.name());
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
                        messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                filename + " ; " + datasetId + " ; " + owner + " ; " + status);
                        continue;
                    }
                    // Make sure this user has permission to modify this dataset
                    try {
                        datasetHandler.verifyOkayToDeleteDataset(datasetId, username);
                    } catch ( Exception ex ) {
                        messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                filename + " ; " + datasetId + " ; " + owner + " ; " + status);
                        continue;
                    }
                    if ( DashboardUtils.APPEND_DATASETS_REQUEST_TAG.equals(action) ) {
                        // Get all the data from the existing dataset
                        DashboardDatasetData oldDataset;
                        try {
                            oldDataset = datasetHandler.getDatasetDataFromFiles(datasetId, 0, -1);
                        } catch ( Exception ex ) {
                            messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
                                    filename + " ; " + datasetId);
                            messages.add(ex.getMessage());
                            messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                            continue;
                        }
                        // If append to dataset, at this time insist on the column names being the same
                        if ( ! datasetData.getUserColNames().equals(oldDataset.getUserColNames()) ) {
                            messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
                            messages.add("Data column names for existing dataset " + datasetId);
                            messages.add("    " + oldDataset.getUserColNames().toString());
                            messages.add("do not match those in uploaded file " + filename);
                            messages.add("    " + datasetData.getUserColNames());
                            messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                            continue;
                        }
                        // Update information on the existing dataset to reflect updated data
                        // leave the original owner and any archive date
                        oldDataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_NOT_CHECKED);
                        oldDataset.setSubmitStatus(DashboardUtils.STATUS_NOT_SUBMITTED);
                        oldDataset.setArchiveStatus(DashboardUtils.ARCHIVE_STATUS_NOT_SUBMITTED);
                        oldDataset.setUploadFilename(filename);
                        oldDataset.setUploadTimestamp(timestamp);
                        oldDataset.setVersion(configStore.getUploadVersion());
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
    
//              // Create the OME XML stub file for this dataset
//              try {
//                  OmeMetadata omeMData = new OmeMetadata(datasetId);
//                  DashboardOmeMetadata mdata = new DashboardOmeMetadata(omeMData,
//                          timestamp, username, datasetData.getVersion());
//                  String msg = "New OME XML document from data file for " + 
//                          datasetId + " uploaded by " + username;
//                  MetadataFileHandler mdataHandler = configStore.getMetadataFileHandler();
//                  mdataHandler.saveMetadataInfo(mdata, msg, false);
//                  mdataHandler.saveAsOmeXmlDoc(mdata, msg);
//              } catch (Exception ex) {
//                  // should not happen
//                  messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
//                          filename + " ; " + datasetId);
//                  messages.add(ex.getMessage());
//                  messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
//                  continue;
//              }
    
                // Add any existing documents for this cruise
                ArrayList<DashboardMetadata> mdataList = 
                        configStore.getMetadataFileHandler().getMetadataFiles(datasetId);
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
                    messages.add(DashboardUtils.UNEXPECTED_FAILURE_HEADER_TAG + " " + 
                            filename + " ; " + datasetId);
                    messages.add(ex.getMessage());
                    messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                    continue;
                }
    
                // Success
                messages.add(DashboardUtils.SUCCESS_HEADER_TAG + " " + datasetId);
                successes.add(datasetId);
            }
        }
        // Update the list of cruises for the user
        try {
            configStore.getUserFileHandler().addDatasetsToListing(successes, username);
        } catch (IllegalArgumentException ex) {
            throw new UploadProcessingException("Unexpected error updating list of datasets \n" + ex.getMessage(), ex);
        }
    }

}
