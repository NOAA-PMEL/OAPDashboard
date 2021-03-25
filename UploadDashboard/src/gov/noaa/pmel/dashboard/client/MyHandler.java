/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.TextBox;

/**
 * @author kamb
 *
 */
public class MyHandler implements AttachEvent.Handler, ClickHandler, KeyUpHandler, FocusHandler, BlurHandler, DoubleClickHandler, MouseMoveHandler, MouseOverHandler, ContextMenuHandler {
    
    private String _name;
    
    public MyHandler(String name) {
        _name = name;
    }
    /**
     * Fired when the user clicks on the sendButton.
     */
    public void onClick(ClickEvent event) {
        GWT.log(_name + " Click: "+ event.getSource());
//        sendNameToServer();
    }

    /**
     * Fired when the user types in the nameField.
     */
    public void onKeyUp(KeyUpEvent event) {
        GWT.log(_name + " keyup: "+ event.getSource());
//        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
//            sendNameToServer();
//        }
    }
    
    @Override
    public void onFocus(FocusEvent event) {
        // TODO Auto-generated method stub
        GWT.log(_name + " event.getSource() focus: " + event.getSource());
//        unitsListBox.clear();
//        listPanel.setVisible(false);
        
//      data1suggestionBox.showSuggestionList();
//      boolean suggestionShowing = data1suggestionBox.getSuggestionDisplay().isSuggestionListShowing();
//      
//      if ( suggestionShowing ) {
//          Window.alert("showing!");
//      }
//      

    }
    @Override
    public void onBlur(BlurEvent event) {
        // TODO Auto-generated method stub
        GWT.log(_name + " event.getSource() blur: " + event.getSource());
        
//        GWT.log("event.getSource() value: " + ((TextBox)event.getSource()).getValue());
//        String checkValue = (String)((TextBox)event.getSource()).getValue();
//        
//        List<String> unitsList = getUnitsList(checkValue, unitsLookup);
//        unitsListBox.clear();
//        
//        if (unitsList.isEmpty()) {
//            listPanel.setVisible(false);
//        } else {
//            for ( String data : unitsList ) {
//                unitsListBox.addItem(data);
//            }
//            listPanel.setVisible(true);
//        }
    }
    
    @Override
    public void onDoubleClick(DoubleClickEvent event) {
        // TODO Auto-generated method stub
        GWT.log(_name + " event.getSource() doubleclik: " + event.getSource());
//        GWT.log("onDoubleClick " + dataTypeSuggestionBox.getValueBox().toString());
//      data1suggestionBox.setFocus(true);
//        dataTypeSuggestionBox.setText(null);  
//        dataTypeSuggestionBox.showSuggestionList();
        
//      data1suggestionBox = new SuggestBox(getDataOracle(data1lookup));
//      data1suggestionBox.getSuggestOracle().setDefault
    }


    @Override
    public void onMouseOver(MouseOverEvent event) {
        // TODO Auto-generated method stub
//      GWT.log(_name + " event.getSource() mouseover: " + event.getSource());
//      GWT.log("onMouseOver " + dataTypeSuggestionBox.getValueBox().toString());
        
//      data1suggestionBox.showSuggestionList();
//        boolean suggestionShowing = dataTypeSuggestionBox.getSuggestionDisplay().isSuggestionListShowing();
        
//      if ( suggestionShowing ) {
//          Window.alert("showing!");
//      }
        
//      NodeList<Element> elements = Document.get().getElementsByTagName("input");
        NodeList<Element> elements = Document.get().getElementsByTagName("div");
        for (int i = 0; i < elements.getLength(); i++) {
//          GWT.log("elements1 " + elements.getItem(i).getClassName());
            if (elements.getItem(i).getClassName().contains("suggestPopupContent")) {
                Element childElement = elements.getItem(i).getFirstChildElement();
                GWT.log("mouseover childElement " + childElement);
                GWT.log("mouseover childElement id " + childElement.getAttribute("aria-activedescendant"));
//                GWT.log("elementsin " + elements.getItem(i).getClassName());
//                GWT.log("elementsin " + elements.getItem(i).getNodeName());
//              GWT.log("elementsin " + elements.getItem(i).getInnerHTML());
//                GWT.log("elementsin " + elements.getItem(i).getAttribute("aria-activedescendant"));
//              <div tabindex="0" role="menubar" class="" hidefocus="true" aria-activedescendant="gwt-uid-21" style="outline: 0px;"></div>
            }
            

        }
//      Document.get().getElementsByClassName();
        
        
//      NodeList<Element> elems = Document.get().getElementsByTagName("div");
//      NodeList<Element> elems = Document.get().
//      //getElementsByTagName("td");
//      int count = 0;
//      for (int i = 0; i < elems.getLength(); i++) {
//
//          Element elem = elems.getItem(i);
//          GWT.log("elem " + elem.getClassName());
////            Label l = Label.wrap(elem);
////            l.addClickHandler(new ClickHandler() {
////                @Override
////                public void onClick(ClickEvent event) {
////                    Window.alert("yay!");
////                }
////            });
//          count = i++;
//          GWT.log("div incremental  " + i);
//      }
//      GWT.log("div count " + count);
        
    }

    @Override
    public void onContextMenu(ContextMenuEvent event) {
        // TODO Auto-generated method stub
        GWT.log(_name + " event.getSource() contectmenu: " + event.getSource());
//        GWT.log("onContextMenu " + dataTypeSuggestionBox.getValueBox().toString());
    }

    /* (non-Javadoc)
     * @see com.google.gwt.event.logical.shared.AttachEvent.Handler#onAttachOrDetach(com.google.gwt.event.logical.shared.AttachEvent)
     */
    @Override
    public void onAttachOrDetach(AttachEvent event) {
        GWT.log(_name + ( event.isAttached() ? " Attach:":" Detach:" )+event.getSource());
    }

    /* (non-Javadoc)
     * @see com.google.gwt.event.dom.client.MouseMoveHandler#onMouseMove(com.google.gwt.event.dom.client.MouseMoveEvent)
     */
    @Override
    public void onMouseMove(MouseMoveEvent event) {
//        GWT.log(_name + " mmove:"+event.getSource());
        NodeList<Element> elements = Document.get().getElementsByTagName("div");
        for (int i = 0; i < elements.getLength(); i++) {
            if (elements.getItem(i).getClassName().contains("suggestPopupContent")) {
                Element childElement = elements.getItem(i).getFirstChildElement();
                GWT.log("mousemove childElement " + childElement.getAttribute("aria-activedescendant"));
            }
        }
    }
}
