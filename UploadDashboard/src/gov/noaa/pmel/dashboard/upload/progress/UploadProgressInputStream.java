package gov.noaa.pmel.dashboard.upload.progress;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.ProgressListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.noaa.pmel.dashboard.server.DataUploadService;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;

public final class UploadProgressInputStream extends FilterInputStream {

  private static final Logger LOGGER = LoggerFactory.getLogger(UploadProgressInputStream.class);
  
  private List<ProgressListener> listeners;
  private long bytesRead = 0;
  private long totalBytes = 0;
  private long maxBytes = -1;
  private boolean failOnOversize = true;

  public UploadProgressInputStream(final InputStream in, final long totalBytes) {
    super(in);
    this.totalBytes = totalBytes;
    listeners = new ArrayList<ProgressListener>();
  }

  public UploadProgressInputStream(final InputStream in, final long totalBytes, 
                                   final long maxUploadSize) {
      this(in, totalBytes);
      this.maxBytes = maxUploadSize;
  }
  public UploadProgressInputStream(final InputStream in, final long totalBytes, 
                                   final long maxUploadSize, boolean failOnOversize) {
      this(in, totalBytes, maxUploadSize);
      this.failOnOversize = failOnOversize;
  }
  
  public void addListener(final ProgressListener listener) {
    listeners.add(listener);
  }

  @Override
  public int read() throws IOException {
    int b = super.read();

    this.bytesRead++;
    if ( maxBytes > 0 ) {
        if ( this.bytesRead > maxBytes ) {
            if ( failOnOversize ) {
                throw new IllegalStateException("Max upload size exceeded.\nMax size: " + DataUploadService.getMaxUploadSizeDisplayStr());
            } else {
                LOGGER.warn("Max upload size exceeded. Max size: " + DataUploadService.getMaxUploadSize() + 
                            " (" + DataUploadService.getMaxUploadSizeDisplayStr() + ") from: " + 
                            String.valueOf(ApplicationConfiguration.getConfigurationProperty("oap.upload.max_size")));
            }
        }
    }

    updateListeners(bytesRead, totalBytes);

    return b;
  }

  @Override
  public int read(final byte b[]) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(final byte b[], final int off, final int len) throws IOException {
    int bytesReadNow = in.read(b, off, len);

    this.bytesRead = this.bytesRead + bytesReadNow;
    if ( maxBytes > 0 ) {
        if ( this.bytesRead > maxBytes ) {
            if ( failOnOversize ) {
                throw new IllegalStateException("Max upload size exceeded.\nMax size: " + DataUploadService.getMaxUploadSizeDisplayStr());
            } else {
                LOGGER.warn("Max upload size exceeded. Max size: " + DataUploadService.getMaxUploadSize() + 
                            " (" + DataUploadService.getMaxUploadSizeDisplayStr() + ") from: " + 
                            String.valueOf(ApplicationConfiguration.getConfigurationProperty("oap.upload.max_size")));
            }
        }
    }
    updateListeners(this.bytesRead, totalBytes);

    return bytesReadNow;
  }

  @Override
  public void close() throws IOException {
    super.close();

    updateListeners(totalBytes, totalBytes);
  }

  private void updateListeners(final long bytesRead, final long totalBytes) {

    for (ProgressListener listener : listeners) {

      listener.update(bytesRead, totalBytes, listeners.size());
    }
  }
}
