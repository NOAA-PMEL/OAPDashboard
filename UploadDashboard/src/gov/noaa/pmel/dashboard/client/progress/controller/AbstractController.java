package gov.noaa.pmel.dashboard.client.progress.controller;

import com.google.gwt.core.client.GWT;
import gov.noaa.pmel.dashboard.shared.UploadProgressService;
import gov.noaa.pmel.dashboard.shared.UploadProgressServiceAsync;

public abstract class AbstractController {

  protected static final UploadProgressServiceAsync SERVICE = GWT.create(UploadProgressService.class);
}
