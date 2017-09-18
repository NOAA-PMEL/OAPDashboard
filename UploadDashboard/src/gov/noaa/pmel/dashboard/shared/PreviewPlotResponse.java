/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author kamb
 *
 */
public class PreviewPlotResponse implements Serializable, IsSerializable {
	
	private static final long serialVersionUID = 5360711782787243232L;

	private boolean finished;
	
	private List<List<PreviewPlotImage>> tabs;

	@SuppressWarnings("unused") // for GWT
	private PreviewPlotResponse() {
		tabs = new ArrayList<List<PreviewPlotImage>>();
	}
	
	public PreviewPlotResponse(List<List<PreviewPlotImage>>plotTabs, boolean isFinished) {
		finished = isFinished;
		tabs = plotTabs != null ? plotTabs : new ArrayList<List<PreviewPlotImage>>();
	}
	public boolean isFinished() { return finished; }
	public List<List<PreviewPlotImage>> getPlotTabs() {
		return tabs;
	}
	
	public void setFinished(boolean isFinished) { finished = isFinished; }
	public void setPlotTabs(List<List<PreviewPlotImage>> plotTabs) { tabs = plotTabs; }
	public void addPlotTab(List<PreviewPlotImage> tab) {
		tabs.add(tab);
	}
}
