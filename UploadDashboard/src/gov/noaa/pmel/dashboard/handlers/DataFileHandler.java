/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.svn.core.SVNException;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.oads.DashboardOADSMetadata;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.server.Users;
import gov.noaa.pmel.dashboard.server.util.FileTypeTest;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.FileType;
import gov.noaa.pmel.dashboard.shared.ObservationType;
import gov.noaa.pmel.dashboard.shared.QCFlag;
import gov.noaa.pmel.dashboard.upload.RecordOrientedFileReader;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.StringUtils;

import static gov.noaa.pmel.dashboard.shared.DashboardUtils.STRING_MISSING_VALUE;

/**
 * Handles storage and retrieval of cruise data in files.
 * 
 * @author Karl Smith
 */
public class DataFileHandler extends VersionedFileHandler {

	private static final String INFO_FILENAME_EXTENSION = ".properties";
	private static final String DATA_FILENAME_EXTENSION = ".tsv";
	private static final String ARCHIVE_FILENAME_EXTENSION = ".csv";
	private static final String COMMA = ",";
	private static final String TAB = "\t";
    private static final String SUBMISSION_RECORD_ID = "recordid";
    private static final String ACCESSION_NUM = "accession";
    private static final String PUBLISHED_URL = "url";
	private static final String DATA_OWNER_ID = "dataowner";
	private static final String FEATURE_TYPE_ID = "featuretype";
	private static final String OBSERVATION_TYPE_ID = "userobservationtype";
	private static final String FILE_TYPE_ID = "filetype";
	private static final String VERSION_ID = "version";
	private static final String UPLOAD_FILENAME_ID = "uploadfilename";
	private static final String UPLOAD_TIMESTAMP_ID = "uploadtimestamp";
	private static final String UPLOADED_FILE_ID = "uploadedfile";
    private static final String USER_DATASET_NAME = "datasetname";
	private static final String DOI_ID = "doi";
	private static final String DATA_CHECK_STATUS_ID = "datacheckstatus";
	private static final String MD_TIMESTAMP_ID = "mdtimestamp";
	private static final String MD_STATUS_ID = "mdstatus";
	private static final String ADDL_DOC_TITLES_ID = "addldoctitles";
	private static final String SUBMIT_STATUS_ID = "submitstatus";
	private static final String ARCHIVE_STATUS_ID = "archivestatus";
	private static final String ARCHIVE_MSG_ID = "archivemessage";
	private static final String ARCHIVE_GEN_DOI_ID = "archivegendoi";
	private static final String ARCHIVAL_DATE_ID = "archivaldate";
	private static final String NUM_DATA_ROWS_ID = "numdatarows";
	private static final String NUM_ERROR_ROWS_ID = "numerrrows";
	private static final String NUM_WARN_ROWS_ID = "numwarnrows";
	private static final String DATA_COLUMN_TYPES_ID = "datacolumntypes";
	private static final String USER_COLUMN_NAMES_ID = "usercolumnnames";
	private static final String DATA_COLUMN_UNITS_ID = "datacolumnunits";
	private static final String MISSING_VALUES_ID = "missingvalues";
	private static final String CHECKER_FLAGS = "checkerflags";
	private static final String USER_FLAGS = "userflags";

	private static final int MIN_NUM_DATA_COLUMNS = 6;

	private KnownDataTypes userTypes;

    private static Logger logger = LogManager.getLogger(DataFileHandler.class);
    
	/**
	 * Handles storage and retrieval of dataset data in files 
	 * under the given data files directory.
	 * 
	 * @param dataFilesDirName
	 * 		name of the data files directory
	 * @param svnUsername
	 * 		username for SVN authentication
	 * @param svnPassword
	 * 		password for SVN authentication
	 * @param userTypes
	 * 		known user data column types
	 * @throws IllegalArgumentException
	 * 		if the specified directory does not exist, is not
	 * 		a directory, or is not under SVN version control
	 */
	public DataFileHandler(String dataFilesDirName, String svnUsername, 
			String svnPassword, KnownDataTypes userTypes) throws IllegalArgumentException {
		super(dataFilesDirName, svnUsername, svnPassword);
		this.userTypes = userTypes;
	}

	/**
	 * @param datasetId
	 * 		the ID of the dataset
	 * @return
	 * 		the file of properties associated with the dataset
	 * @throws IllegalArgumentException
	 * 		if datasetId is not a valid dataset ID
	 */
	public File datasetInfoFile(String datasetId, boolean create) throws IllegalArgumentException {
		// Check and standardize the dataset ID
		String upperExpo = DashboardServerUtils.checkDatasetID(datasetId);
		// Create the file with the full path name of the properties file
		File grandparentDir = new File(filesDir, upperExpo.substring(0,4));
		File parentDir = new File(grandparentDir, upperExpo);
        if ( !parentDir.exists() && create ) {
            parentDir.mkdirs();
        }
		File propsFile = new File(parentDir, upperExpo + INFO_FILENAME_EXTENSION);
		return propsFile;
	}

    public String datasetDataFileName(String datasetId) throws IllegalArgumentException {
        String name = null;
        File parentDir = datasetDataDir(datasetId);
        if ( parentDir.exists()) {
            File[] dataDirFiles = parentDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return ! ( pathname.isDirectory() || pathname.getName().endsWith(".properties"));
                }
            });
            if ( dataDirFiles.length > 0 ) {
                name = dataDirFiles[0].getName();
            }
        }
        
        return name;
    }
    
    public boolean datasetDataDirExists(String datasetId) {
		// Check and standardize the dataset ID
		String upperExpo = DashboardServerUtils.checkDatasetID(datasetId);
		// Create the file with the full path name of the properties file
		File grandparentDir = new File(filesDir, upperExpo.substring(0,4));
		File parentDir = new File(grandparentDir, upperExpo);
        return parentDir.exists();
    }
    public File datasetDataDir(String datasetId) {
		// Check and standardize the dataset ID
		String upperExpo = DashboardServerUtils.checkDatasetID(datasetId);
		// Create the file with the full path name of the properties file
		File grandparentDir = new File(filesDir, upperExpo.substring(0,4));
		File parentDir = new File(grandparentDir, upperExpo);
        return parentDir;
    }
    
	public File datasetUploadedFile(String datasetId) throws IllegalArgumentException {
        DashboardDataset dataset = getDatasetFromInfoFile(datasetId);
        return datasetUploadedFile(datasetId, dataset);
	}
	public File datasetUploadedFile(String datasetId, DashboardDataset dataset) throws IllegalArgumentException {
        File uploadedFile = _datasetDataFile(datasetId, dataset.getUploadFilename());
        return uploadedFile;
	}
	public File datasetUploadedFile(String datasetId, String uploadFilename) throws IllegalArgumentException {
        File uploadedFile = _datasetDataFile(datasetId, uploadFilename);
        return uploadedFile;
	}
	public File datasetDataFile(String datasetId, DashboardDataset dataset) throws IllegalArgumentException {
        File parentDir = datasetDataDir(datasetId);
        if ( !parentDir.exists()) {
            parentDir.mkdirs();
        }
        File dataFile = _datasetDataFile(datasetId, null); // , dataset.getUploadFilename());
        if ( dataFile == null ) {
            if ( dataset.getFeatureType().equals(FeatureType.OTHER)) {
                dataFile = new File(parentDir, dataset.getUploadFilename());
            } else {
                dataFile = new File(parentDir, datasetId + ".tsv");
            }
        }
        return dataFile;
	}
    
	/**
	 * @param datasetId
	 * 		the ID of the dataset
	 * @return
	 * 		the data file associated with the dataset
	 * @throws IllegalArgumentException
	 * 		if datasetId is not a valid dataset ID
	 */
	public File datasetDataFile(String datasetId) throws IllegalArgumentException {
        return datasetDataFile(datasetId, (String)null);
	}
	public File datasetDataFile(String datasetId, String dataFileName) throws IllegalArgumentException {
        File parentDir = datasetDataDir(datasetId);
        if ( !parentDir.exists()) {
            parentDir.mkdirs();
        }
        File dataFile = _datasetDataFile(datasetId, dataFileName);
        if ( dataFile == null ) {
            String defaultName = datasetId +".tsv";
            logger.info("Using default dataset filename: " + defaultName);
            dataFile = new File(parentDir, defaultName);
        }
        return dataFile;
	}
    
	private File _datasetDataFile(String datasetId, String dataFileName) throws IllegalArgumentException {
        File dataFile = null;
        File parentDir = datasetDataDir(datasetId);
        if ( !parentDir.exists()) {
            return null;
        }
        if ( dataFileName != null ) {
            dataFile = new File(parentDir, dataFileName);
        } else {
    		dataFile = new File(parentDir, datasetId.toUpperCase() + DATA_FILENAME_EXTENSION);
//            File[] dataDirFiles = parentDir.listFiles(new FileFilter() {
//                @Override
//                public boolean accept(File pathname) {
//                    return ! ( pathname.isDirectory() || pathname.getName().endsWith(".properties") || pathname.getName().endsWith(".messages"));
//                }
//            });
//            if ( dataDirFiles.length == 0 ) {
//                logger.info("No data files found for dataset ID: " + datasetId);
//            } else {
//        		dataFile = dataDirFiles[0];
//            }
//            if ( dataDirFiles.length > 1 ) {
//                logger.warn(dataDirFiles.length + " data files found for " + datasetId +". Only archiving first: " + dataFile.getAbsolutePath());
//            }
        }
		return dataFile;
	}

	public File archiveDataFile(String datasetId) throws IllegalArgumentException {
		// Check and standardize the dataset ID
		String upperExpo = DashboardServerUtils.checkDatasetID(datasetId);
		// Create the file with the full path name of the properties file
		File grandparentDir = new File(DashboardConfigStore.getTempDir(), upperExpo.substring(0,4));
		File parentDir = new File(grandparentDir, upperExpo);
        if ( !parentDir.exists()) {
            parentDir.mkdirs();
        }
		File dataFile = new File(parentDir, upperExpo + ARCHIVE_FILENAME_EXTENSION);
		return dataFile;
	}
	
	/**
	 * Searches all existing datasets and returns the dataset IDs of those that
	 * match the given dataset ID containing wildcards and/or regular expressions.
	 * 
	 * @param wildDatasetId
	 * 		dataset ID, possibly with wildcards * and ?, to use;
	 * 		any characters are converted to uppercase,
	 * 		"*" is turned in the regular expression "[\p{javaUpperCase}\p{Digit}]+", and
	 * 		"?" is turned in the regular expression "[\p{javaUpperCase}\p{Digit}]{1}".
	 * @return
	 * 		list of dataset IDs of existing datasets that match 
	 * 		the given wildcard dataset ID; never null, but may be empty
	 * @throws IllegalArgumentException
	 * 		if wildDatasetId is not a valid dataset ID pattern
	 */
	public HashSet<String> getMatchingDatasetIds(String wildDatasetId) 
												throws IllegalArgumentException {
		HashSet<String> matchingIds = new HashSet<String>();
		final Pattern filenamePattern;
		try {
			String filenameRegEx = wildDatasetId.toUpperCase();
			filenameRegEx = filenameRegEx.replace("*", "[\\p{javaUpperCase}\\p{Digit}]+");
			filenameRegEx = filenameRegEx.replace("?", "[\\p{javaUpperCase}\\p{Digit}]{1}");
//			filenameRegEx += INFO_FILENAME_EXTENSION;
			filenamePattern = Pattern.compile(filenameRegEx);
		} catch (PatternSyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
		File[] grandparents = filesDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if ( pathname.isDirectory() )
					return true;
				return false;
			}
		});
		for ( File partitionDir : grandparents ) {
    		File[] cruiseDirs = partitionDir.listFiles(new FileFilter() {
    			@Override
    			public boolean accept(File file) {
//    				if ( pathname.isDirectory() )
//    					return true;
//    				return false;
//    			}
//    		});
//            for ( File cruiseDir : cruiseDirs ) {
//    			File[] matchFiles = cruiseDir.listFiles(new FileFilter() {
//    				@Override
//    				public boolean accept(File file) {
    					if ( file.isDirectory() && filenamePattern.matcher(file.getName()).matches() 
    					     && new File(file, file.getName()+".properties").exists()) {
    						return true;
    					}
    					return false;
    				}
    			});
    			for ( File match : cruiseDirs ) {
    				String datasetId = match.getName();
//    				String datasetId = name.substring(0, name.length() - INFO_FILENAME_EXTENSION.length());
    				matchingIds.add(datasetId);
    			}
            }
