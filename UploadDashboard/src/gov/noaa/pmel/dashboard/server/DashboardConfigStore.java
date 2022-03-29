/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.googlecode.gwt.crypto.client.TripleDesCipher;

import gov.noaa.pmel.dashboard.actions.DatasetChecker;
import gov.noaa.pmel.dashboard.actions.DatasetSubmitter;
import gov.noaa.pmel.dashboard.actions.checker.MinimalDatasetChecker;
import gov.noaa.pmel.dashboard.actions.checker.OpaqueDatasetChecker;
import gov.noaa.pmel.dashboard.actions.checker.ProfileDatasetChecker;
import gov.noaa.pmel.dashboard.actions.checker.TimeseriesProfileDatasetChecker;
import gov.noaa.pmel.dashboard.actions.checker.TrajectoryDatasetChecker;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.ferret.FerretConfig;
import gov.noaa.pmel.dashboard.handlers.ArchiveFilesBundler;
import gov.noaa.pmel.dashboard.handlers.CheckerMessageHandler;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.DsgNcFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.handlers.PreviewPlotsHandler;
import gov.noaa.pmel.dashboard.handlers.RawUploadFileHandler;
import gov.noaa.pmel.dashboard.handlers.UserFileHandler;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FeatureType;

/**
 * Reads and holds the Dashboard configuration details
 * 
 * @author Karl Smith
 */
public class DashboardConfigStore {

    private static Logger logger = LogManager.getLogger(DashboardConfigStore.class.getName());
           
           
//	public static class UPLOAD_DASHBOARD_SERVER_NAME not foundUPLOAD_DASHBOARD_SERVER_NAME not foundPropertyNotFoundException extends Exception {
//		private static final long serialVersionUID = -7623805430454642262L;
//		public PropertyNotFoundException() { super(); }
//		public PropertyNotFoundException(String message, Throwable cause) { super(message, cause); }
//		public PropertyNotFoundException(String message) { super(message); }
//	}
		
    public static final String METADATA_EDITOR_URL = "metadata_editor.url";
    public static final String METADATA_EDITOR_POST_ENDPOINT = "metadata_editor.post.url";
    
	private static final String ENCRYPTION_KEY_NAME_TAG = "EncryptionKey";
	private static final String ENCRYPTION_SALT_NAME_TAG = "EncryptionSalt";
	private static final String UPLOAD_VERSION_NAME_TAG = "UploadVersion";
	private static final String QC_VERSION_NAME_TAG = "QCVersion";
	private static final String SVN_USER_NAME_TAG = "SVNUsername";
	private static final String SVN_PASSWORD_NAME_TAG = "SVNPassword";
	private static final String USER_FILES_DIR_NAME_TAG = "UserFilesDir";
	private static final String DATA_FILES_DIR_NAME_TAG = "DataFilesDir";
	private static final String RAW_UPLOAD_FILES_DIR_NAME_TAG = "UploadFilesDir";
	private static final String METADATA_FILES_DIR_NAME_TAG = "MetadataFilesDir";
	private static final String DSG_NC_FILES_DIR_NAME_TAG = "DsgNcFilesDir";
	private static final String DEC_DSG_NC_FILES_DIR_NAME_TAG = "DecDsgNcFilesDir";
	private static final String ARCHIVE_BUNDLES_DIR_NAME_TAG = "ArchiveBundlesDir";
	private static final String ARCHIVE_BUNDLES_EMAIL_ADDRESS_TAG = "ArchiveBundlesEmailAddress";
	private static final String CC_BUNDLES_EMAIL_ADDRESS_TAG = "CCBundlesEmailAddress";
	private static final String SMTP_HOST_ADDRESS_TAG = "SMTPHostAddress";
	private static final String SMTP_HOST_PORT_TAG = "SMTPHostPort";
	private static final String SMTP_USERNAME_TAG = "SMTPUsername";
	private static final String SMTP_PASSWORD_TAG = "SMTPPassword";
	private static final String ERDDAP_DSG_FLAG_FILE_NAME_TAG = "ErddapDsgFlagFile";
	private static final String USER_TYPES_PROPS_FILE_TAG = "UserTypesFile";
	private static final String METADATA_TYPES_PROPS_FILE_TAG = "MetadataTypesFile";
	private static final String DATA_TYPES_PROPS_FILE_TAG = "DataTypesFile";
	private static final String COLUMN_NAME_TYPE_FILE_TAG = "ColumnNameTypeFile";
	private static final String FERRET_CONFIG_FILE_NAME_TAG = "FerretConfigFile";
	private static final String DATABASE_CONFIG_FILE_NAME_TAG = "DatabaseConfigFile";

