/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import gov.noaa.pmel.dashboard.actions.DatasetChecker;
import gov.noaa.pmel.dashboard.actions.DatasetModifier;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.handlers.PreviewPlotsHandler;
import gov.noaa.pmel.dashboard.handlers.UserFileHandler;
import gov.noaa.pmel.dashboard.oads.DashboardOADSMetadata;
import gov.noaa.pmel.dashboard.oads.OADSMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetList;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardServicesInterface;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.FileInfo;
import gov.noaa.pmel.dashboard.shared.MetadataPreviewInfo;
import gov.noaa.pmel.dashboard.shared.NotFoundException;
import gov.noaa.pmel.dashboard.shared.PreviewPlotImage;
import gov.noaa.pmel.dashboard.shared.PreviewPlotResponse;
import gov.noaa.pmel.dashboard.shared.SessionException;
import gov.noaa.pmel.dashboard.shared.ADCMessageList;
import gov.noaa.pmel.dashboard.shared.TypesDatasetDataPair;
import gov.noaa.pmel.dashboard.util.xml.XmlUtils;

/**
 * Implementation of DashboardServicesInterface
 * 
 * @author Karl Smith
 */
public class DashboardServices extends RemoteServiceServlet implements DashboardServicesInterface {

	private static final long serialVersionUID = -8189933983319827049L;

    private static DashboardConfigStore store;
    static {
        try {
            store = DashboardConfigStore.get(true);
            logger = LogManager.getLogger(DashboardServices.class);
        } catch (IOException iex) {
            iex.printStackTrace();
            System.exit(-42);
        }
    }
    
	// To get config debug set: -Dorg.apache.logging.log4j.simplelog.StatusLogger.level=TRACE
	private static Logger logger; //  = LogManager.getLogger(DashboardServices.class);
	
	private String username = null;
	private DashboardConfigStore configStore = null;

	@Override
	public void logoutUser() {
		HttpServletRequest request = getThreadLocalRequest();
		username = null;
		try {
			username = DashboardUtils.cleanUsername(request.getUserPrincipal().getName().trim());
		} catch (Exception ex) {
			// Probably null pointer exception - leave username null
		}
		logger.info("logging out user " + username);
		try {
			request.logout();
		} catch ( Exception ex ) {
			logger.error("request.logout failed: " + ex);
		}
		HttpSession session = request.getSession(false);
		try {
			session.invalidate();
		} catch ( Exception ex ) {
			// Log but otherwise ignore this error
			logger.error("session.invalidate failed: " + ex.getMessage());
		}
		logger.info("logged out " + username);
	}

	/**
	 * Validates the given request by retrieving the current username from the request
	 * and verifying that username with the Dashboard data store.  If pageUsername is
	 * given, also checks these usernames are the same.
	 * Assigns the username and configStore fields in this instance.
	 * 
	 * @param pageUsername
	 * 		if not null, check that this matches the current page username
	 * @return
	 * 		true if the request obtained a valid username; otherwise false
	 * @throws IllegalArgumentException
	 * 		if unable to obtain the dashboard data store
	 */
	private boolean validateRequest(String pageUsername) throws IllegalArgumentException {
		username = null;
		HttpServletRequest request = getThreadLocalRequest();
		try {
			username = DashboardUtils.cleanUsername(request.getUserPrincipal().getName().trim());
		} catch (Exception ex) {
			// Probably null pointer exception
			return false;
		}
		if ( (pageUsername != null) && ! pageUsername.equals(username) )
			return false;

        // Really ?
		configStore = null;
		try {
			configStore = DashboardConfigStore.get(true);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unexpected configuration error: " + ex.getMessage());
		}
        // XXX TODO: No longer using ConfigStore to validate Users.
//		return configStore.validateUser(username);
        return true;
	}

	@Override
	public DashboardDatasetList getDatasetList(String pageUsername) throws IllegalArgumentException, SessionException {
		// Get the dashboard data store and current username
        logger.debug(pageUsername);
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");
		DashboardDatasetList datasetList = configStore.getUserFileHandler().getDatasetListing(username);
		logger.info("dataset list returned for " + username);
		return datasetList;
	}

