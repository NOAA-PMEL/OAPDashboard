package gov.noaa.pmel.dashboard.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import gov.noaa.pmel.dashboard.shared.dto.FileDto;
import gov.noaa.pmel.dashboard.shared.event.Event;

@RemoteServiceRelativePath("uploadprogress")
public interface UploadProgressService extends RemoteService {

  void initialise();

//  int countFiles();
//
//  List<FileDto> readFiles(int page, int pageSize);

  List<Event> getEvents();
}
