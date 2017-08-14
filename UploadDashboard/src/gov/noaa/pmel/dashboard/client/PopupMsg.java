/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * @author kamb
 *
 */
public class PopupMsg extends PopupPanel {
	
	PopupMsg(String msg) {
		super(true);
		setWidget(new HTMLPanel(msg));
	}

}
