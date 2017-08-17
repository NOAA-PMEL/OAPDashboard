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
	
	private boolean finished;
	
	private List<List<String>> tabs;

	@SuppressWarnings("unused") // for GWT
	private PreviewPlotResponse() {
		tabs = new ArrayList<List<String>>();
	}
	
	public PreviewPlotResponse(List<List<String>>plotTabs, boolean isFinished) {
		finished = isFinished;
		tabs = plotTabs != null ? plotTabs : new ArrayList<List<String>>();
	}
	public boolean isFinished() { return finished; }
	public List<List<String>> getPlotTabs() {
		return tabs;
	}
	
	public void setFinished(boolean isFinished) { finished = isFinished; }
	public void setPlotTabs(List<List<String>> plotTabs) { tabs = plotTabs; }
	public void addPlotTab(List<String> tab) {
		tabs.add(tab);
	}
}
