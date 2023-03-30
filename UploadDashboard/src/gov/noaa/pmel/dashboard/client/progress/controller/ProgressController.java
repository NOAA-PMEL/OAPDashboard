package gov.noaa.pmel.dashboard.client.progress.controller;

import com.google.gwt.core.client.GWT;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import gov.noaa.pmel.dashboard.client.progress.state.UploadProgressState;
import gov.noaa.pmel.dashboard.shared.event.Event;
import gov.noaa.pmel.dashboard.shared.event.UploadProgressChangeEvent;

public final class ProgressController extends AbstractController {

    public static final ProgressController INSTANCE = new ProgressController();

    private ProgressController() {
    }

    //  public void findFiles(final int page, final int pageSize) {
    //    SERVICE.readFiles(page, pageSize, new AsyncCallback<List<FileDto>>() {
    //
    //      @Override
    //      public void onFailure(final Throwable t) {
    //        GWT.log("error find files", t);
    //      }
    //
    //      @Override
    //      public void onSuccess(final List<FileDto> files) {
    //        UploadProgressState.INSTANCE.setFiles(files);
    //      }
    //    });
    //  }

    boolean STOPPED = false;

    private void getEvents() {

        GWT.log("PC: Checking events");
        SERVICE.getEvents(new AsyncCallback<List<Event>>() {

            @Override
            public void onFailure(final Throwable t) {
                GWT.log("PC: error get events", t);
            }

            @Override
            public void onSuccess(final List<Event> events) {

                GWT.log("PC: progress success: "+ events);
                for (Event event : events) {
                    handleEvent(event);
                }
                if ( !STOPPED ) {
                    SERVICE.getEvents(this);
                } else {
                    GWT.log("PC: progress checks have been stopped.");
                }
            }

            private void handleEvent(final Event event) {
                GWT.log("PC: handle event: "+ event);
                if (event instanceof UploadProgressChangeEvent) {
                    UploadProgressChangeEvent uploadPercentChangeEvent = (UploadProgressChangeEvent) event;
                    String filename = uploadPercentChangeEvent.getFilename();
                    Integer percentage = uploadPercentChangeEvent.getPercentage();
                    GWT.log("PC: changeEvent: "+ filename + ":"+percentage + "%");

                    UploadProgressState.INSTANCE.setUploadProgress(filename, percentage);
                }
            }
        });
    }

    public void initialise() {
        STOPPED = false;
        SERVICE.initialise(new AsyncCallback<Void>() {

            @Override
            public void onFailure(final Throwable t) {
                GWT.log("error initialise", t);
            }

            @Override
            public void onSuccess(final Void result) {
                getEvents();
            }
        });
    }

    public void stop() {
        GWT.log("PC: Stopping progress checks.");
        STOPPED = true;
    }

    //  public void countFiles() {
    //    SERVICE.countFiles(new AsyncCallback<Integer>() {
    //
    //      @Override
    //      public void onFailure(final Throwable t) {
    //        GWT.log("error count files", t);
    //      }
    //
    //      @Override
    //      public void onSuccess(final Integer result) {
    //        int pageSize = UploadProgressState.INSTANCE.getPageSize();
    //        int pages = (int) Math.ceil((double) result / (double) pageSize);
    //        UploadProgressState.INSTANCE.setPages(pages);
    //      }
    //    });
}