	@Override
	public DashboardDatasetList deleteDatasets(String pageUsername, TreeSet<String> idsSet, 
			Boolean deleteMetadata) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		DataFileHandler dataHandler = configStore.getDataFileHandler();
		// Delete each of the datesets in the given set
		for ( String datasetId : idsSet ) {
			dataHandler.deleteDatasetFiles(datasetId, username, deleteMetadata);
			// IllegalArgumentException for other problems escape as-is
			logger.info("dataset " + datasetId + " deleted by " + username);
		}

		// Return the current list of datasets, which should 
		// detect the missing datasets and update itself
		DashboardDatasetList datasetList = configStore.getUserFileHandler().getDatasetListing(username);
		logger.info("dataset list returned for " + username);
		return datasetList;
	}

	@Override
	public DashboardDatasetList addDatasetsToList(String pageUsername, 
			String wildDatasetId) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		// Add the datasets to the user's list and return the updated list
		DashboardDatasetList cruiseList = configStore.getUserFileHandler()
				.addDatasetsToListing(wildDatasetId, username);
		logger.info("added datasets " + wildDatasetId + " for " + username);
		return cruiseList;
	}

	@Override
	public DashboardDatasetList removeDatasetsFromList(String pageUsername,
			TreeSet<String> idsSet) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		// Remove the datasets from the user's list and return the updated list
		DashboardDatasetList datasetList = configStore.getUserFileHandler()
				.removeDatasetsFromListing(idsSet, username);
		logger.info("removed datasets " + idsSet.toString() + " for " + username);
		return datasetList;
	}

	@Override
	public DashboardDatasetList changeDatasetOwner(String pageUsername, 
			TreeSet<String> idsSet, String newOwner) throws IllegalArgumentException {
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");
		// Get the dashboard username of the new owner
		String newUsername;
		if ( configStore.validateUser(newOwner) ) {
			// dashboard username was given
			newUsername = newOwner;
		}
		else {
			// actual name given?
			try {
				newUsername = configStore.getDatabaseRequestHandler().getReviewerUsername(newOwner);
			} catch (Exception ex) {
				newUsername = null;
			}
			if ( (newUsername == null) || ! configStore.validateUser(newUsername) ) 
				throw new IllegalArgumentException("Unknown dashboard user " + newOwner);
		}
		// Change the owner of the datasets
		DatasetModifier modifier = new DatasetModifier(configStore);
		for ( String datasetId : idsSet ) {
			modifier.changeDatasetOwner(datasetId, newUsername);
			logger.info("changed owner of " + datasetId + " to " + newUsername);
		}
		// Return the updated list of cruises for this user
		DashboardDatasetList datasetList = configStore.getUserFileHandler().getDatasetListing(pageUsername);
		return datasetList;
	}

	@Override
	public DashboardDatasetList getUpdatedDatasets(String pageUsername, 
			TreeSet<String> idsSet) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		// Create the set of updated dataset information to return
		DataFileHandler dataHandler = configStore.getDataFileHandler();
		DashboardDatasetList datasetList = new DashboardDatasetList();
		datasetList.setUsername(username);
		datasetList.setManager(configStore.isManager(username));
		for ( String datasetId : idsSet ) {
			datasetList.put(datasetId, dataHandler.getDatasetFromInfoFile(datasetId));
		}
		logger.info("returned updated dataset information for " + username);
		return datasetList;
	}

	@Override
	public DashboardDatasetList deleteAddlDoc(String pageUsername, String deleteFilename, 
			String datasetId, TreeSet<String> allIds) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		DataFileHandler dataHandler = configStore.getDataFileHandler();
		DashboardDataset dataset = dataHandler.getDatasetFromInfoFile(datasetId);

		// Get the current metadata documents for the cruise
		MetadataFileHandler mdataHandler = configStore.getMetadataFileHandler();
		// XXX TODO: OME_FILENAME check
		if ( DashboardUtils.metadataFilename(datasetId).equals(deleteFilename) ) {
			// Remove the OME XML stub file
			if ( ! Boolean.TRUE.equals(dataset.isEditable()) ) 
				throw new IllegalArgumentException("Cannot delete the metadata for a submitted dataset");
		}
