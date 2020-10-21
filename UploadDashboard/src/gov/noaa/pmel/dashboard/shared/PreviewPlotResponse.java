/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import lombok.Singular;

/**
 * @author kamb
 *
 */
public class PreviewPlotResponse implements Serializable, IsSerializable {
	
	private static final long serialVersionUID = 5360711782787243232L;

	private boolean _finished;
	
    @Singular
	private List<PreviewTab> _tabs;

	@SuppressWarnings("unused") // for GWT
	private PreviewPlotResponse() {
		_tabs = new ArrayList<PreviewTab>();
	}
	
	public PreviewPlotResponse(List<PreviewTab>plotTabs, boolean isFinished) {
		_finished = isFinished;
		_tabs = plotTabs != null ? plotTabs : new ArrayList<PreviewTab>();
	}
	public boolean isFinished() { return _finished; }
	public List<PreviewTab> getPlotTabs() {
		return _tabs;
	}
	
	public void setFinished(boolean isFinished) { _finished = isFinished; }
	public void setPlotTabs(List<PreviewTab> plotTabs) { _tabs = plotTabs; }
	public void addPlotTab(PreviewTab tab) {
		_tabs.add(tab);
	}
}
