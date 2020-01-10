/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.handlers.RawUploadFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.FileType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
public abstract class FileUploadProcessor {

    protected static final String DATASET_ID_COLUMN_FIELD_NAME = "datasetIdColumn";
    
    protected StandardUploadFields _uploadFields;
    protected DashboardConfigStore _configStore;
    protected RawUploadFileHandler _rawFileHandler;
    
    protected ArrayList<String> _messages = new ArrayList<String>();
    protected TreeSet<String> _successes = new TreeSet<String>();

    protected FeatureType _featureType;
    protected FileType _fileType;
    
    protected FileUploadProcessor(StandardUploadFields uploadFields) {
        this._uploadFields = uploadFields;
        this._featureType = uploadFields.featureType();
        this._fileType = uploadFields.fileType();
    }
    
    public void processUpload() throws IOException, UploadProcessingException {
        _configStore = DashboardConfigStore.get(true);
		_rawFileHandler = _configStore.getRawUploadFileHandler();
        List<FileItem> files = _uploadFields.dataFiles();
        doFeatureSpecificProcessing(files);
    }
    
    protected abstract void doFeatureSpecificProcessing(List<FileItem> fileItems) throws UploadProcessingException;
    
    protected void doOnEntryGeneralProcessing(FileItem file) {
        // save raw upload files
    }
    
    protected void doOnExitGeneralProcessing(FileItem file) {
    }
    
    protected void saveRawFile(FileItem item) throws Exception {
        File targetDir = _rawFileHandler.createUploadTargetDir(_uploadFields.username());
        System.out.println("Saving raw " + item.getName());
        _rawFileHandler.writeItem(item, targetDir);
    }
    
    protected void generateEmptyMetadataFile(String datasetId) throws IOException {
        MetadataFileHandler mdf = DashboardConfigStore.get().getMetadataFileHandler();
        mdf.createEmptyOADSMetadataFile(datasetId);
    }
    
    public ArrayList<String> getMessages() {
        return _messages;
    }
    
    public TreeSet<String> getSuccesses() {
        return _successes;
    }
}
