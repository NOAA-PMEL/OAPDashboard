/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.ArrayList;

import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.DsgNcFile;
import gov.noaa.pmel.dashboard.dsg.StdDataArray;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.ferret.FerretConfig;
import gov.noaa.pmel.dashboard.ferret.SocatTool;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FeatureType;


/**
 * NetCDF DSG file handler for the upload dashboard.
 * 
 * @author Karl Smith
 */
public class DsgNcFileHandler {

	private static final String DSG_FILE_SUFFIX = ".nc";

	private File dsgFilesDir;
	private File erddapDsgFlagFile;
	private FerretConfig ferretConfig;
	private KnownDataTypes knownUserDataTypes;
	private KnownDataTypes knownMetadataTypes;
	private KnownDataTypes knownDataFileTypes;
	private WatchService watcher;

	public DsgNcFileHandler(String dsgFilesDirName, String erddapDsgFlagFileName) {
	    this(dsgFilesDirName, erddapDsgFlagFileName, null, null);
	}
	public DsgNcFileHandler(String dsgFilesDirName, String erddapDsgFlagFileName, 
	                        String decDsgFilesDirName, String erddapDecDsgFlagFileName) {
		dsgFilesDir = new File(dsgFilesDirName);
		if ( ! dsgFilesDir.isDirectory() )
			throw new IllegalArgumentException(dsgFilesDirName + " is not a directory");
		erddapDsgFlagFile = new File(erddapDsgFlagFileName);
		File parentDir = erddapDsgFlagFile.getParentFile();
		if ( (parentDir == null) || ! parentDir.isDirectory() )
			throw new IllegalArgumentException("parent directory of " + 
					erddapDsgFlagFile.getPath() + " is not valid");
	}
	public DsgNcFileHandler(String dsgFilesDirName, String erddapDsgFlagFileName, 
	                        String decDsgFilesDirName, String erddapDecDsgFlagFileName,
	                        String dummy1, String dummy2, KnownDataTypes knownDataFileTypes) {
		this(dsgFilesDirName, erddapDsgFlagFileName, decDsgFilesDirName, erddapDecDsgFlagFileName);
        this.knownDataFileTypes = knownDataFileTypes;

	}
	/**
	 * Handles storage and retrieval of full and decimated NetCDF 
	 * discrete geometry files under the given directories.
	 * 
	 * @param dsgFilesDirName
	 * 		name of the directory for the full NetCDF DSG files
	 * @param decDsgFilesDirName
	 * 		name of the directory for the decimated NetCDF DSG files
	 * @param erddapDecDsgFlagFileName
	 * 		name of the flag file to create to notify ERDDAP 
	 * 		of updates to the decimated NetCDF DSG files
	 * @param ferretConf
	 * 		configuration document for running Ferret
	 * @throws IllegalArgumentException
	 * 		if the specified DSG directories, or the parent directories
	 * 		of the ERDDAP flag files, do not exist or are not directories
	 */
	public DsgNcFileHandler(String dsgFilesDirName, String erddapDsgFlagFileName, 
			FerretConfig ferretConf, KnownDataTypes knownUserDataTypes, 
			KnownDataTypes knownMetadataTypes, KnownDataTypes knownDataFileTypes) {
	    this(dsgFilesDirName, erddapDsgFlagFileName);
	    
		ferretConfig = ferretConf;
		this.knownUserDataTypes = knownUserDataTypes;
		this.knownMetadataTypes = knownMetadataTypes;
		this.knownDataFileTypes = knownDataFileTypes;

//		try {
//			Path dsgFilesDirPath = dsgFilesDir.toPath();
//			watcher = FileSystems.getDefault().newWatchService();
//			dsgFilesDirPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY).cancel();
//			watcher.close();
//			watcher = null;
//		} catch (Exception ex) {
//			throw new IllegalArgumentException("Problems creating a watcher for the DSG files directory: " + 
//					ex.getMessage(), ex);
//		}
	}