//		}
		return matchingIds;
	}

	/**
	 * Determines if a dataset properties file exists
	 * @param datasetId
	 * 		ID of the dataset to check
	 * @return
	 * 		true if the dataset properties file exists
	 * @throws IllegalArgumentException
	 * 		if datasetId is not a valid dataset ID
	 */
	public boolean infoFileExists(String datasetId) throws IllegalArgumentException {
		File infoFile = datasetInfoFile(datasetId, false);
		return infoFile.exists();
	}

	/**
	 * Determines if a dataset data file exists
	 * @param datasetId
	 * 		ID of the dataset to check
	 * @return
	 * 		true if the dataset data file exists
	 * @throws IllegalArgumentException
	 * 		if datasetId is not a valid dataset ID
	 */
	public boolean dataFileExists(String datasetId) throws IllegalArgumentException {
//		return dataFileExists(datasetId, null);
//	}
//	public boolean dataFileExists(String datasetId, String dataFileName) throws IllegalArgumentException {
		File dataFile = datasetDataFile(datasetId); // , dataFileName);
		return dataFile.exists();
	}

	/**
	 * Creates dataset data object containing data read from the given reader.
	 * A map of the dataset data objects, keyed by the dataset IDs, is returned.
	 * 
	 * Data is read as String values in the format specified.  Blank lines and 
	 * ines with all-blank values are ignored.  The first line with no blank or
	 * pure-numeric values, and at least {@value #MIN_NUM_DATA_COLUMNS} values, 
	 * is assumed to be the header of data column names, possibly with units.  
	 * Optionally, the column names header line can be immediately followed by 
	 * a line with no pure-numeric values, which will be used as units for the 
	 * data columns.  All remaining lines (that are not ignored) are considered 
	 * data lines and should have the same number of values as in the column names. 
	 * 
	 * One data column in the data read must be recognized as the dataset (cruise) 
	 * name; the name for such a column include (case-insensitive) "Dataset", 
	 * "Dataset ID", "Dataset Name", "Cruise", "Cruise ID", and "Cruise Name".  
	 * The dataset ID for each sample will be constructed from the value in this 
	 * column by converting any letters to uppercase and removing any character 
	 * that is not a number or letter.  The data for this sample will be added 
	 * to the dataset data object associated with that dataset ID.
	 * 
	 * @param dataReader
	 * 		read data from here;
	 * @param dataFormat 
	 * 		format of the data read; one of 
	 * 		{@link DashboardUtils#COMMA_FORMAT_TAG}.
	 * 		{@link DashboardUtils#SEMICOLON_FORMAT_TAG}, or
	 * 		{@link DashboardUtils#TAB_FORMAT_TAG}.
	 * @param owner
	 * 		name of the owner to record in the datasets created or appended to;
	 * 		also used to guess data column types from column names
	 * @param filename
	 * 		upload filename to record in the datasets created or appended to
	 * @param timestamp
	 * 		upload timestamp to record in the datasets created or appended to
	 * @return
	 * 		map of dataset data objects, keyed on their dataset IDs
	 * @throws IOException
	 * 		if reading from datasetsReader throws one,
	 * 		if the dataFormat string is not recognized 
	 * 		if there is an inconsistent number of data values (columns) in a data sample (row)
	 * 		if a dataset/cruise name column was not found,
	 * 		if a dataset ID derived from the dataset name is not valid (too short or too long),
	 * 		if there are too few data columns, or
	 * 		if no data samples (rows) were found.
	 */
	public static TreeMap<String,DashboardDatasetData> createDatasetsFromInput(InputStream inStream,
	                                                                    String dataFormat, 
	                                                                    String owner, 
	                                                                    String filename, 
	                                                                    String timestamp, 
                                                                        String submissionRecordId,
                                                                        String specifiedDatasetId,
	                                                                    String datasetIdColName) 
	    throws IOException 
	{
        BufferedReader dataReader = new BufferedReader( new InputStreamReader(inStream)); 
        return createDatasetsFromInput(dataReader, dataFormat, owner, filename, timestamp, 
                                       submissionRecordId, specifiedDatasetId, datasetIdColName);
	}
    
	public static TreeMap<String,DashboardDatasetData> createDatasetsFromInput(BufferedReader dataReader, 
	                                                                    String dataFormat, 
	                                                                    String owner, 
	                                                                    String filename, 
	                                                                    String timestamp, 
                                                                        String submissionRecordId,
                                                                        String specifiedDatasetId,
	                                                                    String datasetIdColName) 
	    throws IOException 
	{
        // At this point, The spacer is used only to rebuild lines for error messages.
		Character spacer = null;
		if ( DashboardUtils.TAB_FORMAT_TAG.equals(dataFormat) ) {
			spacer = '\t';
		} else if ( DashboardUtils.COMMA_FORMAT_TAG.equals(dataFormat) ) {
			spacer = ',';
		} else if ( DashboardUtils.SEMICOLON_FORMAT_TAG.equals(dataFormat) ) {
			spacer = ';';
		} else if ( DashboardUtils.UNSPECIFIED_DELIMITER_FORMAT_TAG.equals(dataFormat)) {
		    spacer = null;
		}
		else
			throw new IOException("Unexpected invalid data format '" + dataFormat + "'");

		CsvParserSettings settings = new CsvParserSettings();
        if ( spacer == null ) {
    		settings.detectFormatAutomatically();
            settings.setDelimiterDetectionEnabled(true, '\t', ';', ',', '|');
        } else {
            settings.getFormat().setDelimiter(spacer);
        }
        
        settings.setCommentCollectionEnabled(true);
        settings.setNullValue("");
        settings.setEmptyValue("");
        settings.setSkipEmptyLines(true); // default
        
            CsvParser dataParser = new CsvParser(settings);
            dataParser.beginParsing(dataReader);
            if ( spacer == null ) {
                CsvFormat fileFormat = dataParser.getDetectedFormat();
                logger.info("Detected file format: " + fileFormat);
            }
            return createDatasetsFromInput(dataParser.iterate(dataReader), dataFormat, owner, 
                                           filename, timestamp, submissionRecordId, 
                                           specifiedDatasetId, datasetIdColName);
	}
    
	public static DashboardDatasetData createSingleDatasetFromInput(Iterable<String[]> records,
	                                                                    String dataFormat, 
	                                                                    String owner, 
	                                                                    String filename, 
	                                                                    String timestamp, 
                                                                        String submissionRecordId,
                                                                        String specifiedDatasetId,
	                                                                    String datasetIdColName) 
	    throws IOException 
	{
        Map<String, DashboardDatasetData> datasetMap = createDatasetsFromInput(records, dataFormat, 
                                                                               owner, filename, timestamp, 
                                                                               submissionRecordId, 
                                                                               specifiedDatasetId, datasetIdColName);
        if ( datasetMap.isEmpty()) {
            throw new IllegalStateException("No datasets returned from " + owner+":"+filename );
        }
        DashboardDatasetData theDataset = datasetMap.values().iterator().next();
        if ( datasetMap.size() > 1 ) {
            logger.warn("Returned multiple datasets from file " + owner+":"+filename + ": " + datasetMap);
        }
        logger.info("Returning dataset " + theDataset);
        return theDataset;
	}
	public static TreeMap<String,DashboardDatasetData> createDatasetsFromInput(Iterable<String[]> records,
	                                                                    String dataFormat, 
	                                                                    String owner, 
	                                                                    String filename, 
	                                                                    String timestamp, 
                                                                        String submissionRecordId,
                                                                        String specifiedDatasetId,
	                                                                    String datasetIdColName) 
	    throws IOException 
	{
		TreeMap<String,DashboardDatasetData> datasetsMap = new TreeMap<String,DashboardDatasetData>();
		int numDataColumns = 0;
        int rowNum = 0;
        boolean foundHeader = false;

        try {
			ArrayList<String> columnNames = null;
			boolean checkForUnits = false;
			ArrayList<DataColumnType> columnTypes = null;
			int datasetNameColIdx = -1;
			String version = null;
            boolean hasTrailer = false;

			for ( String[] record : records) {

                rowNum += 1;
                if ( rowIsEmpty(record) || isComment(record)) {
                    logger.debug("empty or comment row at " + rowNum);
                    continue;
                }
                int nColumns = record.length;
                
				// Still looking for headers?
				if ( columnNames == null ) {
					if ( nColumns >= MIN_NUM_DATA_COLUMNS ) {
						// These could be the column names headers
						// column names must not be blank or pure numeric
						boolean isHeader = true;
                        int fieldNum = 0;
						for ( String val : record ) {
                            fieldNum += 1;
							if ( val.isEmpty() ) {
                                if ( fieldNum == nColumns) { // last / extra column ignored
                                    hasTrailer = true;
                                } else if ( fieldNum == 1 ) { // first column may be record num.
                                    logger.debug("First header column empty.");
                                    continue;
                                } else {
    								isHeader = false;
    								break;
                                }
							}
							try {
								Double.parseDouble(val);
								isHeader = false;
								break;
							} catch (Exception ex) {
								// Expected result for a name
								;
							}
						}
						if ( isHeader ) {
                            foundHeader = true;
							// These indeed are the column headers
							numDataColumns = hasTrailer ? nColumns -1 : nColumns;
							columnNames = new ArrayList<String>(numDataColumns);
							for ( String val : record ) {
								columnNames.add(val);
							}
                            
						}
						// Check for units in the next record
						checkForUnits = true;
					}
					// Ignore anything prior to the column headers
					continue;
				}

                if ( ! isEndTag(record)) {
    
    				// Check that the number of columns is consistent
                    // We don't check the (possible) units row, as it may have empty columns at the end
    				// which may get trimmed off.
    				if ( nColumns != numDataColumns ) {
                        String msg = "Inconsistent number of data columns (" + 
        							nColumns + " instead of " + numDataColumns + 
        							") at row " + rowNum;
                        logger.info(msg);
                        if ( nColumns < numDataColumns ) { // may have been trimmed by record parser
                            String[] padded = new String[numDataColumns];
                            int i = 0;
                            for ( ; i < nColumns; i++ ) {
                                padded[i] = record[i];
                            }
                            for ( ; i < numDataColumns; i++ ) {
                                padded[i] = "";
                            }
                            record = padded;
                        } else if ( nColumns == numDataColumns+1 && hasTrailer(record) ) {
                            nColumns = numDataColumns;
                            logger.info("Row has a trailer. Ignoring.");
                        } else {
        					throw new IllegalStateException( "Inconsistent number of data columns (" + 
        							nColumns + " instead of " + numDataColumns + 
        							") at row " + rowNum + ":\n    " +
        							rebuildDataline(record, ','));
                        }
        			}
                    
    				if ( checkForUnits ) {
    					// The line immediately following the data column names could be units
    					checkForUnits = false;
    					boolean isUnits = true;
    					// A unit specification cannot be pure numeric
    					for ( String val : record ) {
    						try {
    							Double.valueOf(val);
    							isUnits = false;
    							break;
    						} catch (NumberFormatException ex) {
    							// Expected result for a units specification
    							;
    						}
    					}
    					if ( isUnits ) {
    						// Add the units to the column header names 
    						ArrayList<String> namesWithUnits = new ArrayList<String>(numDataColumns);
    						int k = 0;
    						for ( String units : record ) {
    							if ( units.isEmpty() )
    								namesWithUnits.add(columnNames.get(k));
    							else
    								namesWithUnits.add(columnNames.get(k) + " [" + units + "]");
    							k++;
    						}
    						columnNames = namesWithUnits;
    					}
    					
    					// Assign the data column types from the column names (including customizations for this user)
    					DashboardDataset fakeDataset = new DashboardDataset();
    					fakeDataset.setOwner(owner);
    					fakeDataset.setUserColNames(columnNames);
    					DashboardConfigStore configStore;
    					try {
    						configStore = DashboardConfigStore.get(false);
    					} catch ( IOException ex ) {
    						throw new IOException("Unexpected failure to get the dashboard configuration");
    					}
    					configStore.getUserFileHandler().assignDataColumnTypes(fakeDataset, datasetIdColName);
    					columnTypes = fakeDataset.getDataColTypes();
//                        if ( StringUtils.emptyOrNull(specifiedDatasetId)) {
//        					int k = 0;
//        					for ( DataColumnType dtype : columnTypes ) {
//        						if ( ( ! DashboardUtils.isEmptyNull(datasetIdColName) && columnNames.get(k).equalsIgnoreCase(datasetIdColName)) ||
//        								DashboardServerUtils.DATASET_NAME.typeNameEquals(dtype) ) {
//        							datasetNameColIdx = k;
//        							break;
//        						} 
//        						
//        						k++;
//        					}
//        					if ( datasetNameColIdx < 0 ) {
//        						String msg = "Dataset ID column not found";
//        						if ( ! DashboardUtils.isEmptyNull(datasetIdColName)) {
//        							msg += ": " + datasetIdColName;
//        						}
//        						throw new IllegalStateException(msg);
//        					}
//                        }
        
    					// Get the version to record in the datasets
    					version = configStore.getUploadVersion();
    
    					// If this was indeed a line of units, go on to the next line;
    					// otherwise this is the first line of data values to parse
    					if ( isUnits )
    						continue;
    				}
    
                    
    				ArrayList<String> datavals = new ArrayList<String>(numDataColumns);
    				for ( String val : record ) {
    					datavals.add(val);
    				}
    
                    String datasetId;
                    if ( StringUtils.emptyOrNull(specifiedDatasetId)) {
        				// Actual data line with values
        				String datasetName = datavals.get(datasetNameColIdx);
        				datasetId = DashboardServerUtils.getDatasetIDFromName(datasetName);
        				if ( (datasetId.length() < DashboardServerUtils.MIN_DATASET_ID_LENGTH) ||
        					 (datasetId.length() > DashboardServerUtils.MAX_DATASET_ID_LENGTH) ) 
        					throw new IllegalStateException("Invalid dataset ID \"" + datasetId + "\" from dataset name \"" 
    										   + datasetName + "\" at row number " + rowNum 
    										   +". Expecting dataset ID in column number " + (datasetNameColIdx+1) // zero-based indexing confuses the reader
    										   + " \"" + columnNames.get(datasetNameColIdx) + "\" DatasetID must be between " + DashboardServerUtils.MIN_DATASET_ID_LENGTH
    										   + " and " + DashboardServerUtils.MAX_DATASET_ID_LENGTH + " characters in length.");
                    } else {
                        datasetId = DashboardServerUtils.getDatasetIDFromName(submissionRecordId);
                    }
        
                    String userName = StringUtils.emptyOrNull(specifiedDatasetId) ?
                                        filename :
                                        specifiedDatasetId;
                            
    				DashboardDatasetData dataset = datasetsMap.get(datasetId);
    				if ( dataset == null ) {
    					dataset = new DashboardDatasetData();
    					dataset.setDatasetId(datasetId);
                        dataset.setUserDatasetName(userName);
    					dataset.setOwner(owner);
    					dataset.setUploadFilename(filename);
    					dataset.setUploadTimestamp(timestamp);
    					dataset.setUserColNames(columnNames);
    					dataset.setDataColTypes(columnTypes);
    					dataset.setVersion(version);
    				}
    				int dataRowNum = dataset.getNumDataRows();
    				dataset.getRowNums().add(dataRowNum);
    				dataset.getDataValues().add(datavals);
                    dataset.setNumDataRows(dataRowNum+1);
    				datasetsMap.put(datasetId, dataset);
    			}
		    }
		} catch (Exception ex) {
            logger.warn(ex,ex);
            throw ex;
		} 
        logger.debug("Processed " + rowNum + " records.");
		if ( numDataColumns < MIN_NUM_DATA_COLUMNS ) {
            if ( !foundHeader ) {
                throw new IllegalStateException("A data header row was not found." );
            } else 
    			throw new IllegalStateException("No data columns found, possibly due to incorrect format");
		}
		if ( datasetsMap.isEmpty() )
			throw new IllegalStateException("No data rows found");
		return datasetsMap;
	}
    
    /**
     * @param record
     * @return
     */
    private static boolean hasTrailer(String[] record) {
        return StringUtils.emptyOrNull(record[record.length-1]);
    }

    /**
     * @param record
     * @return
     */
    private static boolean isComment(String[] rowValues) {
        if ( rowValues == null || rowValues.length == 0 ) { return false; }
        return rowValues[0].trim().startsWith("#");
    }
    private static boolean rowIsEmpty(String[] rowValues) {
        if ( rowValues == null || rowValues.length == 0 ) { return true; }
        
        for ( String v : rowValues ) {
            if ( ! StringUtils.emptyOrNull(v)) { return false; }
        }
        return true;
    }

	/**
     * @return
     */
    private static boolean isEndTag(String[] record) {
        return record[0].toUpperCase().contains("END") ||
               record[0].toUpperCase().contains("EOF");
    }

    /**
	 * Returns a new DashboardDataset assigned from the dataset information
	 * file without reading any of the data in dataset data file.
	 * 
	 * @param datasetId
	 * 		ID of the dataset to read
	 * @return
	 * 		new DashboardDataset assigned from the information file,
	 * 		or null if the dataset information file does not exist
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is not valid or
	 * 		if there are problems accessing the information file
	 */
	public DashboardDataset getDatasetFromInfoFile(String datasetId) throws IllegalArgumentException {
		DashboardDataset dataset = new DashboardDataset();
		dataset.setDatasetId(datasetId);
		// Read the information saved in the properties file
		try {
			assignDatasetFromInfoFile(dataset);
		} catch ( FileNotFoundException ex ) {
			return null;
		} catch ( IOException ex ) {
			throw new IllegalArgumentException("Problems reading dataset information for " + 
							datasetId + ": " + ex.getMessage());
		}
		return dataset;
	}

	/**
	 * Get a dataset with data saved to file
	 * 
	 * @param datasetId
	 * 		ID of the dataset to read
	 * @param firstDataRow
	 * 		index of the first data row to return
	 * @param numDataRows
	 * 		maximum number of data rows to return; if negative, no limit
	 * 		is imposed (all remaining data is returned)
	 * @return
	 * 		the dataset with data, or null if there is no 
	 * 		information or data saved for this dataset.
	 * @throws IllegalArgumentException
	 * 		if the dataset is invalid or if there was a error reading 
	 * 		information or data for this cruise
	 */
	public DashboardDatasetData getDatasetDataFromFiles(String datasetId,
			int firstDataRow, int numDataRows) throws IllegalArgumentException {
		// Create the cruise and assign the dataset
		DashboardDatasetData cruiseData = new DashboardDatasetData();
		cruiseData.setDatasetId(datasetId);
		try {
			// Assign values from the cruise information file
			assignDatasetFromInfoFile(cruiseData);
		} catch ( FileNotFoundException ex ) {
			return null;
		} catch ( IOException ex ) {
			throw new IllegalArgumentException("Problems reading dataset information for " + 
					datasetId + ": " + ex.getMessage());
		}
        if ( numDataRows != 0 ) {
        // We assume if they're asking for it, it can be read.
		// This is currently determined at upload time by detected file type, 
		// and no parse exceptions.
//        if ( cruiseData.getUserColNames().isEmpty()) {
        // if ( cruiseData.dataIsParseable() ) {
    		File dataFile = datasetDataFile(datasetId);
            try ( BufferedInputStream instream = new BufferedInputStream(new FileInputStream(dataFile))) {
                String fileType = FileTypeTest.getFileType(instream);
                RecordOrientedFileReader reader = RecordOrientedFileReader.getFileReader(fileType, instream);
                assignDataFromInput(cruiseData, reader, firstDataRow, numDataRows);
    		} catch ( FileNotFoundException ex ) {
    			return null;
    		} catch ( Exception ex ) {
    			throw new IllegalArgumentException("Problems reading cruise data for " + 
    					datasetId + ": " + ex.getMessage());
    		}
//        }
        }
		// Read the cruise data file
//        if ( !datasetId.startsWith("BEJ")) {
//		try ( BufferedReader cruiseReader = new BufferedReader(new FileReader(dataFile)); ) {
//			// Assign values from the cruise data file
//			assignDataFromInput(cruiseData, cruiseReader, firstDataRow, numDataRows);
//		} catch ( FileNotFoundException ex ) {
//			return null;
//		} catch ( IOException ex ) {
//			throw new IllegalArgumentException("Problems reading cruise data for " + 
//					datasetId + ": " + ex.getMessage());
//		}
//        } else {
//        }
		return cruiseData;
	}

    /*
    public void processUploadedFile() throws UploadProcessingException {
            String filename = _uploadedFile.getName();
//            String itemType = item.getContentType();
            // Get the datasets from this file
            TreeMap<String,DashboardDatasetData> datasetsMap = null;
            
            try ( InputStream inStream = new FileInputStream(_uploadedFile); ) {
                RecordOrientedFileReader recordReader = getFileReader(_uploadedFile, inStream);
                datasetsMap = DataFileHandler.createDatasetsFromInput(recordReader, dataFormat, 
                                                                      username, filename, timestamp, 
                                                                      specifiedDatasetId, datasetIdColName);
            } catch (IllegalStateException ex) {
                _messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
                _messages.add(ex.getMessage());
                _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                return;
            } catch (Exception ex) {
                // Mark as a failed file, and go on to the next
                _messages.add(DashboardUtils.INVALID_FILE_HEADER_TAG + " " + filename);
                _messages.add("There was an error processing the data file.");
                _messages.add(DashboardUtils.END_OF_ERROR_MESSAGE_TAG);
                return;
            }

            // Process all the datasets created from this file
            String datasetId = null;
            for ( DashboardDatasetData datasetData : datasetsMap.values() ) {
                datasetData.setFileType(fileType.name());
                datasetData.setFeatureType(_featureType.name());
                datasetData.setUploadedFile(_uploadedFile.getPath());
                // Check if the dataset already exists
                datasetId = datasetData.getDatasetId();
                boolean datasetExists = _datasetHandler.dataFileExists(datasetId);
                boolean appended = false;
                if ( datasetExists ) {
                    String owner = "";
                    String status = "";
                    try {
                        // Read the original dataset info to get the current owner and submit status
                        DashboardDataset oldDataset = _datasetHandler.getDatasetFromInfoFile(datasetId);
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
                        _datasetHandler.verifyOkayToDeleteDataset(datasetId, username);
                    } catch ( Exception ex ) {
                        _messages.add(DashboardUtils.DATASET_EXISTS_HEADER_TAG + " " + 
                                filename + " ; " + datasetId + " ; " + owner + " ; " + status);
                        continue;
                    }
                    if ( DashboardUtils.APPEND_DATASETS_REQUEST_TAG.equals(action) ) {
                        // Get all the data from the existing dataset
                        DashboardDatasetData oldDataset;
                        try {
                            oldDataset = _datasetHandler.getDatasetDataFromFiles(datasetId, 0, -1);
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
                    _datasetHandler.saveDatasetInfoToFile(datasetData, "Dataset info " + commitMsg);
                    _datasetHandler.saveDatasetDataToFile(datasetData, "Dataset data " + commitMsg);
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
//        }
        // Update the list of cruises for the user
        try {
            _configStore.getUserFileHandler().addDatasetsToListing(_successes, username);
        } catch (IllegalArgumentException ex) {
            throw new UploadProcessingException("Unexpected error updating list of datasets \n" + ex.getMessage(), ex);
        }
    }
*/
	
	/**
	 * Saves and commits only the dataset properties to the information file.
	 * This does not save the dataset data of a DashboardDatasetData.
	 * This first checks the currently saved properties for the cruise, and 
	 * writes and commits a new properties file only if there are changes.
	 * 
	 * @param dataset
	 * 		save properties of this dataset
	 * @param message
	 * 		version control commit message; 
	 * 		if null or blank, the commit will not be performed 
	 * @throws IllegalArgumentException
	 * 		if the ID of the dataset is not valid, 
	 * 		if there was an error writing information for this dataset to file, or 
	 * 		if there was an error committing the updated file to version control
	 */
	public void saveDatasetInfoToFile(DashboardDataset dataset, String message) 
												throws IllegalArgumentException {
		// Get the dataset information filename
		String datasetId = dataset.getDatasetId();
		File infoFile = datasetInfoFile(datasetId, true);
		// First check if there are any changes from what is saved to file
		try {
			DashboardDataset savedDataset = getDatasetFromInfoFile(datasetId);
			if ( (savedDataset != null) && savedDataset.equals(dataset) )
				return;
		} catch ( IllegalArgumentException ex ) {
			// Some problem with the saved data
			;
		}
		// Create the directory tree if it does not exist
		File parentFile = infoFile.getParentFile();
		if ( ! parentFile.exists() )
			parentFile.mkdirs();
		// Create the properties for this dataset information file
		Properties datasetProps = new Properties();
        datasetProps.setProperty(ACCESSION_NUM, dataset.getAccession());
        datasetProps.setProperty(PUBLISHED_URL, dataset.getPublishedUrl());
        datasetProps.setProperty(SUBMISSION_RECORD_ID, dataset.getRecordId());
        datasetProps.setProperty(USER_DATASET_NAME, dataset.getUserDatasetName());
		// Owner of the dataset
		datasetProps.setProperty(DATA_OWNER_ID, dataset.getOwner());
        // user observation type
		datasetProps.setProperty(OBSERVATION_TYPE_ID, dataset.getUserObservationType());
        // DSG feature type
		datasetProps.setProperty(FEATURE_TYPE_ID, 
		                         ObservationType.featureTypeOf(dataset.getUserObservationType()).name());
        // file format
		datasetProps.setProperty(FILE_TYPE_ID, dataset.getFileTypeName());
		// Version 
		datasetProps.setProperty(VERSION_ID, dataset.getVersion());
		// Upload filename
		datasetProps.setProperty(UPLOAD_FILENAME_ID, dataset.getUploadFilename());
		// Upload timestamp
		datasetProps.setProperty(UPLOAD_TIMESTAMP_ID, dataset.getUploadTimestamp());
		// Uploaded original file
		datasetProps.setProperty(UPLOADED_FILE_ID, dataset.getUploadedFile());
		// Data DOI
		datasetProps.setProperty(DOI_ID, dataset.getDoi());
		// Data-check status string
		datasetProps.setProperty(DATA_CHECK_STATUS_ID, dataset.getDataCheckStatus());
		// OME metadata timestamp
		datasetProps.setProperty(MD_TIMESTAMP_ID, dataset.getMdTimestamp());
		datasetProps.setProperty(MD_STATUS_ID, dataset.getMdStatus());
		// Metadata documents
		datasetProps.setProperty(ADDL_DOC_TITLES_ID, 
				DashboardUtils.encodeStringArrayList(dataset.getAddlDocs()));
		// QC-submission status string
		datasetProps.setProperty(SUBMIT_STATUS_ID, dataset.getSubmitStatus());
		// Archive status string
		datasetProps.setProperty(ARCHIVE_STATUS_ID, dataset.getArchiveStatus());
        // optional user archive message
		datasetProps.setProperty(ARCHIVE_MSG_ID, dataset.getArchiveSubmissionMessage());
        // whether requesting DOI is generated by archive
		datasetProps.setProperty(ARCHIVE_GEN_DOI_ID, String.valueOf(dataset.getArchiveDOIrequested()));
		// Date of request to archive original data and metadata files
		datasetProps.setProperty(ARCHIVAL_DATE_ID, 
		                         DashboardServerUtils.formatUTC(dataset.getArchiveDate(), 
                                                                DashboardUtils.DATE_ARCHIVE_FORMAT));
		// Total number of data measurements (rows of data)
		datasetProps.setProperty(NUM_DATA_ROWS_ID, 
				Integer.toString(dataset.getNumDataRows()));
		// Number of data rows with error messages
		datasetProps.setProperty(NUM_ERROR_ROWS_ID, 
				Integer.toString(dataset.getNumErrorRows()));
		// Number of data rows with warning messages
		datasetProps.setProperty(NUM_WARN_ROWS_ID, 
				Integer.toString(dataset.getNumWarnRows()));
		// Data column name in the original upload data file
		datasetProps.setProperty(USER_COLUMN_NAMES_ID, 
				DashboardUtils.encodeStringArrayList(dataset.getUserColNames()));
		// Data column type information
		int numCols = dataset.getDataColTypes().size();
		ArrayList<String> colTypeNames = new ArrayList<String>(numCols);
		ArrayList<String> colUnitNames = new ArrayList<String>(numCols);
		ArrayList<String> colMissValues = new ArrayList<String>(numCols);
		for ( DataColumnType colType : dataset.getDataColTypes() ) {
			colTypeNames.add(colType.getVarName());
			colUnitNames.add(colType.getUnits().get(colType.getSelectedUnitIndex()));
			colMissValues.add(colType.getSelectedMissingValue());
		}
		// Data column type/variable name
		datasetProps.setProperty(DATA_COLUMN_TYPES_ID, 
				DashboardUtils.encodeStringArrayList(colTypeNames));
		// Unit for each data column
		datasetProps.setProperty(DATA_COLUMN_UNITS_ID, 
				DashboardUtils.encodeStringArrayList(colUnitNames));
		// Missing value for each data column
		datasetProps.setProperty(MISSING_VALUES_ID, 
				DashboardUtils.encodeStringArrayList(colMissValues));

		// Flags
		datasetProps.setProperty(CHECKER_FLAGS, 
				DashboardUtils.encodeQCFlagSet(dataset.getCheckerFlags()));
		datasetProps.setProperty(USER_FLAGS, 
				DashboardUtils.encodeQCFlagSet(dataset.getUserFlags()));

		// Save the properties to the cruise information file
		try ( PrintWriter propsWriter = new PrintWriter(infoFile); ) {
			datasetProps.store(propsWriter, null);
		} catch ( IOException ex ) {
			throw new IllegalArgumentException("Problems writing dataset information for " + 
					datasetId + " to " + infoFile.getPath() + ": " + ex.getMessage());
		}
		
		if ( (message == null) || message.trim().isEmpty() )
			return;

		// Submit the updated information file to version control
		try {
			commitVersion(infoFile, message);
		} catch ( Exception ex ) {
            logger.warn(ex,ex);
//			throw new IllegalArgumentException("Problems committing updated dataset information for  " + 
//							datasetId + " in file " + infoFile.getAbsolutePath() + ": " + ex.getMessage(), ex);
		}
	}

	/**
	 * Saves and commits the dataset data to the data file.
	 * The dataset information file needs to be saved using 
	 * {@link #saveDatasetInfoToFile(DashboardDataset, String)}.
	 * 
	 * @param datasetData
	 * 		dataset data to save
	 * @param message
	 * 		version control commit message; 
	 * 		if null or blank, the commit will not be performed 
	 * @throws IllegalArgumentException
	 * 		if the ID for the dataset is  not valid, 
	 * 		if there was an error writing data for this dataset to file, or 
	 * 		if there was an error committing the updated file to version control
	 */
	public void new_saveDatasetDataToFile(File datasetFile,
	                                  File sourceFile,
                                      DashboardDatasetData dataset,
            							  String message) throws IOException, IllegalArgumentException {
        File datasetDataDir = datasetFile.getParentFile();
        if ( !datasetDataDir.exists()) {
            if ( !datasetDataDir.mkdirs()) {
                throw new IOException("Failed to create data directory.");
            }
        }
        Files.copy(sourceFile.toPath(), datasetFile.toPath());
        
		// Get the dataset data filename
		String datasetId = dataset.getDatasetId();
		File dataFile = datasetDataFile(datasetId, dataset);
		// Create the directory tree for this file if it does not exist
		File parentFile = dataFile.getParentFile();
		if ( ! parentFile.exists() ) {
			parentFile.mkdirs();
		}
		else {
			// Delete the messages file (for old data) if it exists
			DashboardConfigStore configStore;
			try {
				configStore = DashboardConfigStore.get(false);
			} catch ( IOException ex ) {
				throw new IllegalArgumentException("Unexpected failure to get the dashboard configuration");
			}
			configStore.getCheckerMsgHandler().deleteMsgsFile(datasetId);
		}

//		// Save the data to the data file
//		try ( PrintWriter writer = new PrintWriter(dataFile); ) {
//			// The data column headers
//			String dataline = "";
//			boolean first = true;
//			for ( String name : dataset.getUserColNames() ) {
//				if ( ! first )
//					dataline += TAB;
//				else
//					first = false;
//				dataline += name;
//			}
//			writer.println(dataline);
//			// The data measurements (rows of data)
//			for ( ArrayList<String> datarow : dataset.getDataValues() ) {
//				dataline = "";
//				first = true;
//				for ( String datum : datarow ) {
//					if ( ! first )
//						dataline += TAB;
//					else
//						first = false;
//					dataline += datum;
//				}
//				writer.println(dataline);
//			}
//		} catch ( IOException ex ) {
//			throw new IllegalArgumentException("Problems writing dataset data for " + 
//					datasetId + " to " + dataFile.getPath() + ": " + ex.getMessage());
//		}

		if ( (message == null) || message.trim().isEmpty() )
			return;

		// Submit the updated data file to version control
		try {
			commitVersion(dataFile, message);
		} catch ( Exception ex ) {
			throw new IllegalArgumentException("Problems committing updated dataset data for " + 
							datasetId + ": " + ex.getMessage());
		}
	}

	public void saveDatasetDataToFile(DashboardDatasetData dataset,  
            							  String message) throws IllegalArgumentException {
		// Get the dataset data filename
		String datasetId = dataset.getDatasetId();
		File dataFile = datasetDataFile(datasetId, dataset);
		// Create the directory tree for this file if it does not exist
		File parentFile = dataFile.getParentFile();
		if ( ! parentFile.exists() ) {
			parentFile.mkdirs();
		}
		else {
			// Delete the messages file (for old data) if it exists
			DashboardConfigStore configStore;
			try {
				configStore = DashboardConfigStore.get(false);
			} catch ( IOException ex ) {
				throw new IllegalArgumentException("Unexpected failure to get the dashboard configuration");
			}
			configStore.getCheckerMsgHandler().deleteMsgsFile(datasetId);
		}

		// Save the data to the data file
		try ( PrintWriter writer = new PrintWriter(dataFile); ) {
			// The data column headers
			String dataline = "";
			boolean first = true;
			for ( String name : dataset.getUserColNames() ) {
				if ( ! first )
					dataline += TAB;
				else
					first = false;
				dataline += name;
			}
			writer.println(dataline);
			// The data measurements (rows of data)
			for ( ArrayList<String> datarow : dataset.getDataValues() ) {
				dataline = "";
				first = true;
				for ( String datum : datarow ) {
					if ( ! first )
						dataline += TAB;
					else
						first = false;
					dataline += datum;
				}
				writer.println(dataline);
			}
		} catch ( IOException ex ) {
			throw new IllegalArgumentException("Problems writing dataset data for " + 
					datasetId + " to " + dataFile.getPath() + ": " + ex.getMessage());
		}

		if ( (message == null) || message.trim().isEmpty() )
			return;

		// Submit the updated data file to version control
		try {
			commitVersion(dataFile, message);
		} catch ( Exception ex ) {
			throw new IllegalArgumentException("Problems committing updated dataset data for " + 
							datasetId + ": " + ex.getMessage());
		}
	}

	public File saveArchiveDataFile(DashboardDatasetData datasetData, List<String> columns) 
	        throws IllegalArgumentException, IOException 
	{
		StdUserDataArray stdData = DashboardConfigStore.get(false)
			                            .getDashboardDatasetChecker(datasetData.getFeatureType())
			                            .standardizeDataset(datasetData, null);
		return saveArchiveDataFile(stdData, datasetData, columns);
	}
	
	public File saveArchiveDataFile(StdUserDataArray stdData, DashboardDatasetData datasetData, List<String> columns)  {
		// Get the dataset data filename
		String datasetId = stdData.getDatasetName();
		File dataFile = archiveDataFile(datasetId);
		// Create the directory tree for this file if it does not exist
		File parentFile = dataFile.getParentFile();
		if ( ! parentFile.exists() ) {
			parentFile.mkdirs();
		}
		else {
			// Delete the messages file (for old data) if it exists
			DashboardConfigStore configStore;
			try {
				configStore = DashboardConfigStore.get(false);
			} catch ( IOException ex ) {
				throw new IllegalArgumentException("Unexpected failure to get the dashboard configuration");
			}
			configStore.getCheckerMsgHandler().deleteMsgsFile(datasetId);
		}

		// Save the data to the data file
	    String spacer = "";
		
		try ( PrintWriter writer = new PrintWriter(dataFile); ) {
		    String[] userColumns = stdData.getUserColumnNames();
		    List<DashDataType<?>> dataTypes = stdData.getDataTypes();
		    List<Integer> columnIndexes = new ArrayList<>();
			// The data column headers
			String dataline = "";
			int idx = 0;
			for ( String usrName : userColumns ) {
		        DashDataType<?> colType = dataTypes.get(idx);
			    if ( columns.contains(usrName)) {
			        columnIndexes.add(idx);
				    dataline += spacer;
				    spacer = COMMA;
			        String stdName = colType.getStandardName();
			        if ( stdName.equals(DashboardUtils.STRING_MISSING_VALUE)) {
			            stdName = usrName ;
			        } else if ( colType.hasUnits()) {
			            stdName += " ("+ colType.getUnits().get(0) + ")";
			        }
					dataline += stdName;
			    }
				idx += 1;
			}
			writer.println(dataline);
			// The data measurements (rows of data)
			for (int rowIdx = 0; rowIdx < stdData.getNumSamples(); rowIdx ++ ) {
				spacer = "";
				dataline = "";
				int originalRow = stdData.getOriginalRowIndex(rowIdx);
				if ( originalRow < 0 || originalRow >= stdData.getNumSamples()) {
				    originalRow = 0; // XXX
				}
				for ( Integer colInt : columnIndexes ) {
				    int colIdx = colInt.intValue();
				    DashDataType<?> colType = stdData.getDataTypes().get(colIdx);
				    Object rawValue = stdData.isStandardized(colIdx) ? 
					                    String.valueOf(stdData.getStdVal(rowIdx, colIdx)) :
					                    datasetData.getDataValues().get(originalRow).get(colIdx);
				    String datum = String.valueOf(DashboardUtils.isEmptyNullOrNull(rawValue) ? colType.missingValue() : rawValue);
				    dataline += spacer;
				    spacer = COMMA;
					dataline += datum;
				}
				writer.println(dataline);
			}
		} catch ( IOException ex ) {
			throw new IllegalArgumentException("Problems writing dataset data for " + 
					datasetId + " to " + dataFile.getPath() + ": " + ex.getMessage());
		}

		return dataFile;
	}

	/**
	 * Adds the title of an OME metadata document or a supplemental document 
	 * to the documents list associated with a dataset.
	 * 
	 * @param datasetId
	 * 		add the document to the dataset with this ID
	 * @param addlDoc
	 * 		document to add to the dataset; if an instance of OmeMetadata,
	 * 		this will be added as the OME metadata document for the dataset
	 * 		(just updates omeTimestamp for the dataset), otherwise adds the
	 * 		document as an supplemental document to the dataset (adds the 
	 * 		upload filename and timestamp to addnDocs for the dataset)
	 * @return
	 * 		the updated dataset information
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid,
	 * 		if there are problems accessing the information file for the dataset,
	 * 		if there are problems updating and committing the dataset information,
	 * 		if the filename of the document is the OME filename but 
	 * 		the document is not an instance of OmeMetadata
	 */
	public DashboardDataset addAddlDocTitleToDataset(String datasetId, 
			DashboardMetadata addlDoc) throws IllegalArgumentException {
		DashboardDataset dataset = getDatasetFromInfoFile(datasetId);
		if ( dataset == null )
			throw new IllegalArgumentException("No dataset with the ID " + datasetId);
		String timestamp = addlDoc.getUploadTimestamp();
		
		// XXX TODO: OME_FILENAME check
		if ( addlDoc instanceof DashboardOADSMetadata ) {
			// Assign the OME metadata timestamp for this cruise and save
			if ( ! dataset.getMdTimestamp().equals(timestamp) ) {
				dataset.setMdTimestamp(timestamp);
				saveDatasetInfoToFile(dataset, "Assigned new OME metadata file " +
						"timestamp '" + timestamp + "' to dataset " + datasetId);
			}
		}
		else {
			String uploadFilename = addlDoc.getFilename();
			// XXX TODO: OME_FILENAME check
			if ( MetadataFileHandler.metadataFilename(datasetId).equals(uploadFilename) )
				throw new IllegalArgumentException("Supplemental documents cannot " +
						"have the upload filename of " + MetadataFileHandler.metadataFilename(datasetId));
//			if ( DashboardUtils.PI_OME_FILENAME.equals(uploadFilename) )
//				throw new IllegalArgumentException("Supplemental documents cannot " +
//						"have the upload filename of " + DashboardUtils.PI_OME_FILENAME);
			// Work directly on the additional documents list in the cruise object
			TreeSet<String> addlDocTitles = dataset.getAddlDocs();
			String titleToDelete = null;
			for ( String title : addlDocTitles ) {
				if ( uploadFilename.equals((DashboardMetadata.splitAddlDocsTitle(title))[0]) ) {
					titleToDelete = title;
					break;
				}
			}
			String commitMsg; 
			if ( titleToDelete != null ) {
				addlDocTitles.remove(titleToDelete);
				commitMsg = "Update additional document " + uploadFilename + 
							" (" + timestamp + ") for dataset " + datasetId;
			}
			else {
				commitMsg = "Add additional document " + uploadFilename + 
							" (" + timestamp + ") to dataset " + datasetId;
			}
			addlDocTitles.add(addlDoc.getAddlDocsTitle());
			saveDatasetInfoToFile(dataset, commitMsg);
		}
		return dataset;
	}

	/**
	 * Appropriately renames and modifies the dataset data and info files 
	 * from one cruise/dataset name to another.
	 * 
	 * @param oldName
	 * 		old cruise/dateset name
	 * @param newName
	 * 		new cruise/dataset name
	 * @throws IllegalArgumentException
	 * 		if the data or info file for the old name does not exist, 
	 * 		if a data or info file for the new name already exists, 
	 * 		if the data file has no column associated with the cruise/dataset name, or
	 * 		if unable to rename or update the data or info files
	 */
	public void renameDatasetFiles(String oldName, String newName) throws IllegalArgumentException {
		// Get the dataset IDs for the given names
		String oldId = DashboardServerUtils.getDatasetIDFromName(oldName);
		String newId = DashboardServerUtils.getDatasetIDFromName(newName);
		// Verify old files exist and new files do not
		File oldDataFile = datasetDataFile(oldId);
		if ( ! oldDataFile.exists() ) 
			throw new IllegalArgumentException("Data file for " + oldId + " does not exist");
		File oldInfoFile = datasetInfoFile(oldId, false);
		if ( ! oldInfoFile.exists() )
			throw new IllegalArgumentException("Info file for " + oldId + " does not exist");
		File newDataFile = datasetDataFile(newId);
		if ( newDataFile.exists() )
			throw new IllegalArgumentException("Data file for " + newId + " already exists");
		File newInfoFile = datasetInfoFile(newId, true);
		if ( newInfoFile.exists() )
			throw new IllegalArgumentException("Info file for " + newId + " already exists");

		// Make sure the parent directory for the new files exists
		File parentFile = newDataFile.getParentFile();
		if ( ! parentFile.exists() )
			parentFile.mkdirs();

		// Easiest is to read all the data, modify the cruise/dataset name in the data, 
		// move the files, and save under the new dataset ID
		DashboardDatasetData datasetData = getDatasetDataFromFiles(oldId, 0, -1);
		datasetData.setDatasetId(newId);
		int k = -1;
		int nameIdx = -1;
		for ( DataColumnType type : datasetData.getDataColTypes() ) {
			k++;
			if ( DashboardServerUtils.DATASET_NAME.typeNameEquals(type) ) {
				nameIdx = k;
				break;
			}
		}
		if ( nameIdx < 0 )
			throw new IllegalArgumentException("Unexpected error: no column associated with the cruise/dataset name");
		for ( ArrayList<String> dataVals : datasetData.getDataValues() )
			dataVals.set(k, newName);

		// Move the old dataset files to the new location and name
		String commitMsg = "Rename from " + oldName + " to " + newName;
		try {
			moveVersionedFile(oldDataFile, newDataFile, commitMsg);
			moveVersionedFile(oldInfoFile, newInfoFile, commitMsg);
		} catch (SVNException ex) {
			throw new IllegalArgumentException("Problems renaming the dateaset files from " + 
					oldId + " to " + newId + ": " + ex.getMessage());
		}

		// Save under the new dataset
		saveDatasetInfoToFile(datasetData, commitMsg);
		saveDatasetDataToFile(datasetData, commitMsg);
	}

	/**
	 * Verify a user can overwrite or delete a dataset.  This checks the 
	 * submission state of the dataset as well as ownership of the dataset.  
	 * If not permitted, an IllegalArgumentException is thrown with reason 
	 * for the failure.
	 * 
	 * @param datasetId
	 * 		ID of the data to check
	 * @param username
	 * 		user wanting to overwrite or delete the dataset
	 * @return 
	 * 		the dataset being overwritten or deleted; never null
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid, 
	 * 		if there are problems reading the dataset properties file
	 */
	public DashboardDataset verifyOkayToDeleteDataset(String datasetId, String username)
			throws IllegalArgumentException {
        return verifyOkayToDeleteDataset(getDatasetFromInfoFile(datasetId), username);
	}
    
	public DashboardDataset verifyOkayToDeleteDataset(DashboardDataset dataset, String username) 
			throws IllegalArgumentException {
		// Check if the dataset is in a submitted or published state
		if ( ! Boolean.TRUE.equals(dataset.isEditable()) )
			throw new IllegalArgumentException("dataset status is " + dataset.getSubmitStatus());
		// Check if the user has permission to delete the dataset
		String owner = dataset.getOwner();
		if ( ! ( owner.equals(username) || Users.userManagesOver(username, owner)))
			throw new IllegalArgumentException("Cannot delete dataset. Dataset owner is " + owner);
		return dataset;
	}

	/**
	 * Deletes the information and data files for a dataset 
	 * after verifying the user is permitted to delete this dataset.
	 * 
	 * @param datasetId
	 * 		ID of the dataset to delete
	 * @param username
	 * 		user wanting to delete the dataset
	 * @param deleteMetadata 
	 * 		also delete metadata and additional documents?
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is not valid, 
	 * 		if there were problems access the dataset files, 
	 * 		if the user is not permitted to delete the dataset, or 
	 * 		if there were problems deleting a file or 
	 * 			committing the deletion in version control
	 * @throws IOException 
	 */
	public void deleteDatasetFiles(String datasetId, String username, 
                        			boolean deleteMetadata) throws IllegalArgumentException {
		// Get the dataset information
		DashboardDataset dataset = getDatasetFromInfoFile(datasetId);
		try {
			verifyOkayToDeleteDataset(dataset, username);
		} catch ( IllegalArgumentException ex ) {
			throw new IllegalArgumentException("Not permitted to delete dataset " + 
					datasetId + ": " + ex.getMessage());
		}

		DashboardConfigStore configStore;
		try {
			configStore = DashboardConfigStore.get(false);
		} catch ( IOException ex ) {
			throw new IllegalArgumentException("Unexpected failure to get the dashboard configuration");
		}

		// If they exist, delete the DSG files and notify ERDDAP
		DsgNcFileHandler dsgHandler = configStore.getDsgNcFileHandler();
        try {
    		if ( dsgHandler.deleteCruise(datasetId) )
    			dsgHandler.flagErddap();
        } catch (Exception ex) {
            logger.warn(ex,ex);
        }

        boolean doArchive = ApplicationConfiguration.getProperty("oap.dataset.archive_on_delete", true);
        if ( doArchive ) {
            File archiveRoot = configStore.getContentDir("Attic/" + dataset.getOwner());
            File archiveBase = new File(archiveRoot, dataset.getRecordId());
            File cruiseDir = datasetDataDir(datasetId);
            File cruiseArchive = new File(archiveBase, "CruiseFiles");
            try {
                FileUtils.moveDirectoryToDirectory(cruiseDir, cruiseArchive, true);
                File abbrevDir = cruiseDir.getParentFile();
                boolean abbrevIsEmpty = true;
                for (File f : abbrevDir.listFiles()) {
                    if ( ! f.getName().equals(".svn")) {
                        abbrevIsEmpty = false; 
                        break;
                    }
                }
                if ( abbrevIsEmpty ) {
                    try {
                        FileUtils.forceDelete(abbrevDir);
                    } catch (IOException ex) {
                        logger.warn("Exception deleting data abbrev directory " + abbrevDir.getAbsolutePath(), ex);
                    }
                }
            } catch (IOException ex) {
                logger.warn("Failed to archive dataset cruise dir " + cruiseDir.getAbsolutePath() +
                            " to " + cruiseArchive, ex);
                logger.warn("Forcibly deleting cruise dir : " + cruiseDir.getAbsolutePath());
                try {
                    FileUtils.forceDelete(cruiseDir);
                } catch (IOException ex1) {
                    logger.warn("Failed to forcibly delete dataset cruise dir " + 
                                 cruiseDir.getAbsolutePath(), ex1);
                }
            }
            if ( deleteMetadata ) {
                File metadataDir = configStore.getMetadataFileHandler().getMetadataDirectory(datasetId);
                File metadataArchive = new File(archiveBase, "MetadataDocs");
                try {
                    FileUtils.moveDirectoryToDirectory(metadataDir, metadataArchive, true);
                    File abbrevDir = metadataDir.getParentFile();
                    boolean abbrevIsEmpty = true;
                    for (File f : abbrevDir.listFiles()) {
                        if ( ! f.getName().equals(".svn")) {
                            abbrevIsEmpty = false; 
                            break;
                        }
                    }
                    if ( abbrevIsEmpty ) {
                        try {
                            FileUtils.forceDelete(abbrevDir);
                        } catch (IOException ex) {
                            logger.warn("Exception deleting data abbrev directory " + abbrevDir.getAbsolutePath(), ex);
                        }
                    }
                } catch (IOException ex) {
                    logger.warn("Failed to archive dataset metadata dir " + metadataDir.getAbsolutePath() +
                                " to " + metadataArchive, ex);
                    logger.warn("Forcibly deleting metadata dir : " + metadataDir.getAbsolutePath());
                    try {
                        FileUtils.forceDelete(metadataDir);
                    } catch (IOException ex1) {
                        logger.warn("Failed to forcibly delete dataset metadata dir " + 
                                     metadataDir.getAbsolutePath(), ex1);
                    }
                }
            }
            try {
                zipUp(archiveBase);
                FileUtils.forceDelete(archiveBase);
            } catch (Exception ex) {
                logger.warn("Exception zipping or deleting " + archiveBase.getAbsolutePath(), ex);
            }
        } else {
    		// If it exists, delete the messages file
    		configStore.getCheckerMsgHandler().deleteMsgsFile(datasetId);
    			
    		// Delete the cruise data file
    		String commitMsg = "Cruise file for " + datasetId + " owned by " + 
    				dataset.getOwner() + " deleted by " + username;
            File datasetDataFile = datasetDataFile(datasetId);
    		try {
    			deleteVersionedFile(datasetDataFile, commitMsg);
    		} catch ( SVNException sex ) {
                logger.warn("Exception deleting versioned file: " + sex);
    		} catch ( Exception ex ) {
    			throw new IllegalArgumentException("Problems deleting the cruise data file: " + 
    					ex.getMessage());
    		}
    		// Delete the cruise information file
    		try {
    			deleteVersionedFile(datasetInfoFile(datasetId, true), commitMsg);
    		} catch ( SVNException sex ) {
                logger.warn("Exception deleting versioned file: " + sex);
    		} catch ( Exception ex ) {
    			throw new IllegalArgumentException("Problems deleting the cruise information file: " + 
    					ex.getMessage());
    		}
            File parentDir = datasetDataFile.getParentFile();
            try {
                FileUtils.forceDelete(parentDir);
            } catch (IOException ioex) {
                logger.warn("Failed to delete dataset directory " + parentDir.getPath(), ioex);
            }
            File abbrevDir = parentDir.getParentFile();
            boolean abbrevIsEmpty = true;
            for (File f : abbrevDir.listFiles()) {
                if ( ! f.getName().equals(".svn")) {
                    abbrevIsEmpty = false; 
                    break;
                }
            }
            if ( abbrevIsEmpty ) {
                try {
                    FileUtils.forceDelete(abbrevDir);
                } catch (IOException ex) {
                    logger.warn("Exception deleting data abbrev directory " + abbrevDir.getAbsolutePath(), ex);
                }
            }
    		if ( deleteMetadata ) {
    			// Delete the metadata and additional documents associated with this cruise
    			MetadataFileHandler metadataHandler = configStore.getMetadataFileHandler();
    			try {
    				metadataHandler.deleteAllMetadata(username, datasetId);
    			} catch (Exception ex) {
    				// Ignore - may not exist
    			}
    		}
        }
	}

	/**
     * @param archiveBase
	 * @throws Exception 
     */
    private static void zipUp(File archiveBase) throws Exception {
        File zipFile = new File(archiveBase.getParentFile(), archiveBase.getName()+".zip");
        try ( FileOutputStream fos = new FileOutputStream(zipFile);
              ZipOutputStream zout = new ZipOutputStream(fos); ) {
            zipDir(archiveBase, zout);
        }
    }
    
    /**
     * @param archiveBase
     * @param zout
     * @throws Exception 
     */
    private static void zipDir(File dir, ZipOutputStream zout) throws Exception {
        String basePath = dir.getAbsolutePath();
        basePath = basePath.substring(0, basePath.lastIndexOf(File.separator)+1);
        for (File f : dir.listFiles()) {
            zipFile(f, zout, basePath);
        }
    }

    private static void zipFile(File file, ZipOutputStream zout, String basePath) throws Exception {
        String relativeName = file.getAbsolutePath().substring(basePath.length());
        if ( file.isDirectory()) {
            for (File f : file.listFiles()) {
                zipFile(f, zout, basePath);
            }
        } else {
            try ( FileInputStream fin = new FileInputStream( file )) {
                ZipEntry ze = new ZipEntry(relativeName);
                zout.putNextEntry(ze);
                copy(fin, zout);
                zout.closeEntry();
            }
        }
    }
    
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 10];
        int read = 0;
        while ((read = in.read(buffer)) > 0 ) {
            out.write(buffer, 0, read);
        }
    }

    /**
	 * Assigns a DashboardDataset (or DashboardDatasetData) from the 
	 * dataset properties file.  The ID of the dataset is obtained 
	 * from the DashboardDataset. 
	 * 
	 * @param dataset
	 * 		assign dataset information here
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid, or 
	 * 		if the dataset properties file is invalid
	 * @throws FileNotFoundException
	 * 		if the dataset properties file does not exist
	 * @throws IOException
	 * 		if there are problems reading the dataset properties file
	 */
	private void assignDatasetFromInfoFile(DashboardDataset dataset) 
			throws IllegalArgumentException, FileNotFoundException, IOException {
		// Get the dataset properties file
		File infoFile = datasetInfoFile(dataset.getDatasetId(), true);
		// Get the properties given in this file
		Properties cruiseProps = new Properties();
		
		try ( FileReader infoReader = new FileReader(infoFile); ) {
			cruiseProps.load(infoReader);
		}

		// Assign the DashboardDataset from the values in the properties file

		String value = cruiseProps.getProperty(SUBMISSION_RECORD_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					SUBMISSION_RECORD_ID + " given in " + infoFile.getPath());
		dataset.setRecordId(value);
        
		// Owner of the data file
		value = cruiseProps.getProperty(DATA_OWNER_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					DATA_OWNER_ID + " given in " + infoFile.getPath());
		dataset.setOwner(value);

        // feature type
        value = cruiseProps.getProperty(FEATURE_TYPE_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					FEATURE_TYPE_ID + " given in " + infoFile.getPath());	
		dataset.setFeatureType(value);
        
        // observation type
        value = cruiseProps.getProperty(OBSERVATION_TYPE_ID);
		if ( value == null ) {
//			throw new IllegalArgumentException("No property value for " + 
//					FEATURE_TYPE_ID + " given in " + infoFile.getPath());	
            value = ObservationType.UNSPECIFIED;
		}
		dataset.setUserObservationType(value);
        
        // file type
        value = cruiseProps.getProperty(FILE_TYPE_ID);
		if ( value == null ) {
            value = FileType.UNSPECIFIED.name();
//			throw new IllegalArgumentException("No property value for " + 
//					FILE_TYPE_ID + " given in " + infoFile.getPath());	
		}
		dataset.setFileType(value);
        
		// version 
		value = cruiseProps.getProperty(VERSION_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					VERSION_ID + " given in " + infoFile.getPath());	
		dataset.setVersion(value);

		// Name of uploaded file
		value = cruiseProps.getProperty(UPLOAD_FILENAME_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					UPLOAD_FILENAME_ID + " given in " + infoFile.getPath());			
		dataset.setUploadFilename(value);

		value = cruiseProps.getProperty(USER_DATASET_NAME);
		if ( value == null )
            value = dataset.getUploadFilename();
		dataset.setUserDatasetName(value);
        
		// Time of uploading the file
		value = cruiseProps.getProperty(UPLOAD_TIMESTAMP_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					UPLOAD_TIMESTAMP_ID + " given in " + infoFile.getPath());			
		dataset.setUploadTimestamp(value);

		// Name of uploaded orginal file
		value = cruiseProps.getProperty(UPLOADED_FILE_ID);
		if ( value == null )
			logger.info("No property value for " + UPLOADED_FILE_ID + " given in " + infoFile.getPath());			
		dataset.setUploadedFile(value);
        
		// Data file DOI
		value = cruiseProps.getProperty(DOI_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					DOI_ID + " given in " + infoFile.getPath());			
		dataset.setDoi(value);

		// Data check status
		value = cruiseProps.getProperty(DATA_CHECK_STATUS_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					DATA_CHECK_STATUS_ID + " given in " + infoFile.getPath());			
		dataset.setDataCheckStatus(value);

		// OME metadata timestamp
		value = cruiseProps.getProperty(MD_TIMESTAMP_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					MD_TIMESTAMP_ID + " given in " + infoFile.getPath());			
		dataset.setMdTimestamp(value);

		value = cruiseProps.getProperty(MD_STATUS_ID);
		if ( value == null ) {
            logger.info("No property value for " + MD_STATUS_ID + " given in " + infoFile.getPath());
		    value = DashboardUtils.STRING_MISSING_VALUE;
		}
		dataset.setMdStatus(value);

        // Accession number
		value = cruiseProps.getProperty(ACCESSION_NUM);
        if ( value == null ) { value = STRING_MISSING_VALUE; }
        dataset.setAccession(value);
		        
        // Published URL
		value = cruiseProps.getProperty(PUBLISHED_URL);
        if ( value == null ) { value = STRING_MISSING_VALUE; }
        dataset.setPublishedUrl(value);
		        
		// Metadata documents
		value = cruiseProps.getProperty(ADDL_DOC_TITLES_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					ADDL_DOC_TITLES_ID + " given in " + infoFile.getPath());			
		dataset.setAddlDocs(DashboardUtils.decodeStringArrayList(value));

		// Submit status
		value = cruiseProps.getProperty(SUBMIT_STATUS_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					SUBMIT_STATUS_ID + " given in " + infoFile.getPath());			
		dataset.setSubmitStatus(value);

		// Archive status
		value = cruiseProps.getProperty(ARCHIVE_STATUS_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					ARCHIVE_STATUS_ID + " given in " + infoFile.getPath());			
		dataset.setArchiveStatus(value);

		value = cruiseProps.getProperty(ARCHIVE_MSG_ID);
		if ( value == null ) {
            value = "";
		}
		dataset.setArchiveSubmissionMessage(value);
		
		value = cruiseProps.getProperty(ARCHIVE_GEN_DOI_ID, "false");
		dataset.setArchiveDOIrequested(Boolean.parseBoolean(value));
		
		// Date of request to archive data and metadata
		value = cruiseProps.getProperty(ARCHIVAL_DATE_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					ARCHIVAL_DATE_ID + " given in " + infoFile.getPath());			
		try {
            dataset.setArchiveDate(DashboardServerUtils.getDate(value, DashboardUtils.DATE_ARCHIVE_FORMAT));
        } catch (ParseException ex1) {
            logger.warn("Unable to parse archive date: " + value);
            ex1.printStackTrace();
            dataset.setArchiveDate(null);
        }

		// Number of rows of data (number of samples)
		value = cruiseProps.getProperty(NUM_DATA_ROWS_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					NUM_DATA_ROWS_ID + " given in " + infoFile.getPath());
		try {
			dataset.setNumDataRows(Integer.parseInt(value));
		} catch ( NumberFormatException ex ) {
			throw new IllegalArgumentException(ex);
		}

		// Number of error messages
		value = cruiseProps.getProperty(NUM_ERROR_ROWS_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					NUM_ERROR_ROWS_ID + " given in " + infoFile.getPath());
		try {
			dataset.setNumErrorRows(Integer.parseInt(value));
		} catch ( NumberFormatException ex ) {
			throw new IllegalArgumentException(ex);
		}

		// Number of warning messages
		value = cruiseProps.getProperty(NUM_WARN_ROWS_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					NUM_WARN_ROWS_ID + " given in " + infoFile.getPath());
		try {
			dataset.setNumWarnRows(Integer.parseInt(value));
		} catch ( NumberFormatException ex ) {
			throw new IllegalArgumentException(ex);
		}

		// User-provided data column names
		value = cruiseProps.getProperty(USER_COLUMN_NAMES_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					USER_COLUMN_NAMES_ID + " given in " + infoFile.getPath());
		dataset.setUserColNames(DashboardUtils.decodeStringArrayList(value));
		int numCols =  dataset.getUserColNames().size();

		// Data column type information
		value = cruiseProps.getProperty(DATA_COLUMN_TYPES_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					DATA_COLUMN_TYPES_ID + " given in " + infoFile.getPath());
		ArrayList<String> colTypeNames = DashboardUtils.decodeStringArrayList(value);
		if ( colTypeNames.size() != numCols )
			throw new IllegalArgumentException("number of data column types " +
					"different from number of user column names");
		value = cruiseProps.getProperty(DATA_COLUMN_UNITS_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					DATA_COLUMN_UNITS_ID + " given in " + infoFile.getPath());
		ArrayList<String> colTypeUnits = DashboardUtils.decodeStringArrayList(value);
		if ( colTypeUnits.size() != numCols )
			throw new IllegalArgumentException("number of data column units " +
					"different from number of user column names");
		value = cruiseProps.getProperty(MISSING_VALUES_ID);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					MISSING_VALUES_ID + " given in " + infoFile.getPath());
		ArrayList<String> colMissValues = DashboardUtils.decodeStringArrayList(value);
		if ( colMissValues.size() != numCols )
			throw new IllegalArgumentException("number of data column missing values " +
					"different from number of user column names");

		// Assign the data column types 
		ArrayList<DataColumnType> dataColTypes = new ArrayList<DataColumnType>(numCols);
		for (int k = 0; k < numCols; k++) {
			DashDataType<?> dataType = userTypes.getDataType(colTypeNames.get(k));
			if ( dataType == null )
				throw new IllegalArgumentException("unknown data type \"" + colTypeNames.get(k) + "\"");
			DataColumnType dctype = dataType.dataColumnType();
			if ( ! dctype.setSelectedUnit(colTypeUnits.get(k)) ) {
                String msg = "unknown unit \"" + colTypeUnits.get(k) + 
						"\" for data type \"" + dctype.getVarName() + "\"";
                logger.info(dataset.getRecordId() + ": Failed to set unit of " + colTypeUnits.get(k) + " to " + dctype );
                if ( !"time_of_day".equals(dataType.getVarName())) // XXX temporary until we're sure..
                    throw new IllegalArgumentException(msg);
			}
			dctype.setSelectedMissingValue(colMissValues.get(k));
			dataColTypes.add(dctype);
		}
		dataset.setDataColTypes(dataColTypes);

		value = cruiseProps.getProperty(CHECKER_FLAGS);
		if ( value == null )
			throw new IllegalArgumentException("No property value for " + 
					CHECKER_FLAGS + " given in " + infoFile.getPath());
		TreeSet<QCFlag> checkerFlags = DashboardUtils.decodeQCFlagSet(value);
		dataset.setCheckerFlags(checkerFlags);

		value = cruiseProps.getProperty(USER_FLAGS);
		if ( value == null ) 
			throw new IllegalArgumentException("No property value for " + 
					USER_FLAGS + " given in " + infoFile.getPath());
		TreeSet<QCFlag> userFlags = DashboardUtils.decodeQCFlagSet(value);
		dataset.setUserFlags(userFlags);
	}

	/**
	 * Assigns a DashboardDatasetData with data read from the given buffered
	 * reader.  The data should be tab-separated values.  Empty lines are 
	 * ignored.  The first (non-empty) line should be a header line of data 
	 * column names with units.  The expected number of data columns is 
	 * determined from this line but otherwise is ignored.  The remaining
	 * (non-empty) lines should be data lines with exactly the same number  
	 * of values as there are column names. 
	 * 
	 * @param datasetData
	 * 		assign column names with units and data to this object
	 * @param datasetReader
	 * 		read data from here
	 * @param firstDataRow
	 * 		index of the first data row to return; to return all data 
	 * 		for this dataset, set to zero
	 * @param numDataRows
	 * 		maximum number of data rows to return; if negative, no limit
	 * 		is applied (all remaining data rows are returned)
	 * @throws IOException
	 * 		if reading from datasetReader throws one,
	 * 		if there is a blank data column name with units, 
	 * 		if there is an inconsistent number of data values,
	 * 		if there are too few data columns read
	 */
	private static void assignDataFromInput(DashboardDatasetData datasetData, 
	                                        RecordOrientedFileReader reader,
        	                                int firstDataRow, int numDataRows) throws IOException {
////			BufferedReader datasetReader, int firstDataRow, int numDataRows) throws IOException {
//		// data row numbers
		ArrayList<Integer> rowNums = new ArrayList<Integer>();
//		// data values
		ArrayList<ArrayList<String>> dataValues = new ArrayList<ArrayList<String>>();
//		// Create the parser for the data lines
//		CSVFormat format = CSVFormat.EXCEL.withIgnoreSurroundingSpaces()
//                        				  .withIgnoreEmptyLines()
//                        				  .withDelimiter('\t');
//		
//		try ( CSVParser dataParser = new CSVParser(datasetReader, format); ) {

			int numDataColumns = 0;
			boolean firstLine = true;
			int dataRowNum = 0;
			int rowNum = 0;
			for ( String[] record : reader ) {
                rowNum += 1;
				if ( firstLine ) {
					// Column headers
					numDataColumns = record.length;
					if ( numDataColumns < MIN_NUM_DATA_COLUMNS )
						throw new IOException("Too few data columns (" + numDataColumns + ") read");
					if ( numDataRows == 0 ) {
						// No reading of the data requested - done
						break;
					}
					firstLine = false;
					continue;
				}

				// Data line
				if ( record.length != numDataColumns )
					throw new IOException("Inconsistent number of data columns (" + 
							record.length + " instead of " + numDataColumns + 
							") for measurement " + rowNum + ":\n    " +
							rebuildDataline(record, '\t'));

				dataRowNum++;
				if ( dataRowNum > firstDataRow ) {
					ArrayList<String> datavals = new ArrayList<String>(numDataColumns);
					for ( String val : record )
						datavals.add(val);
					rowNums.add(dataRowNum);
					dataValues.add(datavals);
					if ( (numDataRows > 0) && (dataValues.size() == numDataRows) )
						break;
				}
			}
//		}

		datasetData.setRowNums(rowNums);
		datasetData.setDataValues(dataValues);
	}

	/**
	 * Returns a version of the string that was parsed to create the given record 
	 * but using the given spacer between the entries in the record.
	 * 
	 * @param record
	 * 		record to use
	 * @param spacer
	 * 		spacer to use
	 * @return
	 * 		recreated string for this record
	 */
	 private static String rebuildDataline(String[] record, char spacer) {
         return rebuildDataline(Arrays.asList(record), spacer);
	 }
	 private static String rebuildDataline(Iterable<String> record, char spacer) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for ( String val : record ) {
			if ( ! val.isEmpty() ) {
				if ( first ) {
					first = false;
				}
				else {
					builder.append(spacer);							
				}
				builder.append(val);
			}
		}
		return builder.toString();
	}

}
