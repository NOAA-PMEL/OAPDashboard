/**
 * 
 */
package gov.noaa.pmel.dashboard.actions;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.loc.repository.bagit.exceptions.CorruptChecksumException;
import gov.loc.repository.bagit.exceptions.FileNotInPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.MissingBagitFileException;
import gov.loc.repository.bagit.exceptions.MissingPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.MissingPayloadManifestException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.exceptions.VerificationException;
import gov.noaa.pmel.dashboard.actions.checker.ProfileDatasetChecker;
import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.ArchiveFilesBundler;
import gov.noaa.pmel.dashboard.handlers.Bagger;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.DatabaseRequestHandler;
import gov.noaa.pmel.dashboard.handlers.DsgNcFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.oads.DashboardOADSMetadata;
import gov.noaa.pmel.dashboard.oads.OADSMetadata;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;

/**
 * Submits a dataset.  At this time this just means creating the 
 * DSG and decimated DSG files for the dataset.
 * 
 * @author Karl Smith
 */
public class DatasetSubmitter {

    DashboardConfigStore configStore;
	DataFileHandler dataHandler;
	MetadataFileHandler metadataHandler;
	DatasetChecker datasetChecker;
	DsgNcFileHandler dsgHandler;
	DatabaseRequestHandler databaseHandler;
	ArchiveFilesBundler filesBundler;
	String version;
	Logger logger;

	/**
	 * @param configStore
	 * 		create with the file handlers and data checker in this data store.
	 */
	public DatasetSubmitter(DashboardConfigStore configStore) {
        this.configStore = configStore;
		dataHandler = configStore.getDataFileHandler();
		metadataHandler = configStore.getMetadataFileHandler();
		datasetChecker = configStore.getDashboardDatasetChecker();
		dsgHandler = configStore.getDsgNcFileHandler();
		databaseHandler = configStore.getDatabaseRequestHandler();
		filesBundler = configStore.getArchiveFilesBundler();
		version = configStore.getUploadVersion();
		logger = LogManager.getLogger(getClass());
	}

