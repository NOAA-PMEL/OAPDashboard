/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import gov.noaa.ncei.oads.xml.v_a0_2_2.OadsMetadataDocumentType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonContactInfoType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonNameType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonType;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.handlers.RawUploadFileHandler;
import gov.noaa.pmel.dashboard.oads.OADSMetadata;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.Users;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.FileType;
import gov.noaa.pmel.dashboard.util.FormUtils;
import gov.noaa.pmel.oads.util.TimeUtils;
import gov.noaa.pmel.tws.util.Logging;
import lombok.AccessLevel;
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
    protected String observationType;

    protected File _uploadedFile;
    
    protected FileUploadProcessor(StandardUploadFields uploadFields) {
        this._uploadFields = uploadFields;
        this._featureType = uploadFields.featureType();
        observationType = uploadFields.observationType();
        action = _uploadFields.dataAction();
        username = _uploadFields.username();
        timestamp = _uploadFields.timestamp();
        encoding = _uploadFields.fileDataEncoding();
        dataFormat = FormUtils.getFormField("dataformat", _uploadFields.parameterMap());
        specifiedDatasetId = FormUtils.getFormField(DATASET_ID_FIELD_NAME, _uploadFields.parameterMap());
        datasetIdColName = FormUtils.getFormField(DATASET_ID_COLUMN_FIELD_NAME, _uploadFields.parameterMap());
        fileType = _uploadFields.fileType();
    }
    
    public void processUpload(String submissionId, boolean isUpdateRequest, File uploadedFile) throws IOException, UploadProcessingException {
        this._uploadedFile = uploadedFile;
        this.submissionRecordId = submissionId;
//        this.specifiedDatasetId = submissionId;
        _configStore = DashboardConfigStore.get(false);
        _dataFileHandler = _configStore.getDataFileHandler();
        
        processUploadedFile(isUpdateRequest);
        if ( ! isUpdateRequest ) {
            generateInitialMetadataFile(submissionId);
        }
    }
    
    abstract void processUploadedFile(boolean isUpdateRequest) throws UploadProcessingException;
    
    static Path copyFile(File sourceFile, File destFile, CopyOption... options) throws IOException {
        return Files.copy(sourceFile.toPath(), destFile.toPath(), options);
    }
    private void generateInitialMetadataFile(String datasetId) throws IOException {
        MetadataFileHandler mdf = DashboardConfigStore.get(false).getMetadataFileHandler();
//        mdf.createInitialOADSMetadataFile(datasetId, username);
        // pulling in core from MDF for now.
        File mdataFile = mdf.getMetadataFile(datasetId);
        OadsMetadataDocumentType mdDoc = new OadsMetadataDocumentType();
        try {
            User dataSubmitter = Users.getUser(username);
            if ( dataSubmitter == null ) {
                throw new IllegalStateException("No user found for userid " + username);
            }
            PersonType dsPerson = PersonType.builder()
                    .name(PersonNameType.builder()
                          .first(dataSubmitter.firstName())
                          .middle(dataSubmitter.middle())
                          .last(dataSubmitter.lastName())
                          .build())
                    .addOrganization(dataSubmitter.organization())
                    .contactInfo(PersonContactInfoType.builder()
                                 .email(dataSubmitter.email())
                                 .phone(dataSubmitter.telephoneString())
                                 .build())
                    .build();
            mdDoc.setDataSubmitter(dsPerson);
            OADSMetadata.writeNewOadsXml(mdataFile, mdDoc);
            DataFileHandler dfh = DashboardConfigStore.get(false).getDataFileHandler();
            DashboardDataset dd = dfh.getDatasetFromInfoFile(datasetId);
            dd.setMdStatus("Initial Metadata");
            dd.setMdTimestamp(TimeUtils.formatUTC(new Date(), "yyyy-MM-dd HH:mm Z"));
            dfh.saveDatasetInfoToFile(dd, "Initial Metadata");
        } catch (Exception ex) {
            logger.warn("Exception creating initial metadata document for submission " + 
                         datasetId + " for user " + username, ex);
        }
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
        return ( ! stdFields.featureType().equals(FeatureType.OTHER) 
                 && stdFields.fileType().equals(FileType.DELIMITED) ?
                 new NewGeneralizedUploadProcessor(stdFields) :
                 new NewOpaqueFileUploadProcessor(stdFields)
               );
    }

}
