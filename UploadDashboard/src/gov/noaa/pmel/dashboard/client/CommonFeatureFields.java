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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
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

//    public static final String DATASET_ID_COLUMN_FIELD_NAME = "datasetIdColName";

	@UiField RadioButton commaRadio;
	@UiField RadioButton semicolonRadio;
	@UiField RadioButton tabRadio;
	@UiField Panel delimiterPanel;
    
    @UiField FlexTable flexTable;
    @UiField Label datasetColNameLabel;
    @UiField TextBox datasetColName;
    @UiField HTML datasetColNameDesc;
    @UiField Label unspecDatasetIdLabel;
    @UiField TextBox unspecDatasetIdBox;
    @UiField HTML unspecDatasetIdDesc;

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
        commaRadio.setValue(false, false);
        semicolonRadio.setValue(false, false);
        tabRadio.setValue(false, false);
        
        commaRadio.addValueChangeHandler(radioChange);
        semicolonRadio.addValueChangeHandler(radioChange);
        tabRadio.addValueChangeHandler(radioChange);
        
        delimiterPanel.setVisible(false);
        
        String DATASET_ID_TT_TEXT = "Name of column specifying dataset ID. Only necessary if non-standard.";
        datasetColNameLabel.setText("Dataset ID Column Name:");
        datasetColName.getElement().setPropertyString("placeholder", "dataset ID column name");
        datasetColNameLabel.setTitle(DATASET_ID_TT_TEXT);
        datasetColName.setTitle(DATASET_ID_TT_TEXT);
        
        int windowWidth = Window.getClientWidth();
        int ftWidth = (int)(.75 * windowWidth);
        flexTable.setWidth(ftWidth+"px");
        flexTable.setWidget(0, 0, datasetColNameLabel);
        flexTable.setWidget(0, 1, datasetColName);
        flexTable.setWidget(0, 2, datasetColNameDesc);
        flexTable.setWidget(1, 0, unspecDatasetIdLabel);
        flexTable.setWidget(1, 1, unspecDatasetIdBox);
        flexTable.setWidget(1, 2, unspecDatasetIdDesc);
        
        unspecDatasetIdBox.getElement().setPropertyString("placeholder", "optional dataset name");
        unspecDatasetIdBox.setVisible(false);
        unspecDatasetIdLabel.setVisible(false);
        unspecDatasetIdDesc.setVisible(false);
    }

    @Override
    public void setFormFields(DataUploadPage page) {
        page.setFileDataFormatToken(getSelectedFormat());
        page.setDatasetIdColumnNameToken(datasetColName.getValue());
        page.setDatasetIdToken(unspecDatasetIdBox.getValue());
    }

    private String getSelectedFormat() {
        if ( commaRadio.getValue()) return commaRadio.getName();
        if ( semicolonRadio.getValue()) return semicolonRadio.getName();
        if ( tabRadio.getValue()) return tabRadio.getName();
        else return DashboardUtils.UNSPECIFIED_DELIMITER_FORMAT_TAG;
    }

    @Override
    public void clearFormFields(DataUploadPage page) {
        GWT.log("CommonFields clearFormFields() called");
        unspecDatasetIdBox.setText(null);
        datasetColName.setText(null);
//        if ( formatToken != null ) {
//            formatToken.setValue("");
//            datasetIdColName.setValue("");
//            datasetColName.setValue("");
//        }
    }

}
