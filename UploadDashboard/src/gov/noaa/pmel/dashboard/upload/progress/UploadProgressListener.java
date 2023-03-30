package gov.noaa.pmel.dashboard.upload.progress;

import org.apache.tomcat.util.http.fileupload.ProgressListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.noaa.pmel.dashboard.shared.event.UploadProgressChangeEvent;

public final class UploadProgressListener implements ProgressListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadProgressListener.class);
    
    private static final double COMPLETE_PERECENTAGE = 100d;
    private int percentage = -1;
    private String fileName;
    private UploadProgress uploadProgress;

    public UploadProgressListener(final String fileName, final UploadProgress uploadProgress) {
        this.fileName = fileName;
        this.uploadProgress = uploadProgress;
    }


    long lastReport = 0;
    long reportInterval = 500000;

    @Override
    public void update(final long bytesRead, final long totalBytes, final int items) {

        if ( lastReport == 0 ) {
            LOGGER.info("Starting upload progress " + bytesRead + " of " + totalBytes + " for file " + fileName + " of " + items + " items");
            //          new Exception().printStackTrace(); // XXX To see how this is being called.
            reportInterval = figureReportInterval(totalBytes);
            lastReport = bytesRead;
        } else {
            long check = bytesRead - lastReport;
            if ( check >= reportInterval ) {
                LOGGER.trace(">>>> " + check + " uploaded " + bytesRead + " of " + 
                        totalBytes + " for file " + fileName + " of " + items + " items");
                lastReport = bytesRead;
            }
        }
        int currentPercentage = (int) Math.floor(((double) bytesRead / (double) totalBytes) * COMPLETE_PERECENTAGE);

        if (this.percentage == currentPercentage) {
            return;
        }

        this.percentage = currentPercentage;

        UploadProgressChangeEvent event = new UploadProgressChangeEvent();
        event.setFilename(this.fileName);
        event.setPercentage(currentPercentage);

        synchronized (this.uploadProgress) {
            this.uploadProgress.add(event);
            this.uploadProgress.notifyAll();
        }
    }

    /**
     * Figure out an appropriate report interval.
     * 
     * @param totalBytes
     * @return
     */
    private static long figureReportInterval(long totalBytes) {
        long interval = totalBytes / 20;
        System.out.println("Report interval " + interval + " for " + totalBytes);
        return interval;
    }
}