	/**
	 * Submit a dataset.  This standardized the data using the automated data checker 
	 * and generates DSG and decimated DSG files for datasets which are editable 
	 * (have a QC status of {@link DashboardUtils#QC_STATUS_NOT_SUBMITTED}, 
	 * {@link DashboardUtils#QC_STATUS_UNACCEPTABLE},
	 * {@link DashboardUtils#QC_STATUS_SUSPENDED}, or
	 * {@link DashboardUtils#QC_STATUS_EXCLUDED}.
	 * For all datasets, the archive status is updated to the given value.
	 * 
	 * If the archive status is {@link DashboardUtils#ARCHIVE_STATUS_SENT_FOR_ARHCIVAL},
	 * the archive request is sent for dataset which have not already been sent,
	 * or for all datasets if repeatSend is true.
	 * 
	 * @param idsSet
	 * 		IDs of the datasets to submit
	 * @param archiveStatus
	 * 		archive status to set for these cruises
	 * @param timestamp
	 * 		local timestamp to associate with this submission
	 * @param repeatSend
	 * 		re-send request to archive for datasets which already had a request sent?
	 * @param submitter
	 * 		user performing this submit 
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid,
	 * 		if the data or metadata is missing,
	 * 		if the DSG files cannot be created, or
	 * 		if there was a problem saving the updated dataset information (including archive status)
	 */
	public void submitDatasetsForQC(Collection<String> idsSet, String archiveStatus, String timestamp, 
			boolean repeatSend, String submitter) throws IllegalArgumentException {

		HashSet<String> ingestIds = new HashSet<String>();
		ArrayList<String> errorMsgs = new ArrayList<String>();
		for ( String datasetId : idsSet ) {
			// Get the dataset with data since almost always submitting for QC
			DashboardDatasetData dataset = dataHandler.getDatasetDataFromFiles(datasetId, 0, -1);
			if ( dataset == null ) 
				throw new IllegalArgumentException("Unknown dataset " + datasetId);

			boolean changed = false;
			String commitMsg = "Dataset " + datasetId;

			File dsgFile = dsgHandler.getDsgNcFile(datasetId);
			if ( Boolean.TRUE.equals(dataset.isEditable()) || ! dsgFile.exists() ) {
				try {
					// Get the metadata for this dataset
					// XXX TODO: OME_FILENAME check
				    // XXX TODO: log submission
					DashboardMetadata mdata = metadataHandler.getMetadataInfo(datasetId, DashboardUtils.metadataFilename(datasetId));
					if ( mdata != null && ! version.equals(mdata.getVersion()) ) {
						mdata.setVersion(version);
						metadataHandler.saveMetadataInfo(mdata, "Update metadata version number to " + 
								version + " with submission of " + datasetId, false);
					}
					
					// XXX TODO: OME_FILENAME check
//					DashboardOmeMetadata omeMData = new DashboardOmeMetadata(omeInfo, metadataHandler);
//					DsgMetadata dsgMData = omeMData.createDsgMetadata();
//					dsgMData.setVersion(version);

					// Standardize the data
					// TODO: update the metadata with data-derived values
					StdUserDataArray standardized = datasetChecker.standardizeDataset(dataset, null);
					if ( DashboardUtils.CHECK_STATUS_UNACCEPTABLE.equals(dataset.getDataCheckStatus()) ) {
						errorMsgs.add(datasetId + ": unacceptable; check data check error messages " +
													"(missing lon/lat/depth/time or uninterpretable values)");
						continue;
					}

					// XXX TODO: OME_FILENAME check
					DashboardOADSMetadata oadsMd = OADSMetadata.extractOADSMetadata(standardized);
					DsgMetadata dsgMData = oadsMd.createDsgMetadata();
					dsgMData.setVersion(version);
					
					// Generate the NetCDF DSG file, enhanced by Ferret
					logger.debug("Generating the full-data DSG file for " + datasetId);
					dsgHandler.saveDatasetDsg(dsgMData, standardized);

				} catch (Exception ex) {
					ex.printStackTrace();
					errorMsgs.add(datasetId + ": unacceptable; " + ex.getMessage());
					continue;
				}

				// Update dataset info with status values from the dataset data object
				dataset.setSubmitStatus(DashboardUtils.STATUS_SUBMITTED);
				dataset.setVersion(version);

				// Set up to save changes to version control
				changed = true;
				commitMsg += " submitted";
				ingestIds.add(datasetId);
			}

//			if ( archiveStatus.equals(DashboardUtils.ARCHIVE_STATUS_SENT_FOR_ARCHIVAL) && 
//				 ( repeatSend || dataset.getArchiveDate().isEmpty() ) ) {
//				// Queue the request to send (or re-send) the data and metadata for archival
//				archiveIds.add(datasetId);
//			}
//			else if ( ! archiveStatus.equals(dataset.getArchiveStatus()) ) {
//				// Update the archive status now
//				dataset.setArchiveStatus(archiveStatus);
//				changed = true;
//				commitMsg += " archive status '" + archiveStatus + "'"; 
//			}

			if ( changed ) {
				// Commit this update of the dataset properties
				commitMsg += " by user '" + submitter + "'";
				dataHandler.saveDatasetInfoToFile(dataset, commitMsg);
			}
			try {
				// Wait just a moment to let other things (mysql? svn?) catch up 
				// or clear;  submits of lots of datasets can sometimes cause 
				// messed-up DSG files not seen when submitted in small numbers.
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				// Ignore
				;
			}
		}
//		if (true) return;

		// notify ERDDAP of new/updated dataset
		if ( ! ingestIds.isEmpty() )
			dsgHandler.flagErddap(true);

		// If any dataset submit had errors, return the error messages
		// TODO: do this in a return message, not an IllegalArgumentException
		if ( errorMsgs.size() > 0 ) {
			StringBuilder sb = new StringBuilder();
			for ( String msg : errorMsgs ) { 
				sb.append(msg);
				sb.append("\n");
			}
			throw new IllegalArgumentException(sb.toString());
		}
	}

//	private void bagDatasets(List<String> datasetIds, List<String> columnsList, String archiveStatus, 
//	                         String timestamp, boolean repeatSend, String submitter) {
//        for ( String datasetId : datasetIds ) {
//            try {
//                File bag = Bagger.Bag(datasetId);
//                logger.info("Bagged " + datasetId + " as " + bag);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//    }
    
