/**
 * 
 */
package gov.noaa.pmel.dashboard.upload;

import org.apache.tomcat.util.http.fileupload.FileItem;

import gov.noaa.pmel.dashboard.shared.DashboardDataset;

/**
 * @author kamb
 *
 */
public class OpaqueDataset extends DashboardDataset {

    private static final long serialVersionUID = -920654005579432209L;

    private FileItem _fileItem;
    
    public OpaqueDataset(String datasetId) {
        super();
        super.setDatasetId(datasetId);
    }

    public void setFileItem(FileItem item) {
        _fileItem = item;
    }

    public FileItem getFileItem() {
        return _fileItem;
    }

}
