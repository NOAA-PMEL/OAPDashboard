/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.CustomScrollPanel;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;

/**
 * @author kamb
 *
 */
public class MyDataGrid<T> extends DataGrid<T> {

    /**
     * 
     */
    public MyDataGrid() {
        GWT.log("MDG #0");
    }

    /**
     * @param pageSize
     */
    public MyDataGrid(int pageSize) {
        super(pageSize);
        GWT.log("MDG #1");
    }

    /**
     * @param keyProvider
     */
    public MyDataGrid(ProvidesKey keyProvider) {
        super(keyProvider);
        GWT.log("MDG #2");
    }

    /**
     * @param pageSize
     * @param keyProvider
     */
    public MyDataGrid(int pageSize, ProvidesKey keyProvider) {
        super(pageSize, keyProvider);
        GWT.log("MDG #3");
    }

    /**
     * @param pageSize
     * @param resources
     */
    public MyDataGrid(int pageSize, Resources resources) {
        super(pageSize, resources);
        GWT.log("MDG #4");
    }

    /**
     * @param pageSize
     * @param resources
     * @param keyProvider
     */
    public MyDataGrid(int pageSize, Resources resources, ProvidesKey keyProvider) {
        super(pageSize, resources, keyProvider);
        GWT.log("MDG #5");
    }

    /**
     * @param pageSize
     * @param resources
     * @param keyProvider
     * @param loadingIndicator
     */
    public MyDataGrid(int pageSize, Resources resources, ProvidesKey keyProvider, Widget loadingIndicator) {
        super(pageSize, resources, keyProvider, loadingIndicator);
        GWT.log("MDG #6");
    }

    public ScrollPanel getScrollPanel() {
        HeaderPanel header = (HeaderPanel) getWidget();
        return (ScrollPanel) header.getContentWidget();
    }
}
