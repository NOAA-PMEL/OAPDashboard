/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author kamb
 *
 */
public class ApplicationFooter extends Composite {

    interface ApplicationFooterUiBinder extends UiBinder<Widget, ApplicationFooter> {
    }

    private static ApplicationFooterUiBinder uiBinder = GWT.create(ApplicationFooterUiBinder.class);

    public ApplicationFooter() {
        initWidget(uiBinder.createAndBindUi(this));
    }
    
//    @UiField Label appBuildDisplay;
//    public void setBuildVersion(String version) {
//        appBuildDisplay.setText(version);
//    }

}