//		else if ( DashboardUtils.PI_OME_FILENAME.equals(deleteFilename) ) {
//			// No more PI-provided OME metadata for this cruise
//			dataset.setMdTimestamp(null);
//		}
		else {
			// Directly modify the additional documents list in this dataset
			TreeSet<String> addlDocs = dataset.getAddlDocs();
			// Find this additional document for this cruise
			String titleToRemove = null;
			for ( String docTitle : addlDocs ) {
				String name = DashboardMetadata.splitAddlDocsTitle(docTitle)[0];
				if ( name.equals(deleteFilename) ) {
					titleToRemove = docTitle;
					break;
				}
			}
			if ( (titleToRemove == null) || ! addlDocs.remove(titleToRemove) )
				throw new IllegalArgumentException("Document " + deleteFilename + 
						" is not associated with dataset " + datasetId);
		}
		// Delete this OME metadata or additional documents file on the server
		mdataHandler.deleteMetadata(username, datasetId, deleteFilename);

		logger.info("deleted metadata " + deleteFilename + 
				" from " + datasetId + " for " + username);

		// Save the updated cruise
		dataHandler.saveDatasetInfoToFile(dataset, "Removed metadata document " + 
									deleteFilename + " from dataset " + datasetId);

		// Create the set of updated dataset information to return
		DashboardDatasetList datasetList = new DashboardDatasetList();
		datasetList.setUsername(username);
		datasetList.setManager(configStore.isManager(username));
		for ( String id : allIds ) {
			datasetList.put(id, dataHandler.getDatasetFromInfoFile(id));
		}
		logger.info("returned updated dataset information for " + username);
		return datasetList;
	}

	@Override
	public TypesDatasetDataPair getDataColumnSpecs(String pageUsername,
			String datasetId) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		// Get the list of known user-provided data column types
		KnownDataTypes knownUserTypes = configStore.getKnownUserDataTypes();
		if ( knownUserTypes == null )
			throw new IllegalArgumentException("unexpected missing list of all known data column types");
		TreeSet<DashDataType<?>> knownTypesSet = knownUserTypes.getKnownTypesSet();
		if ( knownTypesSet.isEmpty() )
			throw new IllegalArgumentException("unexpected empty list of all known data column types");
		ArrayList<DataColumnType> knownTypesList = new ArrayList<DataColumnType>(knownTypesSet.size());
		for ( DashDataType<?> dtype : knownTypesSet )
			knownTypesList.add(dtype.duplicate());

		// Get the cruise with the first maximum-needed number of rows
		DashboardDatasetData dataset = configStore.getDataFileHandler()
				.getDatasetDataFromFiles(datasetId, 0, DashboardUtils.MAX_ROWS_PER_GRID_PAGE);
		if ( dataset == null )
			throw new IllegalArgumentException(datasetId + " does not exist");

		TypesDatasetDataPair typesAndDataset = new TypesDatasetDataPair();
		typesAndDataset.setAllKnownTypes(knownTypesList);
		typesAndDataset.setDatasetData(dataset);

		// So we can show the error message on the page.
		ADCMessageList msgs = getDataMessages(pageUsername, datasetId);
		if ( msgs != null ) {
			typesAndDataset.setMsgList(msgs);
		}
		logger.info("data columns specs returned for " + 
				datasetId + " for " + username);
		// Return the cruise with the partial data
		return typesAndDataset;
	}

	@Override
	public ArrayList<ArrayList<String>> getDataWithRowNum(String pageUsername, 
			String datasetId, int firstRow, int numRows) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		int myFirstRow = firstRow;
		if ( myFirstRow < 0 )
			myFirstRow = 0;
		// Get only the desired data from the dataset
		DashboardDatasetData dataset = configStore.getDataFileHandler()
									.getDatasetDataFromFiles(datasetId, myFirstRow, numRows);
		if ( dataset == null )
			throw new IllegalArgumentException(datasetId + " does not exist");
		ArrayList<ArrayList<String>> dataWithRowNums = dataset.getDataValues();
		ArrayList<Integer> rowNums = dataset.getRowNums();
		// Modify the list in this DashboardDatasetData since it is then thrown away
		int k = 0;
		for ( ArrayList<String> rowData : dataWithRowNums ) {
			rowData.add(0, rowNums.get(k).toString());
			k++;
		}
		int myLastRow = myFirstRow + dataWithRowNums.size() - 1;
		logger.info(datasetId + " dataset data [" + Integer.toString(myFirstRow) + 
				" - " + Integer.toString(myLastRow) + "] returned for " + username);
		if ( logger.isDebugEnabled() ) {
			for (k = 0; k < dataWithRowNums.size(); k++) {
				logger.debug("  data[" + Integer.toString(k) + "]=" + dataWithRowNums.get(k).toString());
			}
		}
		return dataWithRowNums;
	}

	@Override
	public DashboardDatasetData saveDataColumnSpecs(String pageUsername, DashboardDataset newSpecs)
			throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		// Retrieve all the current cruise data
		DashboardDatasetData dataset = configStore.getDataFileHandler()
						.getDatasetDataFromFiles(newSpecs.getDatasetId(), 0, -1);
		if ( ! dataset.isEditable() )
			throw new IllegalArgumentException(newSpecs.getDatasetId() + 
					" has been submitted for QC; data column types cannot be modified.");

		// Revise the data column types and units 
		if ( newSpecs.getDataColTypes().size() != dataset.getDataColTypes().size() )
			throw new IllegalArgumentException("Unexpected number of data columns (" +
					newSpecs.getDataColTypes().size() + " instead of " + 
					dataset.getDataColTypes().size());
		dataset.setDataColTypes(newSpecs.getDataColTypes());

		// Save and commit the updated data columns
		configStore.getDataFileHandler().saveDatasetInfoToFile(dataset, 
				"Data column types, units, and missing values for " + 
				dataset.getDatasetId() + " updated by " + username);
		// Update the user-specific data column names to types, units, and missing values 
		configStore.getUserFileHandler().updateUserDataColumnTypes(dataset, username);
		if ( ! username.equals(dataset.getOwner()) )
			configStore.getUserFileHandler().updateUserDataColumnTypes(dataset, dataset.getOwner());
		
		logger.info("data columns specs saved for " + 
				dataset.getDatasetId() + " by " + username);
		
		return dataset;
	}
	
	@Override
	public TypesDatasetDataPair updateDataColumnSpecs(String pageUsername,
			DashboardDataset newSpecs) throws IllegalArgumentException {
		TypesDatasetDataPair tddp = new TypesDatasetDataPair();
		
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		String datasetId = newSpecs.getDatasetId();
		
		// Retrieve all the current cruise data
		DashboardDatasetData dataset = configStore.getDataFileHandler()
						.getDatasetDataFromFiles(datasetId, 0, -1);
		if ( ! dataset.isEditable() )
			throw new IllegalArgumentException(datasetId + 
					" has been submitted for QC; data column types cannot be modified.");

		// Revise the data column types and units 
		if ( newSpecs.getDataColTypes().size() != dataset.getDataColTypes().size() )
			throw new IllegalArgumentException("Unexpected number of data columns (" +
					newSpecs.getDataColTypes().size() + " instead of " + 
					dataset.getDataColTypes().size());
		dataset.setDataColTypes(newSpecs.getDataColTypes());

		// Run the automated data checker with the updated data types.
		// Assigns the data check status and the WOCE-3 and WOCE-4 flags.
		DatasetChecker checker = configStore.getDashboardDatasetChecker(dataset.getFeatureType());
		StdUserDataArray stdArray = checker.standardizeDataset(dataset, null);

		// Save and commit the updated data columns
		configStore.getDataFileHandler().saveDatasetInfoToFile(dataset, 
				"Data column types, units, and missing values for " + 
				datasetId + " updated by " + username);
		// Update the user-specific data column names to types, units, and missing values 
		configStore.getUserFileHandler().updateUserDataColumnTypes(dataset, username);
		
		if ( ! stdArray.hasCriticalError()) {
			DashboardOADSMetadata mdata = OADSMetadata.extractOADSMetadata(stdArray);
			configStore.getMetadataFileHandler().saveAsOadsXmlDoc(mdata, 
			                                                      DashboardUtils.autoExtractedMdFilename(datasetId), 
			                                                      "Initial Auto-extraction");
		}
		
		// ??? Is this possible at this point for a user to be editing another user's dataset ?
		if ( ! username.equals(dataset.getOwner()) )
			configStore.getUserFileHandler().updateUserDataColumnTypes(dataset, dataset.getOwner());
		
		// Remove all but the first maximum-needed number of rows of cruise data 
		// to minimize the payload of the returned cruise data
		int numRows = dataset.getNumDataRows();
		if ( numRows > DashboardUtils.MAX_ROWS_PER_GRID_PAGE ) {
			dataset.getDataValues()
				   .subList(DashboardUtils.MAX_ROWS_PER_GRID_PAGE, numRows)
				   .clear();
			dataset.getRowNums()
				   .subList(DashboardUtils.MAX_ROWS_PER_GRID_PAGE, numRows)
				   .clear();
		}

		logger.info("data columns specs updated for " + 
				datasetId + " by " + username);
		// Return the updated truncated cruise data for redisplay 
		// in the DataColumnSpecsPage
		tddp.setDatasetData(dataset);
		
		// So we can show the error message on the page.
		// XXX TODO: ??? can't we get these from the stdArray ?
		ADCMessageList msgs = getDataMessages(pageUsername, datasetId);
		if ( msgs != null ) {
			tddp.setMsgList(msgs);
		}
		return tddp;
	}

	@Override
	public void updateDataColumns(String pageUsername, 
			ArrayList<String> idsList) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		DataFileHandler dataHandler = configStore.getDataFileHandler();
		UserFileHandler userHandler = configStore.getUserFileHandler();
		Logger dataSpecsLogger = logger;

		for ( String datasetId : idsList ) {
			// Retrieve all the current data
			DashboardDatasetData dataset = dataHandler.getDatasetDataFromFiles(datasetId, 0, -1);
			if ( ! dataset.isEditable() )
				throw new IllegalArgumentException("Dataset " + datasetId + 
						" has been submitted for QC; data column types cannot be modified.");

    		DatasetChecker datasetChecker = configStore.getDashboardDatasetChecker(dataset.getFeatureType());
			try {
				// Identify the columns from stored names-to-types for this user
				userHandler.assignDataColumnTypes(dataset);
				// Save and commit these column assignments in case the sanity checker has problems
				dataHandler.saveDatasetInfoToFile(dataset, "Column types for " + datasetId + 
						" updated by " + username + " from post-processing a multiple-dataset upload");
			
				// Run the automated data checker with the updated data types.  Saves the messages,
				// and assigns the data check status and the WOCE-3 and WOCE-4 flags.
				datasetChecker.standardizeDataset(dataset, null);

				// Save and commit the updated dataset information
				dataHandler.saveDatasetInfoToFile(dataset, "Automated data check status and flags for " + 
						datasetId + " updated by " + username + " from post-processing a multiple-dataset upload");
				dataSpecsLogger.info("Updated data column specs for " + datasetId + " for " + username);
			} catch (Exception ex) {
				// ignore problems (such as unidentified columns) - cruise will not have been updated
				dataSpecsLogger.error("Unable to update data column specs for " + datasetId + ": " + ex.getMessage());
				continue;
			}
		}
	}

	@Override
	public ADCMessageList getDataMessages(String pageUsername, 
			String datasetId) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		// Get the list of saved automated data checker messages for this dataset
		ADCMessageList scMsgList;
		try {
			scMsgList = configStore.getCheckerMsgHandler().getCheckerMessages(datasetId);
		} catch (Exception ex) {
			logger.info("No messages for dataset id " + datasetId);
			scMsgList = new ADCMessageList();
//			throw new IllegalArgumentException("The automated data checker has not been run on dataset " + datasetId);
		}
		scMsgList.setUsername(username);
		logger.info("returned automated data checker messages for " + datasetId + " for " + username);
		return scMsgList;
	}

	// XXX NOT USED
	// XXX TODO: OME_FILENAME check