	private static final String CONFIG_FILE_INFO_MSG = 
			"This configuration file should look something like: \n" +
			"# ------------------------------ \n" +
			ENCRYPTION_KEY_NAME_TAG + "=[ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, " +
					"13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 ] \n" +
			ENCRYPTION_SALT_NAME_TAG + "=SomeArbitraryStringOfCharacters \n" +
			UPLOAD_VERSION_NAME_TAG + "=SomeVersionNumber \n" +
			QC_VERSION_NAME_TAG + "=SomeVersionNumber \n" +
			SVN_USER_NAME_TAG + "=SVNUsername \n" +
			SVN_PASSWORD_NAME_TAG + "=SVNPasswork \n" +
			USER_FILES_DIR_NAME_TAG + "=/Some/SVN/Work/Dir/For/User/Data \n" +
			DATA_FILES_DIR_NAME_TAG + "=/Some/SVN/Work/Dir/For/Data/Files \n" +
			METADATA_FILES_DIR_NAME_TAG + "=/Some/SVN/Work/Dir/For/Metadata/Docs \n" +
			DSG_NC_FILES_DIR_NAME_TAG + "=/Some/Plain/Dir/For/NetCDF/DSG/Files \n" +
			DEC_DSG_NC_FILES_DIR_NAME_TAG + "=/Some/Plain/Dir/For/NetCDF/Decimated/DSG/Files \n" +
			ARCHIVE_BUNDLES_DIR_NAME_TAG + "=/Some/SVN/Work/Dir/For/Archive/Bundles \n" + 
			ARCHIVE_BUNDLES_EMAIL_ADDRESS_TAG + "=archiver@gdac.org \n" +
			CC_BUNDLES_EMAIL_ADDRESS_TAG + "=dashboard@my.group.org \n" +
			SMTP_HOST_ADDRESS_TAG + "=smtp.server.for.dashboard \n" +
			SMTP_HOST_PORT_TAG + "=smtp.server.port.number \n" +
			SMTP_USERNAME_TAG + "=username.for.smtp \n" +
			SMTP_PASSWORD_TAG + "=password.for.smtp \n" +
			ERDDAP_DSG_FLAG_FILE_NAME_TAG + "=/Some/ERDDAP/Flag/Filename/For/DSG/Update \n" +
			USER_TYPES_PROPS_FILE_TAG + "=/Path/To/User/Uploaded/Data/Types/PropsFile \n" +
			METADATA_TYPES_PROPS_FILE_TAG + "=/Path/To/File/Metadata/Types/PropsFile \n" +
			DATA_TYPES_PROPS_FILE_TAG + "=/Path/To/File/Data/Types/PropsFile \n" +
			COLUMN_NAME_TYPE_FILE_TAG + "=/Path/To/Column/Name/To/Type/PropsFile \n" +
			FERRET_CONFIG_FILE_NAME_TAG + "=/Path/To/FerretConfig/XMLFile \n" +
			DATABASE_CONFIG_FILE_NAME_TAG + "=/Path/To/DatabaseConfig/PropsFile \n" + 
			"# ------------------------------ \n" +
			"The EncryptionKey should be 24 random integer values in [-128,127] \n" +
			"The hexidecimal keys for users can be generated using the mkpasshash.sh script. \n";

	private static final Object SINGLETON_SYNC_OBJECT = new Object();
	private static DashboardConfigStore singleton = null;

	private TripleDesCipher cipher;
	private String uploadVersion;
	private String qcVersion;
	private UserFileHandler userFileHandler;
	private DataFileHandler dataFileHandler;
	private RawUploadFileHandler rawUploadFileHandler;
	private CheckerMessageHandler checkerMsgHandler;
	private MetadataFileHandler metadataFileHandler;
	private ArchiveFilesBundler archiveFilesBundler;
	private DsgNcFileHandler dsgNcFileHandler;
	private FerretConfig ferretConf;
	private OpaqueDatasetChecker opaqueChecker;
	private ProfileDatasetChecker profileChecker;
	private TrajectoryDatasetChecker trajectoryChecker;
    private TimeseriesProfileDatasetChecker timeseriesProfileChecker;
	private MinimalDatasetChecker minimalDatasetChecker;
	private PreviewPlotsHandler plotsHandler;
	private DatasetSubmitter datasetSubmitter;
	private KnownDataTypes knownUserDataTypes;
	private KnownDataTypes knownMetadataTypes;
	private KnownDataTypes knownDataFileTypes;

    private static File _baseDir;
    private static File _configDir;
    private static File _configFile;
    private static File _appContentDir;
    private static String _serverAppName;
	private static Properties _configProps;
	private HashSet<File> filesToWatch;
	private Thread watcherThread;
	private WatchService watcher;
	private boolean needToRestart;

	private static File getBaseDir() throws RuntimeException {
	    if ( _baseDir == null ) {
    		String baseDir = tryProperty("OA_DOCUMENT_ROOT");
    		if ( baseDir == null ) {
    			baseDir = tryProperty("CATALINA_BASE");
    		}
    		if ( baseDir == null ) {
    			baseDir = tryProperty("CATALINA_HOME");
    		}
    		if ( baseDir == null ) {
    			System.out.println("*** ENV:\n"+System.getenv());
    			System.out.println("*** PROPS:\n"+System.getProperties());
    			throw new RuntimeException("Document config root not found.");
    		}
    		if ( ! baseDir.endsWith(File.separator)) {
    			baseDir += File.separator;
    		}
            _baseDir = new File(baseDir);
            if ( !_baseDir.exists()) {
                throw new RuntimeException(_baseDir.getPath());
            }
	    }
		return _baseDir;
	}
	
