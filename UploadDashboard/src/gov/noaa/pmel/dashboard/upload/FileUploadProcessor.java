/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.handlers.RawUploadFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
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

    protected StandardUploadFields uploadFields;
    protected DashboardConfigStore configStore;
    protected RawUploadFileHandler _rawFileHandler;
    
    protected ArrayList<String> messages = new ArrayList<String>();

    protected FileUploadProcessor(StandardUploadFields uploadFields) {
        this.uploadFields = uploadFields;
    }
    
    public void processUpload() throws IOException, UploadProcessingException {
        configStore = DashboardConfigStore.get(true);
		_rawFileHandler = configStore.getRawUploadFileHandler();
        List<FileItem> files = uploadFields.dataFiles();
        doFeatureSpecificProcessing(files);
    }
    
    protected abstract void doFeatureSpecificProcessing(List<FileItem> fileItems) throws UploadProcessingException;
    
    protected void doOnEntryGeneralProcessing(FileItem file) {
        // save raw upload files
    }
    
    protected void doOnExitGeneralProcessing(FileItem file) {
    }
    
    protected void saveRawFile(FileItem item) throws Exception {
        File targetDir = _rawFileHandler.createUploadTargetDir(uploadFields.username());
        System.out.println("Saving raw " + item.getName());
        _rawFileHandler.writeItem(item, targetDir);
    }
    
    protected void generateEmptyMetadataFile(String datasetId) throws IOException {
        MetadataFileHandler mdf = DashboardConfigStore.get().getMetadataFileHandler();
        mdf.createEmptyOADSMetadataFile(datasetId);
    }
    
    public ArrayList<String> getMessages() {
        return messages;
    }
}
