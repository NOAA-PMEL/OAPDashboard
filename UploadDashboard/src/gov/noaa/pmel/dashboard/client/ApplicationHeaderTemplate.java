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
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author kamb
 *
 */
public class ApplicationHeaderTemplate extends CompositeWithUsername {

    @UiField
    Label titleLabel;
    @UiField
    InlineLabel userInfoLabel;
    @UiField
    Button logoutButton;

    interface ApplicationHeaderTemplateUiBinder extends UiBinder<Widget, ApplicationHeaderTemplate> {
    }

    private static ApplicationHeaderTemplateUiBinder uiBinder = GWT.create(ApplicationHeaderTemplateUiBinder.class);

    public ApplicationHeaderTemplate() {
        initWidget(uiBinder.createAndBindUi(this));
		logoutButton.setText(LOGOUT_TEXT);
		logoutButton.setTitle(LOGOUT_TEXT);
    }

    protected void setPageTitle(String title) {
        titleLabel.setText(title);
    }

    @UiHandler("logoutButton")
    void logoutOnClick(ClickEvent event) {
        DashboardLogoutPage.showPage();
    }

}
