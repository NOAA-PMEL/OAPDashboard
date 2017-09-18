/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author kamb
 *
 */
public class MetadataPreviewInfo implements Serializable, IsSerializable {
	
	private static final long serialVersionUID = -7362924830027941621L;

	private FileInfo metadataFileInfo;
	
	private String metadataPreview;

	public MetadataPreviewInfo() {}
	
	public MetadataPreviewInfo(FileInfo metadataFileInfo, String metadata) {
		super();
		this.metadataFileInfo = metadataFileInfo;
		this.metadataPreview = metadata;
	}

	public FileInfo getMetadataFileInfo() {
		return metadataFileInfo;
	}

	public void setMetadataFileInfo(FileInfo metadataFileInfo) {
		this.metadataFileInfo = metadataFileInfo;
	}

	public String getMetadataPreview() {
		return metadataPreview;
	}

	public void setMetadataPreview(String metadata) {
		this.metadataPreview = metadata;
	}

	
}