	/**
	 * Generates the full NetCDF DSG abstract file for a dataset.
	 * Creates the parent subdirectory if it does not exist.
	 * 
	 * @param datasetId
	 * 		ID of the dataset
	 * @return
	 * 		full NetCDF DSG abstract file for the dataset
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid, or
	 * 		if problems creating the parent subdirectory
	 * @throws IOException 
	 */
	public DsgNcFile getDsgNcFile(String datasetId) throws IllegalArgumentException, IOException {
        return getDsgNcFile(datasetId, true);
	}
	/**
	 * Generates the full NetCDF DSG abstract file for a dataset.
	 * This version is used when only checking for existence of the DsgFile
	 * so that the parent dir is not created unnecessarily.
	 * 
	 * @param datasetId
	 * 		ID of the dataset
     * @param createDirs
     *      Whether or not to create the parent subdirectory.
	 * @return
	 * 		full NetCDF DSG abstract file for the dataset
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid, or
	 * 		if problems creating the parent subdirectory
	 * @throws IOException 
	 */
	public DsgNcFile getDsgNcFile(String datasetId, boolean createDirs) throws IllegalArgumentException, IOException {
		// Check and standardize the dataset ID
		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
        DashboardDataset dd = DashboardConfigStore.get(false).getDataFileHandler().getDatasetFromInfoFile(stdId);
        FeatureType dsgType = dd.getFeatureType();
		// Make sure the parent directory exists
		File parentDir = new File(dsgFilesDir, stdId.substring(0,4));
		if ( parentDir.exists() ) {
			if ( ! parentDir.isDirectory() ) {
				throw new IllegalArgumentException("Parent subdirectory exists but is not a directory: " +
						parentDir.getPath());
			}
		}
		else if ( createDirs && ! parentDir.mkdirs() ) {
			throw new IllegalArgumentException("Unable to create the new subdirectory " + 
					parentDir.getPath());
		}
		return DsgNcFile.newDsgFile(dsgType, parentDir, stdId + DSG_FILE_SUFFIX); // XXX hard-coded profile type
	}

	/**
	 * Saves the DsgMetadata and data into a new full-data NetCDF DSG file.  
	 * After successful creation of the DSG file, ERDDAP will need to be notified 
	 * of changes to the DSG files.  This notification is not done in this 
	 * routine so that a single notification event can be made after multiple 
	 * modifications.
	 * 
	 * @param metadata
	 * 		metadata for the dataset
	 * @param dataset
	 * 		dataset with data to save
	 * @throws IllegalArgumentException
	 * 		if there are problems with the metadata or data given, or
	 * 		if there are problems creating or writing the full-data DSG file
	 * @throws IOException 
	 */
	public void saveDatasetDsg(DsgMetadata metadata, 
	                           StdUserDataArray stdUserData) throws IllegalArgumentException, IOException {
		// Get the location and name for the NetCDF DSG file
		DsgNcFile dsgFile = getDsgNcFile(stdUserData.getDatasetName());

		// Create the NetCDF DSG file
		try {
			dsgFile.create(metadata, stdUserData, knownDataFileTypes);
		} catch (Exception ex) {
			dsgFile.delete();
			ex.printStackTrace();
			throw new IllegalArgumentException("Problems creating the DSG file " + 
					dsgFile.getName() + "\n    " + ex.getMessage(), ex);
		}

	}

	/**
	 * Appropriately renames any DSG and decimated DSG files, if they exist, 
	 * for a change in dataset ID.  Changes the dataset ID in the DSG files.
	 * 
	 * @param oldId
	 * 		standardized old ID for the dataset
	 * @param newId
	 * 		standardized new ID for the dataset
	 * @throws IllegalArgumentException
	 * 		if a DSG or decimated DSG file for the new ID already exists, or
	 * 		if the contents of the old DSG file are invalid
	 * @throws IOException
	 * 		if unable to regenerate the DSG or decimated DSG file with the new ID
	 * 			from the data and metadata in the old DSG file, or 
	 * 		if unable to delete the old DSG or decimated DSG file
	 */
	public void renameDsgFiles(String oldId, String newId) 
			throws IllegalArgumentException, IOException {
		DsgNcFile newDsgFile = getDsgNcFile(newId);
		if ( newDsgFile.exists() )
			throw new IllegalArgumentException(
					"DSG file for " + oldId + " already exist");

		DsgNcFile oldDsgFile = getDsgNcFile(oldId);
		if ( oldDsgFile.exists() )  {
			// Just re-create the DSG file with the updated metadata
			ArrayList<String> missing = oldDsgFile.readMetadata(knownMetadataTypes);
			if ( ! missing.isEmpty() )
				throw new RuntimeException("Unexpected metadata fields missing from the DSG file: " + missing);
			missing = oldDsgFile.readData(knownDataFileTypes);
			if ( ! missing.isEmpty() )
				throw new RuntimeException("Unexpected data fields missing from the DSG file: " + missing);
			try {
				StdDataArray dataVals = oldDsgFile.getStdDataArray();
				DsgMetadata updatedMeta = oldDsgFile.getMetadata();
				updatedMeta.setDatasetId(newId);
				newDsgFile.create(updatedMeta, dataVals);
				// Call Ferret to add lon360 and tmonth (calculated data should be the same)
				SocatTool tool = new SocatTool(ferretConfig);
				ArrayList<String> scriptArgs = new ArrayList<String>(1);
				scriptArgs.add(newDsgFile.getPath());
				tool.init(scriptArgs, newId, FerretConfig.Action.COMPUTE);
				tool.run();
				if ( tool.hasError() )
					throw new IllegalArgumentException(newId + 
							": Failure adding computed variables: " + tool.getErrorMessage());
				// Delete the old DSG and decimated-data DSG files
				oldDsgFile.delete();
			} catch ( Exception ex ) {
				throw new IOException(ex);
			}

			// Tell ERDDAP there are changes
			flagErddap();
		}

	}