	public void archiveDatasets(List<String> datasetIds, List<String> columnsList, String archiveStatus, 
	                            String timestamp, boolean repeatSend, String submitter) {
		ArrayList<String> errorMsgs = new ArrayList<String>();
		
		// Send dataset data and metadata for archival where user requested immediate archival
		if ( ! datasetIds.isEmpty() ) {
			String userRealName;
			try {
				userRealName = databaseHandler.getReviewerRealname(submitter);
			} catch (Exception ex) {
                logger.warn(ex);
				userRealName = null;
			}
			if ( (userRealName == null) || userRealName.isEmpty() )
				throw new IllegalArgumentException("Unknown real name for user " + submitter);

			String userEmail;
			try {
				userEmail = databaseHandler.getReviewerEmail(submitter);
			} catch (Exception ex) {
				userEmail = null;
			}
			if ( (userEmail == null) || userEmail.isEmpty() )
				throw new IllegalArgumentException("Unknown e-mail address for user " + submitter);

			for ( String datasetId : datasetIds ) {
			    String thisStatus = archiveStatus;
				String commitMsg = "Immediate archival of dataset " + datasetId + " requested by " + 
						userRealName + " (" + userEmail + ") at " + timestamp;
				try {
					// filesBundler.sendOrigFilesBundle(datasetId, commitMsg, userRealName, userEmail);
//					File archiveBundle = filesBundler.createAndSendArchiveFilesBundle(datasetId, columnsList, commitMsg, userRealName, userEmail);
                    File archiveBundle = getArchiveBundle(datasetId, columnsList);
                    archiveBundleFile(datasetId, archiveBundle, datasetId, userRealName, userEmail);
                    String archiveStatusMsg = "Submitted " + archiveBundle + " to " + userEmail + " at " + DashboardServerUtils.formatTime(new Date());
                    logger.info(archiveStatusMsg);
					thisStatus = "Submitted " + DashboardServerUtils.formatTime(new Date());
				} catch (Exception ex) {
					ex.printStackTrace();
					errorMsgs.add("Failed to submit request for immediate archival of " + 
							datasetId + ": " + ex.getMessage());
					thisStatus = "Submission Failed";
//					continue;
				}
				// When successful, update the archived timestamp
				DashboardDataset cruise = dataHandler.getDatasetFromInfoFile(datasetId);
				cruise.setArchiveStatus(thisStatus);
				cruise.setArchiveDate(timestamp);
				dataHandler.saveDatasetInfoToFile(cruise, commitMsg);
			}
		}
		// If any dataset submit had errors, return the error messages
		// TODO: do this in a return message, not an IllegalArgumentException
		if ( errorMsgs.size() > 0 ) {
			StringBuilder sb = new StringBuilder();
			for ( String msg : errorMsgs ) { 
				sb.append(msg);
				sb.append("\n");
			}
			throw new IllegalArgumentException(sb.toString());
		}

	}

	/**
     * @param archiveBundle
     * @param datasetId
     * @param userRealName
     * @param userEmail
     * @return
	 * @throws IOException 
     */
    private void archiveBundleFile(String stdId, File archiveBundle, String archiveMessage, 
                                     String userRealName, String userEmail) throws IOException {
        boolean useFtp = ApplicationConfiguration.getProperty("oap.archive.use_ftp", false);
        if ( useFtp ) {
//            throw new IllegalStateException("FTP not yet supported.");
            System.out.println("FTP not yet supported.");
        } else {
            filesBundler.sendArchiveBundle(stdId, archiveBundle, userRealName, userEmail);
        }
    }

    /**
     * @param datasetId
     * @param columnsList
     * @return
	 * @throws Exception
     */
    private File getArchiveBundle(String datasetId, List<String> columnsList) throws Exception {
        File archiveBundle = null;
        if ( ApplicationConfiguration.getProperty("oap.archive.use_bagit", true)) {
            archiveBundle = Bagger.Bag(datasetId);
        } else {
            archiveBundle = filesBundler.createArchiveDataFile(datasetId, columnsList);
        }
        return archiveBundle;
    }

    public static void main(String[] args) {
        try {
            DatasetSubmitter ds = DashboardConfigStore.get(false).getDashboardDatasetSubmitter();
            ArrayList<String> ids = new ArrayList<String>() {{ add("PRISM082008"); }};
            ds.submitDatasetsForQC(ids, null, new Date().toString(), false, "lkamb");
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }
}
