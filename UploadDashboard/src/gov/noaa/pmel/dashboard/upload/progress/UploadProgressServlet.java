package gov.noaa.pmel.dashboard.upload.progress;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.util.List;
import gov.noaa.pmel.dashboard.shared.UploadProgressService;
import gov.noaa.pmel.dashboard.shared.event.Event;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UploadProgressServlet extends RemoteServiceServlet implements UploadProgressService {

    /**
     * Generated
     */
    private static final long serialVersionUID = -6749129505404141552L;

    private static final int EVENT_WAIT = 30 * 1000;
    private static final String PROPERTIES_FILE = "WEB-INF/classes/uploadprogress.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadProgressServlet.class);
    private String uploadDirectory;

    @Override
    public void init() throws ServletException {
        //    Properties properties = new Properties();
        //    try {
        //      properties.load(getServletContext().getResourceAsStream(PROPERTIES_FILE));
        //    } catch (IOException ioe) {
        //      throw new ServletException(ioe);
        //    }

        //    uploadDirectory = properties.getProperty("upload.directory", "target");
        uploadDirectory = ApplicationConfiguration.getProperty("oap.upload.raw_directory", "RawFiles");
    }

    @Override
    public void initialise() {
        HttpSession session = getThreadLocalRequest().getSession(true);
        LOGGER.info("session: " + session);
    }

    //  @Override
    //  public List<FileDto> readFiles(final int page, final int pageSize) {
    //
    //      System.out.println("UploadProgressServlet readFiles");
    //    File[] listFiles = readFiles(this.uploadDirectory);
    //    sortFiles(listFiles);
    //
    //    int firstFile = pageSize * (page - 1);
    //    int lastFile = firstFile + pageSize;
    //
    //    int fileCount = listFiles.length;
    //    if (fileCount < lastFile) {
    //      lastFile = fileCount;
    //    }
    //
    //    if (firstFile < fileCount) {
    //      List<FileDto> files = new ArrayList<FileDto>();
    //
    //      for (int i = firstFile; i < lastFile; i++) {
    //
    //        File file = listFiles[i];
    //        FileDto fileDto = new FileDto();
    //        fileDto.setFilename(file.getName());
    //        fileDto.setDateUploaded(new Date(file.lastModified()));
    //        files.add(fileDto);
    //      }
    //      return files;
    //    } else {
    //      return Collections.EMPTY_LIST;
    //    }
    //  }

    @Override
    public List<Event> getEvents() {

        LOGGER.trace("UploadProgressServlet getEvents");
        HttpSession session = getThreadLocalRequest().getSession();
        UploadProgress uploadProgress = UploadProgress.getUploadProgress(session);

        List<Event> events = null;
        if (null != uploadProgress) {
            if (uploadProgress.isEmpty()) {
                try {
                    synchronized (uploadProgress) {
                        LOGGER.trace("waiting...");
                        uploadProgress.wait(EVENT_WAIT);
                    }
                } catch (final InterruptedException ie) {
                    LOGGER.debug("interrupted...");
                }
            }

            synchronized (uploadProgress) {
                events = uploadProgress.getEvents();
                uploadProgress.clear();
            }
        } else {
            LOGGER.warn("Null UploadProgress for session " + session);
        }

        LOGGER.trace("Events: " + events);
        return events;
    }

    //  @Override
    //  public int countFiles() {
    //    return readFiles(this.uploadDirectory).length;
    //  }
    //  
    //  private File[] readFiles(final String directory) {
    //    File uploadDirectory = new File(directory);
    //    return uploadDirectory.listFiles(new FileFilter() {
    //
    //      @Override
    //      public boolean accept(final File file) {
    //        return null == file ? false : file.isFile();
    //      }
    //    });
    //  }
    //
    //  private void sortFiles(final File[] listFiles) {
    //    Arrays.sort(listFiles, new Comparator<File>() {
    //
    //      @Override
    //      public int compare(final File f1, final File f2) {
    //        return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
    //      }
    //    });
    //  }  
}