	/**
	 * Deletes the DSG and decimated DSG files, if they exist, for a dataset.  
	 * If a file was deleted, true is returned and ERDDAP will need to be 
	 * notified of changes to the DSG and decimated DSG files.  This notification
	 * is not done in this routine so that a single notification event can be
	 * made after multiple modifications.
	 * 
	 * @param datasetId
	 * 		delete the DSG and decimated DSG files for the dataset with this ID
	 * @return
	 * 		true if a file was deleted
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid, or 
	 * 		if unable to delete one of the files 
	 * @throws IOException 
	 */
	public boolean deleteCruise(String datasetId) throws IllegalArgumentException, IOException {
		boolean fileDeleted = false;
		File dsgFile = getDsgNcFile(datasetId, false);
		if ( dsgFile.exists() ) {
			if ( ! dsgFile.delete() )
				throw new IllegalArgumentException("Unable to delete the DSG file for " + 
						datasetId);
			fileDeleted = true;
		}
		return fileDeleted;
	}

	/**
	 * Notifies ERDDAP that content has changed in the DSG files. 
	 * 
	 * @param flagDsg
	 * 		if true, notify ERDDAP that content has changed in the full DSG files.
	 * @param flagDecDsg
	 * 		if true, notify ERDDAP that content has changed in the decimated DSG files.
	 * @return
	 * 		true if successful
	 */
	public boolean flagErddap() {
		try ( FileOutputStream touchFile = new FileOutputStream(erddapDsgFlagFile); ) {
    		return true;
		} catch (IOException ex) {
			return false;
		}
	}

	/**
	 * Reads and returns the array of data values for the specified variable
	 * contained in the DSG file for the specified dataset.  The variable must 
	 * be saved in the DSG file as characters.  Empty strings are changed to 
	 * {@link DashboardUtils#CHAR_MISSING_VALUE}.  For some variables, the  
	 * DSG file must have been processed by Ferret, such as when saved using 
	 * {@link DsgNcFileHandler#saveDataset(OmeMetadata, DashboardDatasetData, String)}
	 * for the data values to be meaningful.
	 * 
	 * @param datasetId
	 * 		get the data values for the dataset with this ID
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws FileNotFoundException
	 * 		if the full-data DSG file does not exist
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid, or
	 * 		if the variable is not a single-character array variable
	 */
	public char[] readCharVarDataValues(String datasetId, String varName) 
			throws IllegalArgumentException, FileNotFoundException, IOException {
		DsgNcFile dsgFile = getDsgNcFile(datasetId);
		if ( ! dsgFile.exists() )
			throw new FileNotFoundException("Full data DSG file for " + 
					datasetId + " does not exist");
		return dsgFile.readCharVarDataValues(varName);
	}

	/**
	 * Reads and returns the array of data values for the specified variable
	 * contained in the DSG file for the specified dataset.  The variable must 
	 * be saved in the DSG file as integers.  For some variables, the DSG file 
	 * must have been processed by Ferret, such as when saved using 
	 * {@link DsgNcFileHandler#saveDataset(OmeMetadata, DashboardDatasetData, String)}
	 * for the data values to be meaningful.
	 * 
	 * @param datasetId
	 * 		get the data values for the dataset with this ID
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws FileNotFoundException
	 * 		if the full-data DSG file does not exist
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid
	 */
	public int[] readIntVarDataValues(String datasetId, String varName) 
			throws IllegalArgumentException, FileNotFoundException, IOException {
		DsgNcFile dsgFile = getDsgNcFile(datasetId);
		if ( ! dsgFile.exists() )
			throw new FileNotFoundException("Full data DSG file for " + 
					datasetId + " does not exist");
		return dsgFile.readIntVarDataValues(varName);
	}

	/**
	 * Reads and returns the array of data values for the specified variable
	 * contained in the full-data DSG file for the specified dataset.  The 
	 * variable must be saved in the DSG file as doubles.  NaN and infinite 
	 * values are changed to {@link DashboardUtils#FP_MISSING_VALUE}.  For 
	 * some variables, the DSG file must have been processed by Ferret, such 
	 * as when saved using {@link DsgNcFileHandler#saveDataset(OmeMetadata, 
	 * DashboardDatasetData, String)} for the data values to be meaningful.
	 * 
	 * @param datasetId
	 * 		get the data values for the dataset with this ID
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws FileNotFoundException
	 * 		if the full-data DSG file does not exist
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid
	 */
	public double[] readDoubleVarDataValues(String datasetId, String varName) 
			throws IllegalArgumentException, FileNotFoundException, IOException {
		DsgNcFile dsgFile = getDsgNcFile(datasetId);
		if ( ! dsgFile.exists() )
			throw new FileNotFoundException("Full data DSG file for " + 
					datasetId + " does not exist");
		return dsgFile.readDoubleVarDataValues(varName);
	}

}
