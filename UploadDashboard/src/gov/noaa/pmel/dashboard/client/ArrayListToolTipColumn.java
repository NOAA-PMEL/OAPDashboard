/**
* 
*/
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Map;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;

import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;

import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
* @author kamb
*
*/
public class ArrayListToolTipColumn extends ArrayListTextColumn {

	interface Templates extends SafeHtmlTemplates {
	
		@Template("<div title=\"{0}\">")
		SafeHtml startToolTip(String toolTipText);
		
		@Template("</div>")
		SafeHtml endToolTip();
	
	}
	
	private static final Templates TEMPLATES = GWT.create(Templates.class);
	
	public ArrayListToolTipColumn(int colIdx, DashboardDataset dataset, Map<Integer, ADCMessage> rowMsgMap) {
		super(colIdx, dataset, rowMsgMap);
	}
	
	@Override
	public void render(final Context context, final ArrayList<String> object, final SafeHtmlBuilder sb) {
	
		String toolTipText = getMessagesText(context, null);
		sb.append(TEMPLATES.startToolTip(toolTipText));
		super.render(context, object, sb);
		sb.append(TEMPLATES.endToolTip());
	}
}
