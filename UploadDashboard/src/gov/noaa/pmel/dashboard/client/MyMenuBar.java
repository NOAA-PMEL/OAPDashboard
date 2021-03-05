/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;

/**
 * @author kamb
 *
 */
public class MyMenuBar extends MenuBar {
    public MyMenuBar() { super(true); }
//    public MyMenuBar(boolean vertical, Resources resources) { super(vertical, resources); }
//    public MyMenuBar(boolean vertical) { super(vertical); }
//    public MyMenuBar(Resources resources) { super(resources); }
    
    private MenuBar parentMenu;
    void setParentMenu(MenuBar menuBar) {
       parentMenu = menuBar; 
    }
    @Override
    public void onBrowserEvent(Event event) {
        GWT.log(String.valueOf(event)+":"+event.getType());
        if ( event.getType().equals("mouseout")) {
            MenuItem item = findItem(DOM.eventGetTarget(event));
            GWT.log("MenuItem:"+String.valueOf(item));
            if ( item != null && item.getElement().getId().equals("changePasswordBtn")) {
                parentMenu.closeAllChildren(false);
            }
        }
        super.onBrowserEvent(event);
    }
    private MenuItem findItem(Element hItem) {
    for (MenuItem item : getItems()) {
        if (item.getElement().isOrHasChild(hItem)) {
          return item;
        }
      }
      return null;
    }

}
