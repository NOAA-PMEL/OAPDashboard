package gov.noaa.pmel.dashboard.upload.progress;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.noaa.pmel.dashboard.shared.event.*;

public final class UploadProgress {

  private static final Logger LOGGER = LoggerFactory.getLogger(UploadProgressInputStream.class);
  
  private static final String SESSION_KEY = "uploadProgress";
  private List<Event> events = new ArrayList<Event>();

  private UploadProgress() {
  }

  public List<Event> getEvents() {

    return events;
  }

  public void add(final Event event) {
    events.add(event);
  }

  public void clear() {
    events = new ArrayList<Event>();
  }

  public boolean isEmpty() {
    return events.isEmpty();
  }

  public static UploadProgress getUploadProgress(final HttpSession session) {
    LOGGER.trace(new Date() + " GetUploadProgress : " + session);
    Object attribute = session.getAttribute(SESSION_KEY);
    if (null == attribute) {
      attribute = new UploadProgress();
      session.setAttribute(SESSION_KEY, attribute);
    }

    return (UploadProgress) attribute;
//    return null == attribute ? null : (UploadProgress) attribute;
  }
}
