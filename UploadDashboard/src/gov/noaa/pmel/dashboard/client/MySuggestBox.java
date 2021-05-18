/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.Collection;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.ui.MyDecoratedPopupPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;

import com.google.gwt.user.client.ui.PopupPanel.AnimationType;
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
    public MySuggestBox(SuggestOracle oracle, TextBox theBox, AttachEvent.Handler attachHandler) {
        super(oracle, theBox, new MySuggestionDisplay(attachHandler));
    }

    public static class MySuggestionDisplay extends SuggestBox.DefaultSuggestionDisplay {
//        private MyHandler handler;
        private MyDecoratedPopupPanel decPopup;

        public MySuggestionDisplay(Handler attachHandler) {
            super();
            decPopup.addAttachHandler(attachHandler);
        }
        
    	// SET popup same size as textbox
    	@Override
    	protected void showSuggestions(SuggestBox suggestBox, Collection<? extends Suggestion> suggestions, boolean isDisplayStringHTML, boolean isAutoSelectEnabled, SuggestionCallback callback) {
    		super.showSuggestions(suggestBox, suggestions, isDisplayStringHTML, isAutoSelectEnabled, callback);
    		getPopupPanel().setWidth((suggestBox.getElement().getAbsoluteRight() - suggestBox.getAbsoluteLeft()) + Unit.PX.getType());
    	}

        @Override
        protected PopupPanel createPopup() {
            decPopup = new MyDecoratedPopupPanel(true, false);
            decPopup.setStyleName("gwt-SuggestBoxPopup");
            decPopup.setPreviewingAllNativeEvents(true);
            decPopup.setAnimationType(AnimationType.ROLL_DOWN);
            return decPopup;
          }
    }
}
