/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author kamb
 *
 */
public class ProfileFeatureFields extends Composite implements FeatureTypeFields {

    @UiField CommonFeatureFields _commonFields;
    
    interface ProfileFeatureFieldsUiBinder extends UiBinder<Widget, ProfileFeatureFields> {
    }

    private static ProfileFeatureFieldsUiBinder uiBinder = GWT.create(ProfileFeatureFieldsUiBinder.class);

    public ProfileFeatureFields() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void setFormFields(DataUploadPage page) {
        _commonFields.setFormFields(page);
    }

    @Override
    public void clearFormFields(DataUploadPage page) {
        _commonFields.clearFormFields(page);
    }

}
