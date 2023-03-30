package gov.noaa.pmel.dashboard.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import gov.noaa.pmel.dashboard.shared.dto.FileDto;
import gov.noaa.pmel.dashboard.shared.event.Event;

public interface UploadProgressServiceAsync {

  void initialise(AsyncCallback<Void> asyncCallback);

//  void countFiles(AsyncCallback<Integer> asyncCallback);
//
//  void readFiles(int page, int pageSize, AsyncCallback<List<FileDto>> asyncCallback);

  void getEvents(AsyncCallback<List<Event>> asyncCallback);
}
