/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.Collection;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MyDecoratedPopupPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.Widget;

import gov.noaa.pmel.dashboard.shared.DataColumnType;

import com.google.gwt.user.client.ui.PopupPanel.AnimationType;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionCallback;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

/**
 * @author kamb
 *
 */
public class MySuggestBox extends SuggestBox {

    /**
     * 
     */
    public MySuggestBox() {
        // TODO Auto-generated constructor stub
    	super();
    }
     

    /**
     * @param oracle
     */
    public MySuggestBox(SuggestOracle oracle, TextBox theBox) {
        super(oracle, theBox, new MySuggestionDisplay());
    }

    /**
     * @param oracle
     * @param box
    public MySuggestBox(SuggestOracle oracle, ValueBoxBase<String> box) {
        super(oracle, box);
        // TODO Auto-generated constructor stub
    }
     */

	/**
     * @param oracle
     * @param box
     * @param suggestDisplay
    public MySuggestBox(SuggestOracle oracle, ValueBoxBase<String> box, SuggestionDisplay suggestDisplay) {
        super(oracle, box, suggestDisplay);
        // TODO Auto-generated constructor stub
    }
     */

    public static class MySuggestionDisplay extends SuggestBox.DefaultSuggestionDisplay {
        private MyHandler handler;

        public MySuggestionDisplay() {
            super();
        }
        
    	// SET popup same size as textbox
    	@Override
    	protected void showSuggestions(SuggestBox suggestBox, Collection<? extends Suggestion> suggestions, boolean isDisplayStringHTML, boolean isAutoSelectEnabled, SuggestionCallback callback) {
    		super.showSuggestions(suggestBox, suggestions, isDisplayStringHTML, isAutoSelectEnabled, callback);
    		getPopupPanel().setWidth((suggestBox.getElement().getAbsoluteRight() - suggestBox.getAbsoluteLeft()) + Unit.PX.getType());
    	}

        @Override
        protected PopupPanel createPopup() {
            MyDecoratedPopupPanel p = new MyDecoratedPopupPanel(true, false);
            handler = new MyHandler("MyDeco handler");
            p.addHandler(handler, KeyUpEvent.getType());
            p.addHandler(handler, BlurEvent.getType());
            p.addHandler(handler, FocusEvent.getType());
            p.addHandler(handler, ScrollEvent.getType());
            p.addHandler(handler, MouseWheelEvent.getType());
            p.addHandler(handler, MouseMoveEvent.getType());
            p.addHandler(handler, MouseOverEvent.getType());
            p.setStyleName("gwt-SuggestBoxPopup");
            p.setPreviewingAllNativeEvents(true);
            p.setAnimationType(AnimationType.ROLL_DOWN);
            return p;
          }

    }
}
