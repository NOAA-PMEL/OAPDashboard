/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.tws.util.TimeUtils;
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
    @Builder.Default
    protected FeatureType _featureType = FeatureType.UNSPECIFIED;
    @NonNull
    private String _dataAction;
    @Builder.Default
    protected String _timestamp = TimeUtils.formatUTC(new Date(), TimeUtils.non_std_ISO_8601_nofrac_SPACE_Z);
    @NonNull
    @Builder.Default
    protected String _fileDataEncoding = "UTC-8";
}
