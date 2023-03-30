package gov.noaa.pmel.dashboard.client.progress.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import gov.noaa.pmel.dashboard.shared.dto.FileDto;

public final class UploadProgressState extends AbstractState {

  public static final UploadProgressState INSTANCE = new UploadProgressState();
  private Map<String, Integer> uploadProgress;
//  private List<FileDto> files;

  private UploadProgressState() {
    uploadProgress = new HashMap<String, Integer>();
  }

//  public List<FileDto> getFiles() {
//    return files;
//  }

//  public void setFiles(final List<FileDto> files) {
//    List<FileDto> old = this.files;
//    this.files = files;
//    firePropertyChange("files", old, files);
//  }

  public Integer getUploadProgress(final String filename) {
    return uploadProgress.get(filename);
  }

  public void clear() {
      uploadProgress.clear();
  }
  
  public void setUploadProgress(final String filename, final Integer percentage) {
    Integer old = this.uploadProgress.get(filename);
    uploadProgress.put(filename, percentage);
    firePropertyChange("uploadProgress", old, uploadProgress);
  }
}
