/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author kamb
 *
 */
public class OpaqueUploadFeatureFields extends Composite implements FeatureTypeFields {

    interface UnspecifiedUploadFeatureFieldsUiBinder extends UiBinder<Widget, OpaqueUploadFeatureFields> {
    }

    private static UnspecifiedUploadFeatureFieldsUiBinder uiBinder = GWT.create(UnspecifiedUploadFeatureFieldsUiBinder.class);

    @UiField TextBox unspecDatasetIdBox;
    Hidden datasetIdFormField;
    
    public OpaqueUploadFeatureFields() {
        initWidget(uiBinder.createAndBindUi(this));
        unspecDatasetIdBox.getElement().setPropertyString("placeholder", "Default: [File Name]");
    }

    @Override
    public void setFormFields(Panel form) {
        if ( datasetIdFormField == null ) {
            datasetIdFormField = new Hidden("datasetID");
            form.add(datasetIdFormField);
        }
        datasetIdFormField.setValue(unspecDatasetIdBox.getValue());
    }

    @Override
    public void clearFormFields(Panel form) {
        if ( datasetIdFormField != null ) {
            datasetIdFormField.setValue("");
        }
    }
}
