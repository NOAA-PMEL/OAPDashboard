package gov.noaa.pmel.dashboard.shared.event;

import java.io.Serializable;

public final class UploadProgressChangeEvent implements Event, Serializable {

  private String filename;
  private Integer percentage;

  public UploadProgressChangeEvent() {
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(final String filename) {
    this.filename = filename;
  }

  public Integer getPercentage() {
    return percentage;
  }

  public void setPercentage(final Integer percentage) {
    this.percentage = percentage;
  }

  @Override
  public String toString() {
    return filename + " - " + percentage;
  }
}
