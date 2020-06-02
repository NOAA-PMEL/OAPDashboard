/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.FileType;
import gov.noaa.pmel.dashboard.shared.ObservationType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author kamb
 *
 */
@Data
@Builder
@Setter(AccessLevel.NONE)
public class StandardUploadFields {

    @NonNull
    protected Map<String,List<FileItem>> _parameterMap;
    @NonNull
    protected List<FileItem> _dataFiles;
    @Setter(AccessLevel.PUBLIC)
    protected String _username;
    protected String _datasetId;
    protected String _datasetIdColumnName;
    @Setter(AccessLevel.PUBLIC)
    protected String _checkedFileType;
    @Setter(AccessLevel.PUBLIC)
    protected String _uploadFileName;
    @Setter(AccessLevel.PUBLIC)
    protected String _uploadType;
    @Builder.Default
    @Setter(AccessLevel.PUBLIC)
    protected FileType _fileType = FileType.UNSPECIFIED;
    @Builder.Default
    protected FeatureType _featureType = FeatureType.UNSPECIFIED;
    @Builder.Default
    protected String _observationType = ObservationType.UNSPECIFIED;
    @NonNull
    private String _dataAction;
    @Builder.Default
    protected String _timestamp = "";
    @NonNull
    @Builder.Default
    protected String _fileDataEncoding = "UTC-8";
}
