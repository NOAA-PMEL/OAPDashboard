/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;

/**
 * @author kamb
 *
 */
public interface FeatureTypeFields extends IsWidget {

    void setFormFields(DataUploadPage page);

    void clearFormFields(DataUploadPage page);
}
