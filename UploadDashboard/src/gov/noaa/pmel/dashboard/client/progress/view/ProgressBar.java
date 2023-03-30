package gov.noaa.pmel.dashboard.client.progress.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public final class ProgressBar extends SimplePanel {

    private static final double COMPLETE_PERECENTAGE = 100d;
    private static final double START_PERECENTAGE = 0d;
    private Panel progress;
    private int lastChange = -1;

    public ProgressBar() {

        setStyleName("ProgressBar");

        progress = new SimplePanel();
        progress.setStyleName("progress");
        progress.setWidth("0px");

        add(progress);
    }

    public void update(final int percentage) {
        GWT.log("PB: update bar:" + percentage);
        if (percentage < START_PERECENTAGE || percentage > COMPLETE_PERECENTAGE) {
            throw new IllegalArgumentException("invalid value for percentage");
        }

        int decorationWidth = progress.getAbsoluteLeft() - getAbsoluteLeft();

        int barWidth = getParent().getParent().getOffsetWidth(); // XXX Always?  This is changed from original...
        int progressWidth = (int) (((barWidth - (decorationWidth * 2)) / COMPLETE_PERECENTAGE) * percentage);
        GWT.log("Wprog: " + progressWidth + ", Wdeco: "+decorationWidth+", Wbar: " + barWidth ); // + ", pWidth: "+ Wparent);

        if ( percentage == 100) {
            if ( progressWidth != barWidth) {
                GWT.log("Bad complete! : Wprog: " + progressWidth + ", Wdeco: "+decorationWidth+", Wbar: " + barWidth ); // + ", pWidth: "+ Wparent);
            }
            progressWidth = barWidth;
        }
        if ( progressWidth < lastChange ) {
            GWT.log("WARNIG: Ignoring backward progress! : " + progressWidth);
        } else {
            GWT.log("PB: set width:" + progressWidth);
            progress.setWidth(progressWidth + "px");
        }
    }
}