//	@Override
//	public String getOmeXmlPath(String pageUsername, String datasetId, 
//			String previousId) throws IllegalArgumentException {
//		// Get the dashboard data store and current username, and validate that username
//		if ( ! validateRequest(pageUsername) ) 
//			throw new IllegalArgumentException("Invalid user request");
//		MetadataFileHandler metadataHandler = configStore.getMetadataFileHandler();
//
//		if ( ! previousId.isEmpty() ) {
//			// Read the OME XML contents for the previous dataset 
//			// XXX TODO: OME_FILENAME check
//			DashboardMetadata mdata = metadataHandler.getMetadataInfo(previousId, DashboardUtils.OME_FILENAME);
//			DashboardOmeMetadata updatedOmeMData = new DashboardOmeMetadata(mdata, metadataHandler);
//			// Reset the ID and related fields to that for this dataset
//			updatedOmeMData.changeDatasetID(datasetId);
//			// Read the OME XML contents currently saved for activeExpocode
//			mdata = metadataHandler.getMetadataInfo(datasetId, DashboardUtils.OME_FILENAME);
//			DashboardOmeMetadata origOmeMData = new DashboardOmeMetadata(mdata, metadataHandler);
//			// Create the merged OME and save the results
//			DashboardOmeMetadata mergedOmeMData = origOmeMData.mergeModifiable(updatedOmeMData);
//			metadataHandler.saveAsOmeXmlDoc(mergedOmeMData, "Merged OME of " + previousId + 
//															" into OME of " + datasetId);
//		}
//
//		// return the absolute path to the OME.xml for activeExpcode
//		File omeFile = metadataHandler.getMetadataFile(datasetId, DashboardUtils.OME_FILENAME);
//		return omeFile.getAbsolutePath();
//	}
	
	@Override
	public MetadataPreviewInfo getMetadataPreviewInfo(String pageUsername, String datasetId)
		throws NotFoundException, IllegalArgumentException {
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");
		try {
			MetadataPreviewInfo preview = new MetadataPreviewInfo();
			String xml = null;
			File mdFile = OADSMetadata.getMetadataFile(datasetId);
			if ( !mdFile.exists()) {
				mdFile = OADSMetadata.getExtractedMetadataFile(datasetId);
			}
			if ( !mdFile.exists()) {
				throw new FileNotFoundException("No metadata file found for " + datasetId);
			}
			Date fileModTime = new Date(mdFile.lastModified());
			Path fPath = mdFile.toPath();
			BasicFileAttributes attr = Files.getFileAttributeView(fPath, BasicFileAttributeView.class).readAttributes();
			Date fileCreateTime = new Date(attr.creationTime().toMillis());
			FileInfo mdFileInfo = new FileInfo(mdFile.getName(), fileModTime, fileCreateTime, mdFile.length());
			preview.setMetadataFileInfo(mdFileInfo);
			xml = OADSMetadata.getMetadataXml(datasetId, mdFile);
			String html = XmlUtils.asHtml(xml);
			preview.setMetadataPreview(html);
			return preview;
		} catch (FileNotFoundException ex) {
			throw new NotFoundException(ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	@Override
	public String sendMetadataInfo(String pageUsername, String datasetId)
		throws NotFoundException, IllegalArgumentException {
        String docId = null;
        try {
			File mdFile = OADSMetadata.getMetadataFile(datasetId);
			if ( !mdFile.exists()) {
				mdFile = OADSMetadata.getExtractedMetadataFile(datasetId);
			} 
			if ( !mdFile.exists()) {
                mdFile = OADSMetadata.createEmptyOADSMetadataFile(datasetId);
			}
            // XXX HttpClient and stuff coming (currently) from netcdfAll jar 
            @SuppressWarnings("resource")
            HttpClient client = HttpClients.createDefault();
            String metadataEditorPostEndpoint = getMetadataPostPoint(datasetId);
            HttpPost post = new HttpPost(metadataEditorPostEndpoint);
            FileBody body = new FileBody(mdFile, ContentType.APPLICATION_XML);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addPart("xmlFile", body);
            String notifyUrl = getNotificationUrl(getThreadLocalRequest().getRequestURL().toString(), datasetId);
            StringBody notificationUrl = new StringBody(notifyUrl, ContentType.MULTIPART_FORM_DATA);
            builder.addPart("notificationUrl", notificationUrl);
            HttpEntity postit = builder.build();
            post.setEntity(postit);
            HttpResponse response = client.execute(post);
            HttpEntity responseEntity = response.getEntity();
            byte[] bbuf = new byte[4096];
            int read = responseEntity.getContent().read(bbuf);
            StatusLine statLine = response.getStatusLine();
            int responseStatus = statLine.getStatusCode();
            if ( responseStatus != HttpServletResponse.SC_OK ) {
                String msg = new String(bbuf);
                throw new IllegalArgumentException(msg);
            }
            docId = new String(bbuf);
            return getMetadataEditorPage(docId);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
	}
    
    // request URL is in the form: http://matisse:8080/OAPUploadDashboard/OAPUploadDashboard/DashboardServices
    private static String getNotificationUrl(String requestUrl, String datasetId) {
        String notifyUrl = requestUrl.substring(0, requestUrl.lastIndexOf("OAPUploadDashboard"));
        notifyUrl = notifyUrl + "DashboardUpdateService/notify/"+datasetId;
        return notifyUrl;
    }

    private String getMetadataEditorPage(String docId) throws IOException {
		HttpServletRequest request = getThreadLocalRequest();
        String server = request.getServerName();
        String url = DashboardConfigStore.get().getProperty(DashboardConfigStore.METADATA_EDITOR_URL+"."+server);
        return url + "?id="+docId;
    }
    
    private String getMetadataPostPoint(String datasetId) throws IOException {
        // "http://matisse:8383/oap/document/postit/<datasetId>";
		HttpServletRequest request = getThreadLocalRequest();
        String server = request.getServerName();
        String url = DashboardConfigStore.get().getProperty(DashboardConfigStore.METADATA_EDITOR_POST_ENDPOINT+"."+server);
        String slash = url.endsWith("/") ? "" : "/";
        return url + slash + datasetId;
    }

    @Override
	public PreviewPlotResponse buildPreviewImages(String pageUsername, String datasetId, 
												  String timetag, boolean force) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		logger.debug(datasetId+" @ " + timetag + " force:"+ force);
		
		boolean generatePlots = force || checkNeedToGeneratePlots(datasetId);
		
		// Generate the preview plots for this dataset
		// TODO: refactor so starts this in a separate thread when firstCall is true and 
		//       returns false, then when gets called again with firstCall is false for
		//       a status update, returns false if still working and true if all plots are done
		if ( generatePlots ) {
			configStore.getPreviewPlotsHandler().createPreviewPlots(datasetId, timetag);
		}
		List<List<PreviewPlotImage>> plots = configStore.getPreviewPlotsHandler().getPreviewPlots(datasetId);
		return new PreviewPlotResponse(plots, true);
	}

	private boolean checkNeedToGeneratePlots(final String datasetId) {
		PreviewPlotsHandler pph = configStore.getPreviewPlotsHandler();
		File plotsDir = pph.getDatasetPreviewPlotsDir(datasetId);
		if ( ! plotsDir.exists()) { 
			return true; 
		}
		File[] plots = plotsDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(datasetId);
			}
		});
		if ( plots == null || plots.length < 10 ) { 
			return true; 
		}
		long plotsTime = plotsDir.lastModified();
		DataFileHandler dfh = configStore.getDataFileHandler();
		File dataFile = dfh.datasetDataFile(datasetId);
		long dataTime = dataFile.lastModified();
		if ( dataTime > plotsTime ) {
			return true;
		}
		File dataInfoFile = configStore.getDataFileHandler().datasetInfoFile(datasetId);
		long infoTime = dataInfoFile.lastModified();
		if ( infoTime > plotsTime ) {
			return true;
		}
		
		return false;
	}

	@Override
	public void submitDatasetsForQC(String pageUsername, HashSet<String> idsSet, String archiveStatus, 
			String timestamp, boolean repeatSend) throws IllegalArgumentException {
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		// Submit the datasets for QC and possibly send to be archived
		configStore.getDashboardDatasetSubmitter().submitDatasetsForQC(idsSet, 
				archiveStatus, timestamp, repeatSend, username);
		logger.info("datasets " + idsSet.toString() + 
				" submitted by " + username);
	}

	@Override
	public void submitDatasetsToArchive(String pageUsername, List<String> datasetIds, List<String> columnsList, 
	                                    String archiveStatus, String timestamp, boolean repeatSend) 
        throws IllegalArgumentException {
    	// Get the dashboard data store and current username, and validate that username
    	if ( ! validateRequest(pageUsername) ) 
    		throw new IllegalArgumentException("Invalid user request");
    
    	logger.info("archiving datasets " + datasetIds.toString() + 
    			" submitted by " + username);
    	
    	// Submit the datasets to Archive
    	configStore.getDashboardDatasetSubmitter().archiveDatasets(datasetIds, columnsList,
    															   archiveStatus, timestamp, 
    															   repeatSend, username);
    }
	
	@Override
	public void suspendDatasets(String pageUsername, Set<String> idsSet, String timestamp) throws IllegalArgumentException {
		logger.debug("Suspending dataset ids " + idsSet);
		// Get the dashboard data store and current username, and validate that username
		if ( ! validateRequest(pageUsername) ) 
			throw new IllegalArgumentException("Invalid user request");

		for (String datasetId : idsSet) {
			String message = "Suspending dataset " + datasetId;
			DataFileHandler df = configStore.getDataFileHandler();
			DashboardDataset ds = df.getDatasetFromInfoFile(datasetId);
			ds.setSubmitStatus(DashboardUtils.STATUS_SUSPENDED);
			df.saveDatasetInfoToFile(ds, message);
		}
	}
}
