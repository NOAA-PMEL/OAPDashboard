/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.MyDecoratedPopupPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBoxBase;

import gov.noaa.pmel.dashboard.shared.DataColumnType;

import com.google.gwt.user.client.ui.PopupPanel.AnimationType;

/**
 * @author kamb
 *
 */
public class MySuggestBox extends SuggestBox {

    /**
     * 
    public MySuggestBox() {
        // TODO Auto-generated constructor stub
    }
     */

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
        
//        public MySug() {
//            suggestionMenu = new SuggestionMenu(true);
//            suggestionPopup = createPopup();
//            suggestionPopup.setWidget(decorateSuggestionList(suggestionMenu));
//          }

        @Override
        protected PopupPanel createPopup() {
//            MyDecoPanel p = new MyDecoPanel();
            MyDecoratedPopupPanel p = new MyDecoratedPopupPanel(true, false);
            handler = new MyHandler("MyDeco handler");
            p.addHandler(handler, KeyUpEvent.getType());
            p.addHandler(handler, ScrollEvent.getType());
            p.addHandler(handler, MouseWheelEvent.getType());
            p.addHandler(handler, MouseMoveEvent.getType());
//            p.addDomHandler(new MyHandler("MyDeco domHandler"), MouseOverEvent.getType());
//            p.addAttachHandler(new MyHandler("MyDeco attachHandler"));

//            DecoratorPanel decPanel = p.getDecoPanel();
//            MyDecoratedPopupPanel p = new MyDecoratedPopupPanel();
//            p.setStyleName(decPanel.getContainerElement(), prefix + "Content", true);
            p.setStyleName("gwt-SuggestBoxPopup");
            p.setPreviewingAllNativeEvents(true);
            p.setAnimationType(AnimationType.ROLL_DOWN);
            return p;
          }

    }
    
//    public static class MyDecoPanel extends DecoratedPopupPanel {
//        
//        DecoratorPanel superDeco;
//        
//        public MyDecoPanel() {
//            super(true, false);
//        }
//        
//        DecoratorPanel getDecoPanel() {
//            if ( superDeco == null ) {
//                try {
//                    Field decoField = this.getClass().getField("decPanel");
//                    superDeco = (DecoratorPanel)decoField.get(this);
//                } catch (Exception nonsuch) {
//                    throw new RuntimeException(nonsuch);
//                }
//            }
//            return superDeco;
//        }
//    }
}
