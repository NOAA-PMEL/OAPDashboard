/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author kamb
 *
 */
public class PreviewTab implements Serializable, IsSerializable {
    
    private static final long serialVersionUID = 4964233401608770035L;

    private String _title;
    
    private List<PreviewPlotImage> _plots;
    
    @SuppressWarnings("unused") // GWT
    private PreviewTab() {}
        
    public PreviewTab(String title) { this._title = title; }
    
    public PreviewTab(String title, List<PreviewPlotImage> plots) {
        this(title);
        this._plots = plots;
    }
    
    public String title() { return _title; }
    
    public void addPlot(PreviewPlotImage plot) {
        getPlots().add(plot);
    }
    public synchronized List<PreviewPlotImage> getPlots() {
        if ( _plots == null ||
             _plots == Collections.EMPTY_LIST ) {
            _plots = new ArrayList<PreviewPlotImage>();
        }
        return _plots;
    }
    public synchronized void setPlots(List<PreviewPlotImage> plots) {
        _plots = plots;
    }
    
    @Override
    public String toString() {
        return _title;
    }
}
