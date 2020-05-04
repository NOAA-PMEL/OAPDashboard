/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.handlers.RawUploadFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DataUploadService;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.FileType;
import gov.noaa.pmel.dashboard.util.FormUtils;
import gov.noaa.pmel.tws.util.Logging;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * @author kamb
 *
 */
//@Data
@Setter(AccessLevel.NONE)
public abstract class FileUploadProcessor {

    private static Logger logger = Logging.getLogger(FileUploadProcessor.class);

    static final String DATASET_ID_FIELD_NAME = "datasetId";
    static final String DATASET_ID_COLUMN_FIELD_NAME = "datasetIdColumn";
    
    protected StandardUploadFields _uploadFields;
    protected DashboardConfigStore _configStore;
    protected RawUploadFileHandler _rawFileHandler;
    protected DataFileHandler _dataFileHandler;
    
    protected ArrayList<String> _messages = new ArrayList<String>();
    protected TreeSet<String> _successes = new TreeSet<String>();

    protected FeatureType _featureType;
    
    protected String action;
    protected String username;
    protected String timestamp;
    protected String encoding;
    protected String dataFormat;
    protected String specifiedDatasetId;
    protected String datasetIdColName;
    protected FileType fileType;
    protected String submissionRecordId;

    protected File _uploadedFile;
    
    protected FileUploadProcessor(StandardUploadFields uploadFields) {
        this._uploadFields = uploadFields;
        this._featureType = uploadFields.featureType();
        action = _uploadFields.dataAction();
        username = _uploadFields.username();
        timestamp = _uploadFields.timestamp();
        encoding = _uploadFields.fileDataEncoding();
        dataFormat = FormUtils.getFormField("dataformat", _uploadFields.parameterMap());
        specifiedDatasetId = FormUtils.getFormField(DATASET_ID_FIELD_NAME, _uploadFields.parameterMap());
        datasetIdColName = FormUtils.getFormField(DATASET_ID_COLUMN_FIELD_NAME, _uploadFields.parameterMap());
        fileType = _uploadFields.fileType();
    }
    
    public void processUpload(String submissionId, File uploadedFile) throws IOException, UploadProcessingException {
        this._uploadedFile = uploadedFile;
        this.submissionRecordId = submissionId;
//        this.specifiedDatasetId = submissionId;
        _configStore = DashboardConfigStore.get(false);
        _dataFileHandler = _configStore.getDataFileHandler();
        
        processUploadedFile();
    }
    
    abstract void processUploadedFile() throws UploadProcessingException;
    
    static Path copyFile(File sourceFile, File destFile, CopyOption... options) throws IOException {
        return Files.copy(sourceFile.toPath(), destFile.toPath(), options);
    }
    protected static void generateEmptyMetadataFile(String datasetId) throws IOException {
        MetadataFileHandler mdf = DashboardConfigStore.get().getMetadataFileHandler();
        mdf.createEmptyOADSMetadataFile(datasetId);
    }
    
    public ArrayList<String> getMessages() {
        return _messages;
    }
    
    public TreeSet<String> getSuccesses() {
        return _successes;
    }
    
    /**
     * @param itemType
     * @param tikaType
     * @return
     */

    public static FileUploadProcessor getProcessor(File uploadFile, StandardUploadFields stdFields) {
        return ( stdFields.fileType().equals(FileType.DELIMITED) ?
                 new NewGeneralizedUploadProcessor(stdFields) :
                 new NewOpaqueFileUploadProcessor(stdFields)
               );
    }

}