    public static File getWebappDir() {
        File webappDir;
        String serverAppName;
        File webAppSubDir = new File(DashboardConfigStore.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        do {
            webAppSubDir = webAppSubDir.getParentFile();
            serverAppName = webAppSubDir.getName();
        } while ( ! serverAppName.equals("WEB-INF") );
        webAppSubDir = webAppSubDir.getParentFile();
        String dirName = webAppSubDir.getAbsolutePath();
        try {
            dirName = URLDecoder.decode(dirName, "utf8");
        } catch (Exception ex) {
            logger.warn(ex,ex);
            dirName = webAppSubDir.getAbsolutePath();
        }
        webappDir = new File(dirName);
        return webappDir;
    }
    
    public static File getContentDir(String dirname) throws RuntimeException {
        File appContentDir = getAppContentDir();
        File theDir = new File(appContentDir, dirname);
        if ( !theDir.exists()) {
            if ( !theDir.mkdirs()) {
                throw new RuntimeException("Unable to create content dir " + theDir.getAbsolutePath());
            }
        }
        return theDir;
    }
    
    
    public static File getAppContentDir() throws RuntimeException {
        if ( _appContentDir == null ) {
            File baseDir = getBaseDir();
    		String serverAppName = getServerAppName();
            String appContentDirPath = baseDir.getPath() + File.separator +  "content" + File.separator + serverAppName + File.separator;
            _appContentDir = new File(appContentDirPath);
        }
        return _appContentDir;
    }
    
    public static String getServerAppName() {
        
        if ( _serverAppName == null ) {
    		// First check is UPLOAD_DASHBOARD_SERVER_NAME is defined for alternate configurations 
    		// when running the dashboard.program.* applications
    		String serverAppName = tryProperty("UPLOAD_DASHBOARD_SERVER_NAME");
    		if ( serverAppName == null ) {
    			// Get the app name from the location of this class source in tomcat;
    			// e.g., "/home/users/tomcat/webapps/SocatUploadDashboard/WEB-INF/classes/gov/noaa/pmel/dashboard/server/DashboardConfigStore.class"
    			try {
    				File webAppSubDir = new File(DashboardConfigStore.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    				do {
    					webAppSubDir = webAppSubDir.getParentFile();
    					serverAppName = webAppSubDir.getName();
    				} while ( ! serverAppName.equals("WEB-INF") );
    				webAppSubDir = webAppSubDir.getParentFile();
    				serverAppName = webAppSubDir.getName();
                    if ( "war".equals(serverAppName )) { // running in dev
                        serverAppName = null;
                    }
    			} catch ( Exception ex ) {
    				serverAppName = "";
    			}
    			if ( serverAppName == null || serverAppName.isEmpty() )
                    serverAppName = "OAPUploadDashboard";
    		}
            _serverAppName = serverAppName;
        }
        return _serverAppName;
    }
    
    /**
     * @return
     * @throws IOException 
     */
    private static File getConfigDir() throws IOException {
        if ( _configDir == null ) {
            File contentDir = getAppContentDir();
    		File appConfigDir = new File(contentDir, "config");
    		if ( !appConfigDir.exists() || !appConfigDir.isDirectory() || !appConfigDir.canRead()) {
    			throw new IllegalStateException("Problem with app config dir: " + appConfigDir.getAbsoluteFile());
    		}
            _configDir = appConfigDir;
        }
        return _configDir;
    }
    
//    		// Configure the log4j2 logger
//    		System.setProperty("log4j.configurationFile", appConfigDir.getPath() + "/log4j2.properties");
//            System.out.println("log4j.configurationFile: " + System.getProperty("log4j.configurationFile"));
//            System.out.println("logger: " + logger);
//            logger.warn("Warn level test");
//            logger.info("Info level test");
//            logger.debug("Debug level test");
    
    public static File getConfigFile() throws IOException {
        if ( _configFile == null ) {
    
            File appConfigDir = getConfigDir();
            String serverAppName = getServerAppName();
    		// Read the properties from the standard configuration file
    		_configFile = new File(appConfigDir, serverAppName + ".properties");
        }
        return _configFile;
    }
        
	private static String tryProperty(String propName) { 
		String propVal = System.getenv(propName);
		if ( propVal == null ) {
			propVal = System.getProperty(propName);
		}
		if ( propVal == null ) {
			logger.debug("trying property " + propName + " not found");
		} else {
			logger.debug("found property " + propName+":"+ propVal);
		}
		return propVal;
	}

	/**
	 * Creates a data store initialized from the contents of the standard 
	 * configuration file.  See the contents of {@link #CONFIG_FILE_INFO_MSG} 
	 * for information on the configuration file format.
	 * 
	 * Do not create an instance of this class; 
	 * instead use {@link #get()} to retrieve the singleton instance
	 * 
	 * @param startMonitors
	 * 		start the file change monitors? 
	 * @throws IOException 
	 * 		if unable to read the standard configuration file
	 */
	private DashboardConfigStore(boolean startMonitors) throws Exception {
        _configFile = getConfigFile();
        filesToWatch = new HashSet<>();
		filesToWatch.add(_configFile);
    	_configProps = new Properties();
		try ( FileReader reader = new FileReader(_configFile); ) {
			_configProps.load(reader);
		} catch ( Exception ex ) {
			throw new IOException("Problems reading " + _configFile.getPath() +
					"\n" + ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}
		String propVal;

		// Read the encryption key from the data store and initialize the cipher with it
		try {
			propVal = _configProps.getProperty(ENCRYPTION_KEY_NAME_TAG);
			if ( propVal == null )
				throw new IllegalArgumentException("value not defined");
			byte[] encryptionKey = DashboardUtils.decodeByteArray(propVal.trim());
			if ( (encryptionKey.length < 16) || (encryptionKey.length > 24) )
				throw new IllegalArgumentException(
						"array must have 16 to 24 values");
			cipher = new TripleDesCipher();
			cipher.setKey(encryptionKey);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + ENCRYPTION_KEY_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}

		// Read the SOCAT versions
		try {
			propVal = _configProps.getProperty(UPLOAD_VERSION_NAME_TAG);
			if ( propVal == null )
				throw new IllegalArgumentException("value not defined");
			propVal = propVal.trim();
			if ( propVal.isEmpty() )
				throw new IllegalArgumentException("blank value");
			uploadVersion = propVal;
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + UPLOAD_VERSION_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}
		try {
			propVal = _configProps.getProperty(QC_VERSION_NAME_TAG);
			if ( propVal == null )
				throw new IllegalArgumentException("value not defined");
			propVal = propVal.trim();
			if ( propVal.isEmpty() )
				throw new IllegalArgumentException("blank value");
			qcVersion = propVal;
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + QC_VERSION_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}

		// Read the SVN username
		String svnUsername;
		try {
			propVal = _configProps.getProperty(SVN_USER_NAME_TAG);
			if ( propVal == null )
				throw new IllegalArgumentException("value not defined");
			propVal = propVal.trim();
			if ( propVal.isEmpty() )
				throw new IllegalArgumentException("blank value");
			svnUsername = propVal;
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + SVN_USER_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}
		
		if ( !startMonitors ) { svnUsername = null; }

		// Read the SVN password; can be blank or not given
		String svnPassword = "";
		propVal = _configProps.getProperty(SVN_PASSWORD_NAME_TAG);
		if ( propVal != null )
			svnPassword = propVal.trim();

        File baseDir = getBaseDir();
        File appConfigDir = getConfigDir();
        String serverAppName = getServerAppName();
		try {
			propVal = getFilePathProperty(_configProps, USER_TYPES_PROPS_FILE_TAG, appConfigDir);
			File userTypesFile = new File(propVal);
			filesToWatch.add(userTypesFile);
			Properties typeProps = new Properties();
			try ( FileReader propsReader = new FileReader(propVal); ) {
				typeProps.load(propsReader);
			}
			knownUserDataTypes = new KnownDataTypes();
			knownUserDataTypes.addStandardTypesForUsers();
			knownUserDataTypes.addTypesFromProperties(typeProps);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + USER_TYPES_PROPS_FILE_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}
		if ( logger.isTraceEnabled() ) {
			logger.info("Known user-provided data types: ");
			TreeSet<DashDataType<?>> knownTypes = knownUserDataTypes.getKnownTypesSet();
			for ( DashDataType<?> dtype : knownTypes )
				logger.trace("    " + dtype.getVarName() + "=" + dtype.toPropertyValue());			
		}

		try {
			propVal = getFilePathProperty(_configProps, METADATA_TYPES_PROPS_FILE_TAG, appConfigDir);
			if ( propVal == null )
				throw new IllegalArgumentException("value not defined");
			propVal = propVal.trim();
			Properties typeProps = new Properties();
			try ( FileReader propsReader = new FileReader(propVal); ) {
				typeProps.load(propsReader);
			}
			knownMetadataTypes = new KnownDataTypes();
			knownMetadataTypes.addStandardTypesForMetadataFiles();
			knownMetadataTypes.addTypesFromProperties(typeProps);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + METADATA_TYPES_PROPS_FILE_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}
		if ( logger.isTraceEnabled() ) {
			logger.info("Known file metadata types: ");
			TreeSet<DashDataType<?>> knownTypes = knownMetadataTypes.getKnownTypesSet();
			for ( DashDataType<?> dtype : knownTypes )
				logger.trace("    " + dtype.getVarName() + "=" + dtype.toPropertyValue());			
		}

		try {
			propVal = getFilePathProperty(_configProps, DATA_TYPES_PROPS_FILE_TAG, appConfigDir);
			Properties typeProps = new Properties();
			try ( FileReader propsReader = new FileReader(propVal); ) {
				typeProps.load(propsReader);
			}
			knownDataFileTypes = new KnownDataTypes();
			knownDataFileTypes.addStandardTypesForDataFiles();
			knownDataFileTypes.addTypesFromProperties(typeProps);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + DATA_TYPES_PROPS_FILE_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}
		if ( logger.isInfoEnabled() ) {
			logger.info("Known file data types: ");
			TreeSet<DashDataType<?>> knownTypes = knownDataFileTypes.getKnownTypesSet();
			for ( DashDataType<?> dtype : knownTypes )
				logger.info("    " + dtype.getVarName() + "=" + dtype.toPropertyValue());			
		}

		// Read the default column names to types with units properties file
		String colNamesToTypesFilename;
		try {
			colNamesToTypesFilename = getFilePathProperty(_configProps, COLUMN_NAME_TYPE_FILE_TAG, appConfigDir);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + COLUMN_NAME_TYPE_FILE_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}

		// Read the user files directory name
		try {
			propVal = getFilePathProperty(_configProps, USER_FILES_DIR_NAME_TAG, appConfigDir);
			userFileHandler = new UserFileHandler(propVal, startMonitors ? svnUsername : null, 
					svnPassword, colNamesToTypesFilename, knownUserDataTypes);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + USER_FILES_DIR_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}

		// Read the cruise files directory name
		try {
			propVal = getFilePathProperty(_configProps, DATA_FILES_DIR_NAME_TAG, appConfigDir);
			dataFileHandler = new DataFileHandler(propVal, svnUsername, 
					svnPassword, knownUserDataTypes);
			// Put SanityChecker message files in the same directory
			checkerMsgHandler = new CheckerMessageHandler(propVal);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + DATA_FILES_DIR_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}
		
		// Read the raw upload files directory name
		try {
			propVal = getFilePathProperty(_configProps, RAW_UPLOAD_FILES_DIR_NAME_TAG, appConfigDir);
			rawUploadFileHandler = new RawUploadFileHandler(propVal, svnUsername, svnPassword);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + RAW_UPLOAD_FILES_DIR_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}

		// Read the metadata files directory name
		try {
			propVal = getFilePathProperty(_configProps, METADATA_FILES_DIR_NAME_TAG, appConfigDir);
			metadataFileHandler = new MetadataFileHandler(propVal, svnUsername, svnPassword);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + METADATA_FILES_DIR_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}

		// Read the email addresses to send archival bundles
		propVal = _configProps.getProperty(ARCHIVE_BUNDLES_EMAIL_ADDRESS_TAG);
		if ( propVal == null )
			throw new IOException("Invalid " + ARCHIVE_BUNDLES_EMAIL_ADDRESS_TAG + 
					" value specified in " + _configFile.getPath() + 
					"\nvalue not defined\n" + CONFIG_FILE_INFO_MSG);
		String[] toEmailAddresses = propVal.trim().split(",");
		// Read the email addresses to be cc'd on the archival email
		propVal = _configProps.getProperty(CC_BUNDLES_EMAIL_ADDRESS_TAG);
		if ( propVal == null )
			throw new IOException("Invalid " + CC_BUNDLES_EMAIL_ADDRESS_TAG + 
					" value specified in " + _configFile.getPath() + 
					"\nvalue not defined\n" + CONFIG_FILE_INFO_MSG);
		String[] ccEmailAddresses = propVal.trim().split(",");
		// Read the SMTP server information
		propVal = _configProps.getProperty(SMTP_HOST_ADDRESS_TAG);
		if ( propVal == null )
			throw new IOException("Invalid " + SMTP_HOST_ADDRESS_TAG + 
					" value specified in " + _configFile.getPath() + 
					"\nvalue not defined\n" + CONFIG_FILE_INFO_MSG);
		String smtpHostAddress = propVal.trim();
		propVal = _configProps.getProperty(SMTP_HOST_PORT_TAG);
		if ( propVal == null )
			throw new IOException("Invalid " + SMTP_HOST_PORT_TAG + 
					" value specified in " + _configFile.getPath() + 
					"\nvalue not defined\n" + CONFIG_FILE_INFO_MSG);
		String smtpHostPort = propVal.trim();
		propVal = _configProps.getProperty(SMTP_USERNAME_TAG);
		if ( propVal == null )
			throw new IOException("Invalid " + SMTP_USERNAME_TAG + 
					" value specified in " + _configFile.getPath() + 
					"\nvalue not defined\n" + CONFIG_FILE_INFO_MSG);
		String smtpUsername = propVal.trim();
		propVal = _configProps.getProperty(SMTP_PASSWORD_TAG);
		if ( propVal == null )
			throw new IOException("Invalid " + SMTP_PASSWORD_TAG + 
					" value specified in " + _configFile.getPath() + 
					"\nvalue not defined\n" + CONFIG_FILE_INFO_MSG);
		String smtpPassword = propVal.trim();
		// Read the CDIAC bundles directory name and create the CDIAC archival bundler
		try {
			propVal = getFilePathProperty(_configProps, ARCHIVE_BUNDLES_DIR_NAME_TAG, appConfigDir);
			archiveFilesBundler = new ArchiveFilesBundler(propVal, svnUsername, 
					svnPassword, toEmailAddresses, ccEmailAddresses, 
					smtpHostAddress, smtpHostPort, smtpUsername, smtpPassword, false);
			logger.info("Archive files bundler and mailer using:");
			logger.info("    bundles directory: " + propVal);
			String emails = toEmailAddresses[0];
			for (int k = 1; k < toEmailAddresses.length; k++)
				emails += ", " + toEmailAddresses[k];
			logger.info("    To: " + emails);
			emails = ccEmailAddresses[0];
			for (int k = 1; k < ccEmailAddresses.length; k++)
				emails += ", " + ccEmailAddresses[k];
			logger.info("    CC: " + emails);
			logger.info("    SMTP host: " + smtpHostAddress);
			logger.info("    SMTP port: " + smtpHostPort);
			logger.info("    SMTP username: " + smtpUsername);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + ARCHIVE_BUNDLES_DIR_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}

		// Read the Ferret configuration filename
		try {
			propVal = getFilePathProperty(_configProps, FERRET_CONFIG_FILE_NAME_TAG, appConfigDir);
			// Read the Ferret configuration given in this file
			File ferretPropsFile = new File(propVal);
			filesToWatch.add(ferretPropsFile);
		    try ( InputStream stream = new FileInputStream(ferretPropsFile); ) {
			    SAXBuilder sb = new SAXBuilder();
		    	Document jdom = sb.build(stream);
		    	ferretConf = new FerretConfig();
		    	ferretConf.setRootElement((Element)jdom.getRootElement().clone());
		    }
		    logger.info("read Ferret configuration file " + propVal);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + FERRET_CONFIG_FILE_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}

		// Read the DSG files directory names and ERDDAP flag file names
		String dsgFileDirName;
		try {
			dsgFileDirName = getFilePathProperty(_configProps, DSG_NC_FILES_DIR_NAME_TAG, appConfigDir);
		    logger.info("DSG directory = " + dsgFileDirName);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + DSG_NC_FILES_DIR_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}
		String erddapDsgFlagFileName;
		try {
			erddapDsgFlagFileName = getFilePathProperty(_configProps, ERDDAP_DSG_FLAG_FILE_NAME_TAG, appConfigDir);
		    logger.info("ERDDAP DSG flag file = " + erddapDsgFlagFileName);
		} catch ( Exception ex ) {
			throw new IOException("Invalid " + ERDDAP_DSG_FLAG_FILE_NAME_TAG + 
					" value specified in " + _configFile.getPath() + "\n" + 
					ex.getMessage() + "\n" + CONFIG_FILE_INFO_MSG);
		}
		try {
			dsgNcFileHandler = new DsgNcFileHandler(dsgFileDirName, erddapDsgFlagFileName, ferretConf, 
			                                        knownUserDataTypes, knownMetadataTypes, knownDataFileTypes);
		} catch ( Exception ex ) {
			throw new IOException(ex);
		}

		// SanityChecker initialization from this same properties file 
		opaqueChecker = new OpaqueDatasetChecker();
		profileChecker = new ProfileDatasetChecker(knownUserDataTypes, checkerMsgHandler);
		trajectoryChecker = new TrajectoryDatasetChecker(knownUserDataTypes, checkerMsgHandler);
        timeseriesProfileChecker = new TimeseriesProfileDatasetChecker(knownUserDataTypes, checkerMsgHandler);
		minimalDatasetChecker = new MinimalDatasetChecker(knownUserDataTypes, checkerMsgHandler);

		String previewDirname = getPreviewDirName(baseDir, serverAppName);

		// The PreviewPlotsHandler uses the various handlers just created
		plotsHandler = new PreviewPlotsHandler(previewDirname + "/dsgfiles", 
				previewDirname + "/plots", this);

		// Create the OME XML to PDF generator
//		omePdfGenerator = new OmePdfGenerator(appConfigDir, 
//				metadataFileHandler, dataFileHandler);

		// The DatasetSubmitter uses the various handlers just created
		datasetSubmitter = new DatasetSubmitter(this);

		logger.info("read configuration file " + _configFile.getPath());
		watcher = null;
		watcherThread = null;
		needToRestart = false;
		if ( startMonitors ) {
			// Watch for changes to the configuration file
			watchConfigFiles();
		}
	}

	private static String getPreviewDirName(File baseDir, String serverAppName) {
		
		String dirname = tryProperty("PREVIEW_DIR");
		if ( dirname == null ) {
            File contextDir = getWebappDir();
            try {
                dirname = new File(contextDir, "preview").getCanonicalPath();
            } catch (Exception ex) {
                logger.warn(ex.toString());
    			dirname = baseDir.getPath() + File.separator + "webapps" + File.separator + serverAppName + File.separator + "preview" + File.separator;
            }
		}
		return dirname;
	}

	private static String getFilePathProperty(Properties configProps, String propKey, File baseDir) throws IOException {
		String fPath = configProps.getProperty(propKey);
		if ( fPath == null ) {
			throw new IllegalArgumentException("Property value not defined for key: " + propKey);
		}
		return getFileProperty(fPath, baseDir);
	}

	private static String getFileProperty(String propVal, File baseDir) throws IOException {
		if ( propVal == null || propVal.trim().length() == 0 ) {
			throw new IllegalArgumentException("Empty or null file path specifier");
		}
		String path = propVal.trim();
		if ( path.startsWith("/")) {
			return path;
		} else {
			File parentDir = baseDir;
			File propFile = new File(parentDir, propVal);
			return propFile.getCanonicalPath();
//			while ( parentDir != null && path.length() > 0 && path.startsWith("../")) {
//				parentDir = parentDir.getParentFile();
//				path = path.substring(3);
//			}
//			if ( parentDir != null ) {
//				return parentDir.getAbsolutePath() + File.separator + path;
//			} else {
//				throw new IllegalArgumentException("Problem with relative path: " + propVal + " from " + baseDir.getAbsolutePath());
//			}
		}
	}

	/**
	 * @param startMonitors
	 * 		start the file change monitors? 
	 * 		(ignored if the singleton instance of the DashboardConfigStore already exists)
	 * @return
	 * 		the singleton instance of the DashboardConfigStore
	 * @throws IOException 
	 * 		if unable to read the standard configuration file
	 */
	public static DashboardConfigStore get(boolean startMonitors) throws IOException {
		synchronized(SINGLETON_SYNC_OBJECT) {
			if ( (singleton != null) && 
				 (singleton.needToRestart || (startMonitors && singleton.watcherThread == null ))) {
				singleton.stopMonitors();
				singleton = null;
			}
			if ( singleton == null ) {
				try {
					singleton = new DashboardConfigStore(startMonitors);
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new IOException(ex);
				}
			}
		}
		return singleton;
	}

    /**
     * @return
     * @throws IOException 
     */
    public static DashboardConfigStore get() throws IOException {
        return get(false);
    }

	/**
	 * Shuts down the handlers and monitors associated with the current singleton 
	 * data store and removes it as the singleton instance of this class.
	 */
	public static void shutdown() {
		synchronized(SINGLETON_SYNC_OBJECT) {
			if ( singleton != null ) {
				// stop the handler and monitors for the singleton instance
				singleton.stopMonitors();
				// Discard this DashboardConfigStore as the singleton instance
				singleton = null;
			}
		}
	}

	/**
	 * Shuts down the handlers and monitors associated with this data store.
	 */
	private void stopMonitors() {
		// Shutdown all the VersionsedFileHandlers
		userFileHandler.shutdown();
		dataFileHandler.shutdown();
		metadataFileHandler.shutdown();
		archiveFilesBundler.shutdown();
		// Stop the configuration watcher
		cancelWatch();
	}
	
	/**
	 * Monitors the configuration files for the current DashboardConfigStore 
	 * singleton object.  If a configuration file has changed, sets 
	 * needsToRestart to true and the monitoring thread exits.
	 */
	private void watchConfigFiles() {
		// Make sure the watcher is not already running
		if ( watcherThread != null )
			return;
		watcherThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// Create a new watch service for the dashboard configuration files
				try {
					watcher = FileSystems.getDefault().newWatchService();
				} catch (Exception ex) {
					logger.error("Unexpected error starting a watcher for the default file system", ex);
					return;
				}
				// Register the the directories containing the dashboard configuration files with the watch service
				HashSet<File> parentDirs = new HashSet<File>();
				for ( File configFile : filesToWatch ) {
					parentDirs.add(configFile.getParentFile());
				}
				ArrayList<WatchKey> registrations = new ArrayList<WatchKey>(parentDirs.size());
				for ( File watchDir : parentDirs ) {
					try {
						registrations.add(watchDir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY));
					} catch (Exception ex) {
						logger.error("Unexpected error registering " + watchDir.getPath() + " for watching", ex);
						for ( WatchKey reg : registrations ) {
							reg.cancel();
							reg.pollEvents();
						}
						try {
							watcher.close();
						} catch (Exception e) {
							;
						}
						watcher = null;
						return;
					}
				}
				for (;;) {
					try {
						WatchKey key = watcher.take();
						Path parentPath = (Path) key.watchable();
						for ( WatchEvent<?> event : key.pollEvents() ) {
							Path relPath = (Path) event.context();
							File thisFile = parentPath.resolve(relPath).toFile();
							if ( filesToWatch.contains(thisFile) ) {
								needToRestart = true;
								throw new Exception();
							}
						}
						if ( ! key.reset() )
							break;
					} catch (Exception ex) {
						// Probably the watcher was closed
						break;
					}
				}
				for ( WatchKey reg : registrations ) {
					reg.cancel();
					reg.pollEvents();
				}
				try {
					watcher.close();
				} catch (Exception ex) {
					;
				}
				watcher = null;
				return;
			}
		});
		logger.info("Starting new thread monitoring the dashboard configuration files");
		watcherThread.start();
	}

	/**
	 * Stops the monitoring the dashboard configuration files.  
	 * If the dashboard configuration files are not being monitored, this call does nothing. 
	 */
	private void cancelWatch() {
		try {
			if ( watcher != null ) {
				watcher.close();
			}
			// Only the thread modifies the value of watcher
		} catch (Exception ex) {
			// Might be NullPointerException
		}
		if ( watcherThread != null ) {
			try {
				watcherThread.join();
			} catch (Exception ex) {
				;
			}
			watcherThread = null;
			logger.info("End of thread monitoring the dashboard configuration files");
		}
	}

	/**
	 * @return
	 * 		the version for uploaded data; never null
	 */
	public String getUploadVersion() {
		return uploadVersion;
	}

	/**
	 * @return
	 * 		the version for QC flagging; never null
	 */
	public String getQCVersion() {
		return qcVersion;
	}

	/**
	 * @return 
	 * 		the handler for user data files
	 */
	public UserFileHandler getUserFileHandler() {
		return userFileHandler;
	}

	/**
	 * @return 
	 * 		the handler for cruise data files
	 */
	public DataFileHandler getDataFileHandler() {
		return dataFileHandler;
	}

	/**
	 * @return
	 * 		the handler for the raw upload files
	 */
	public RawUploadFileHandler getRawUploadFileHandler() {
		return rawUploadFileHandler;
	}

	/**
	 * @return
	 * 		the handler for SanityChecker messages
	 */
	public CheckerMessageHandler getCheckerMsgHandler() {
		return checkerMsgHandler;
	}

	/**
	 * @return
	 * 		the handler for cruise metadata documents
	 */
	public MetadataFileHandler getMetadataFileHandler() {
		return metadataFileHandler;
	}

	/**
	 * @return
	 * 		the handler for NetCDF DSG files
	 */
	public DsgNcFileHandler getDsgNcFileHandler() {
		return dsgNcFileHandler;
	}

	/**
	 * @return
	 * 		the Ferret configuration
	 */
	public FerretConfig getFerretConfig() {
		return ferretConf;
	}

	/**
	 * @param featureType The FeatureType of the dataset to be checked.
	 * @return
	 * 		the checker for the type of dataset data and metadata
	 */
	public DatasetChecker getDashboardDatasetChecker(FeatureType featureType) {
		DatasetChecker datasetChecker;
        switch (featureType) {
            case PROFILE:
                datasetChecker = profileChecker;
                break;
            case TRAJECTORY:
                datasetChecker = trajectoryChecker;
                break;
            case TIMESERIES_PROFILE:
                datasetChecker = timeseriesProfileChecker;
                break;
            case TIMESERIES:
            case TRAJECTORY_PROFILE:
            case OTHER:
            default:
                datasetChecker = minimalDatasetChecker;
//                throw new IllegalStateException("No checker available for observation type: " + featureType.name());
        }
		return datasetChecker;
	}

	/**
	 * @return
	 * 		the preview plots handler
	 */
	public PreviewPlotsHandler getPreviewPlotsHandler() {
		return plotsHandler;
	}

	/**
	 * @return
	 * 		the submitter for dashboard datasets
	 */
	public DatasetSubmitter getDashboardDatasetSubmitter() {
		return datasetSubmitter;
	}

	/**
	 * @return
	 * 		the files bundler for archiving datasets
	 */
	public ArchiveFilesBundler getArchiveFilesBundler() {
		return archiveFilesBundler;
	}

	/**
	 * @return
	 * 		the OME XML to PDF generator
	public OmePdfGenerator getOmePdfGenerator() {
		return omePdfGenerator;
	}
	 */

	/**
	 * @return
	 * 		the known user data column types
	 */
	public KnownDataTypes getKnownUserDataTypes() {
		return this.knownUserDataTypes;
	}

	/**
	 * @return
	 * 		the known metadata types in DSG files
	 */
	public KnownDataTypes getKnownMetadataTypes() {
		return this.knownMetadataTypes;
	}

	/**
	 * @return
	 * 		the known data types in DSG files
	 */
	public KnownDataTypes getKnownDataFileTypes() {
		return this.knownDataFileTypes;
	}

	private static File tmpDir;
	public static File getTempDir() {
	    if ( tmpDir == null ) {
	        tmpDir = new File("/var/tmp/oap"); // XXX
	        if ( !tmpDir.exists()) {
	            tmpDir.mkdir();
	        }
	        if ( ! ( tmpDir.exists() && tmpDir.canWrite())) {
	            throw new IllegalStateException("Unable to create or write to temp dir:"+tmpDir.getAbsolutePath());
	        }
	    }
	    return tmpDir;
	}
}
