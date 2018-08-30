/**
 * 
 */
package gov.noaa.pmel.dashboard.client;


import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.shared.DashboardUtils;

/**
 * @author kamb
 *
 */
public class CommonFeatureFields extends Composite implements FeatureTypeFields {

    private static Logger logger = Logger.getLogger("CommonFeatureFields");
    
	private static final String COMMA_FORMAT_TEXT = "file contains comma-separated values";
	private static final String SEMICOLON_FORMAT_TEXT = "file contains semicolon-separated values";
	private static final String TAB_FORMAT_TEXT = "file contains tab-separated values";

    public static final String DATASET_ID_COLUMN_FIELD_NAME = "datasetIdColName";

	@UiField RadioButton commaRadio;
	@UiField RadioButton semicolonRadio;
	@UiField RadioButton tabRadio;
    
    @UiField Label datasetColNameLabel;
    @UiField TextBox datasetColName;

//	Hidden formatToken;
//	Hidden datasetIdColName;
    
    interface CommonFeatureFieldsUiBinder extends UiBinder<Widget, CommonFeatureFields> {
    }

    private static CommonFeatureFieldsUiBinder uiBinder = GWT.create(CommonFeatureFieldsUiBinder.class);

    private ValueChangeHandler<Boolean> radioChange = new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> event) {
            GWT.log(event.getSource().toString());
            RadioButton rb = (RadioButton)event.getSource();
            FlowPanel panel = (FlowPanel)rb.getParent();
            for (int i = 0; i < panel.getWidgetCount(); i++) {
                Widget x = panel.getWidget(i);
                if ( x instanceof RadioButton && x != rb ) {
                    ((RadioButton)x).setValue(Boolean.FALSE);
                }
            }
        }
    };

    public CommonFeatureFields() {
        initWidget(uiBinder.createAndBindUi(this));
        
        commaRadio.setName(DashboardUtils.COMMA_FORMAT_TAG);
		commaRadio.setText(COMMA_FORMAT_TEXT);
		semicolonRadio.setName(DashboardUtils.SEMICOLON_FORMAT_TAG);
		semicolonRadio.setText(SEMICOLON_FORMAT_TEXT);
		tabRadio.setName(DashboardUtils.TAB_FORMAT_TAG);
		tabRadio.setText(TAB_FORMAT_TEXT);
        commaRadio.setValue(true, false);
        semicolonRadio.setValue(false, false);
        tabRadio.setValue(false, false);
        
        commaRadio.addValueChangeHandler(radioChange);
        semicolonRadio.addValueChangeHandler(radioChange);
        tabRadio.addValueChangeHandler(radioChange);
        
        String DATASET_ID_TT_TEXT = "Name of column specifying dataset ID. Only necessary if non-standard.";
        datasetColNameLabel.setText("Dataset ID Column Name:");
        datasetColName.getElement().setPropertyString("placeholder", "dataset ID column name");
        datasetColNameLabel.setTitle(DATASET_ID_TT_TEXT);
        datasetColName.setTitle(DATASET_ID_TT_TEXT);
    }

    @Override
    public void setFormFields(DataUploadPage page) {
        page.setFileDataFormatToken(getSelectedFormat().getName());
        page.setFormField(DATASET_ID_COLUMN_FIELD_NAME, datasetColName.getValue());
//        if ( formatToken == null ) {
//            logger.info("Adding dataformat field for " + this.getClass());
//            formatToken = new Hidden("dataformat");
//            form.add(formatToken);
//        }
//        if ( datasetIdColName == null ) {
//            logger.info("Adding datasetIdColName field for " + this.getClass());
//            datasetIdColName = new Hidden("datasetIdColName");
//            form.add(datasetIdColName);
//        }
//        formatToken.setValue(getSelectedFormat().getName());
//		datasetIdColName.setValue(datasetColName.getValue());
    }

    private RadioButton getSelectedFormat() {
        if ( commaRadio.getValue()) return commaRadio;
        if ( semicolonRadio.getValue()) return semicolonRadio;
        else return tabRadio;
    }

    @Override
    public void clearFormFields(DataUploadPage page) {
//        if ( formatToken != null ) {
//            formatToken.setValue("");
//            datasetIdColName.setValue("");
//            datasetColName.setValue("");
//        }
    }

}
