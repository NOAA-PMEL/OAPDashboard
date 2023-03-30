package gov.noaa.pmel.dashboard.client.progress.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import java.util.HashMap;
import java.util.Map;

import gov.noaa.pmel.dashboard.client.progress.controller.ProgressController;
import gov.noaa.pmel.dashboard.client.progress.state.UploadProgressState;

public final class UploadProgress extends Composite {

    private Panel panel;
    private Map<String, UploadPanel> uploads;

    public UploadProgress() {

        panel = new VerticalPanel();
        panel.setStyleName("UploadProgress");
        uploads = new HashMap<String, UploadPanel>();

        this.initWidget(panel);

        UploadProgressState.INSTANCE.addPropertyChangeListener("uploadProgress", new UploadProgressListener());
    }

    public void reset() {
        GWT.log("reset UploadProgress");
        for (UploadPanel progressPanel : uploads.values()) {
            GWT.log("removing panel " + progressPanel);
            if ( ! panel.remove(progressPanel)) {
                GWT.log("FAILED to remove panel : " + progressPanel);
            }
        }
        uploads.clear();
    }
    private final class UploadProgressListener implements PropertyChangeListener {

        private static final int COMPLETE_PERECENTAGE = 100;
        private static final int REMOVE_DELAY = 3000;

        @Override
        public void propertyChange(final PropertyChangeEvent event) {

            GWT.log("UP: property change:" + event);
            Map<String, Integer> uploadPercentage = (Map<String, Integer>) event.getNewValue();
            GWT.log("UP: percentages:" + uploadPercentage);

            for (Map.Entry<String, Integer> entry : uploadPercentage.entrySet()) {
                String file = entry.getKey();
                Integer percentage = entry.getValue();
                GWT.log("UP: entry:" + file+":"+percentage);
                final UploadPanel uploadPanel;
                if (!uploads.containsKey(file)) {
                    GWT.log("UP: creating progress for : " + file);
                    uploadPanel = new UploadPanel(file);
                    GWT.log("UP: adding panel : " + uploadPanel);
                    uploads.put(file, uploadPanel);
                    panel.add(uploadPanel);
                } else {
                    uploadPanel = uploads.get(file);
                }

                uploadPanel.update(percentage);

                if (percentage == COMPLETE_PERECENTAGE) {
                    GWT.log("UP: progress complete. Stopping checks.");
                    ProgressController.INSTANCE.stop();
//                    GWT.log("UP: schedule remove progress bar for : " + file );
//                    Timer timer = new Timer() {
//                        @Override
//                        public void run() {
//                            GWT.log("UP: (NOT) removing progress bar for : " + file );
//                            //              panel.remove(uploadPanel);
//                        }
//                    };
//                    timer.schedule(REMOVE_DELAY);
                }
            }
        }
    }

    private static final class UploadPanel extends HorizontalPanel {

        private ProgressBar bar;
        //    private Label label;

        public UploadPanel(final String file) {

            setStyleName("UploadPanel");

            bar = new ProgressBar();
            //      label = new Label(file);

            add(bar);
            //      add(label);
        }

        public void update(final int percentage) {
            bar.update(percentage);
        }
    }
}
