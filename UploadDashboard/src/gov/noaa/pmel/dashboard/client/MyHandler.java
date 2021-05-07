/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;


/**
 * @author kamb
 *
 */
public class MyHandler implements AttachEvent.Handler, ClickHandler, KeyUpHandler, KeyPressHandler,
                                  FocusHandler, BlurHandler, DoubleClickHandler, 
                                  MouseMoveHandler, MouseOverHandler, MouseOutHandler, 
                                  ScrollHandler, MouseWheelHandler {
	
    private String _name;
    private String _currentId = null;
    
    private String lastSelectionValue = null;
    
    static PopupPanel descriptionPopup;
    private int _mmoveCount = 0;
    private boolean _hasMouseOver = false;
    private boolean _hasMouseOut = false;
    
    final int VISIBLE_DELAY = 500;
    final String TOOLTIP_STYLENAME = "tooltip";
    
    /**
     * Constant returned from {@link #getDraggable()}.
     */
	public static final String POINTEREVENTS_AUTO = "auto";
    public static final String POINTEREVENTS_NONE = "none";
    
    private Timer descriptionPopupTimer = null;
    public boolean timerState = false;

    // Simplified regular expression for an HTML tag (opening or closing) or an HTML escape. 
    final RegExp SKIP_HTML_RE = RegExp.compile("<[^>]*>|&[^;]+;", "g");
    
    /**
     * @param string
     */
    public MyHandler(String name) {
        _name = name;
    }
        
    /**
     * Fired when the user clicks on the sendButton.
     */
    public void onClick(ClickEvent event) {
        GWT.log(_name + " Click: "+ event.getSource());
    }

    /* (non-Javadoc)
     * @see com.google.gwt.event.dom.client.MouseWheelHandler#onMouseWheel(com.google.gwt.event.dom.client.MouseWheelEvent)
     */
    @Override
    public void onMouseWheel(MouseWheelEvent event) {
        GWT.log(_name + " mousewheel: "); // + event.getNativeEvent());

        if (descriptionPopup != null) {
        	hideDescriptionPopup();
        }
        
    }
    /* (non-Javadoc)
     * @see com.google.gwt.event.dom.client.ScrollHandler#onScroll(com.google.gwt.event.dom.client.ScrollEvent)
     */
    @Override
    public void onScroll(ScrollEvent event) {
        GWT.log(_name + " scroll: "+ event.getSource());
    }
    
    /**
     * Fired when the user types in the nameField.
     */
    public void onKeyPress(KeyPressEvent event) {
        GWT.log(_name + " keypress: "); // + event.getSource());
    }
    public void onKeyUp(KeyUpEvent event) {
        GWT.log(_name + " keyup: " + event.getNativeKeyCode()); // + event.getSource());

        int keyCode = event.getNativeKeyCode();
        
        if (_currentId != null) {
        	GWT.log(_name + " keyup[currentId]: " + _currentId);
        }

        if ( keyCode == KeyCodes.KEY_UP || keyCode == KeyCodes.KEY_DOWN ) {
        	hideDescriptionPopup();
            NodeList<Element> elements = Document.get().getElementsByTagName("div");
            for (int i = 0; i < elements.getLength(); i++) {
    //          GWT.log("elements1 " + elements.getItem(i).getClassName());
                if (elements.getItem(i).getClassName().contains("suggestPopupContent")) {
//                	setTablePointerEvent(elements.getItem(i), POINTEREVENTS_NONE);
                    Element childElement = elements.getItem(i).getFirstChildElement();
    //                GWT.log("mouseover childElement " + childElement);
                    String activeId = childElement.getAttribute("aria-activedescendant");
                    
                    GWT.log("arrow active id " + activeId);
                    GWT.log("arrow current id " + _currentId);
                    boolean wasNull_currentId = false;
                    if (_currentId == null) {
                    	_currentId = activeId;
                    	GWT.log("arrow current id was null" + _currentId);
                    	childElement.setAttribute("aria-activedescendant", _currentId);
                    	Element line = Document.get().getElementById(_currentId);
                        GWT.log("element: " + line + " : " + line.getInnerHTML());
                        line.addClassName("item-selected");
                        
                        GWT.log("element: " + line + " : " + line.getClassName());
                    	wasNull_currentId = true;
                    }
                    
                    if ( activeId != null && ! activeId.equals(_currentId) ) {
                        Element line = Document.get().getElementById(activeId);
                        GWT.log("element: " + line + " : " + line.getInnerHTML());
                        line.addClassName("item-selected");
                        
                        GWT.log("element: " + line + " : " + line.getClassName());
                        
                        lastSelectionValue = line.getInnerHTML();
                        
//                        Element table = getClosestParentByTagName(line, "table");
//                        GWT.log("Aelement: " + table + " : " + table.getClassName());
//                        
//                        setTablePointerEvent(table, POINTEREVENTS_NONE);
//                        
//                        GWT.log("BSelement: " + table + " : " + table.getClassName());

//                        if ( ! isInView(activeId)) {
                        if (_currentId != null && wasNull_currentId ) {
                        	scrollIt(_currentId);
                        }
                        else {
                        	scrollIt(activeId);
                        }
                            
//                        }
                            _currentId = activeId;
                    }
                }
            }
        }
    }
    
	public final native Element getClosestParentByTagName(Element el, String value) /*-{
		return el.closest(value);
	}-*/;
	
	// pointer-events property value: auto|none;
	public final native void setTablePointerEvent(Element table, String value ) /*-{
		console.log("table:" + table);
		console.log("value:" + value);
		table.style.pointerEvents = value || POINTEREVENTS_NONE;
		console.log("setTablePointerEvent " + table.style.getPropertyValue("pointer-events"));
	}-*/;
    
//    private native boolean isInView(String eid) /*-{
//        var container = $(eid);
//        var contHeight = container.height();
//        var contTop = container.scrollTop();
//        var contBottom = contTop + contHeight ;
//    
//        var elemTop = $(elem).offset().top - container.offset().top;
//        var elemBottom = elemTop + $(elem).height();
//    
//        var isTotal = (elemTop >= 0 && elemBottom <=contHeight);
//        var isPart = ((elemTop < 0 && elemBottom > 0 ) || (elemTop > 0 && elemTop <= container.height())) && partial ;
//    
//        return  isTotal  || isPart ;
//    }-*/;
    
    private native void scrollIt(String eid) /*-{
        console.log("scrolling " + eid);
        var cell = $doc.getElementById(eid);
//        var table = cell.closest('table');
//        console.log(cell, table);
//        table.style.pointerEvents="none"
//        console.log("element: "+ cell);
//        var offp = cell.offsetParent; //  === null;
//        console.log("offp " + offp);
//        var style = window.getComputedStyle(cell);
//        console.log("style display: " + style.display);
//        console.log("style visibility: " + style.visibility);
//        if (style.display === 'none')
            cell.scrollIntoView({behavior: "smooth", block: "nearest", inline: "nearest"});
//          _currentId = eid;
    }-*/;
    
    @Override
    public void onFocus(FocusEvent event) {
        // TODO Auto-generated method stub
        GWT.log(_name + " event.getSource() focus: "); // + event.getSource());

    }
    @Override
    public void onBlur(BlurEvent event) {
        // TODO Auto-generated method stub
        GWT.log(" ***** "+ _name + " onBlur "); // + event.getSource());
        
        hideDescriptionPopup();
    }
    
    @Override
    public void onDoubleClick(DoubleClickEvent event) {
        // TODO Auto-generated method stub
        GWT.log(_name + " event.getSource() doubleclik: " + event.getSource());
//        _dataTypeSelectorPopup.showAllChoices();
    }

    /* (non-Javadoc)
     * @see com.google.gwt.event.dom.client.MouseMoveHandler#onMouseMove(com.google.gwt.event.dom.client.MouseMoveEvent)
     */
    @Override
    public void onMouseMove(MouseMoveEvent event) {
//        GWT.log(_name + " mmove:"); // +event.getSource());
//        GWT.log(_name + " getRelativeElement:"); // + event.getRelativeElement());

    	String activeId = null;
    	Element line = null;

        NodeList<Element> elements = Document.get().getElementsByTagName("div");
        for (int i = 0; i < elements.getLength(); i++) {
            if (elements.getItem(i).getClassName().contains("suggestPopupContent")) {
//            	setTablePointerEvent(elements.getItem(i), POINTEREVENTS_AUTO);
                Element childElement = elements.getItem(i).getFirstChildElement();
                activeId = childElement.getAttribute("aria-activedescendant");
                
//                if ( activeId != null) {
//                	line = Document.get().getElementById(activeId);
//                	Element table = getClosestParentByTagName(line, "table");
//                    GWT.log("mousemove: " + table + " : " + table.getInnerHTML());
//                    
//                    setTablePointerEvent(table, POINTEREVENTS_AUTO);
//                    
//                    GWT.log("mousemove: " + table + " : " + table.getClassName());
//                }

                if ( activeId != null && ! activeId.equals(_currentId)) {
//                    GWT.log(_name + " mousemove childElement " + activeId);
                	
                    line = Document.get().getElementById(activeId);
//                    Element table = getClosestParentByTagName(line, "table");
//                    GWT.log("mousemove: " + table + " : " + table.getInnerHTML());
//                    
//                    setTablePointerEvent(table, POINTEREVENTS_AUTO);
                    
                    
                    
//                    varName = line.getInnerHTML();
//                    GWT.log(_name + " element: " + line + " : " + varName + " : " + DataTypeSelectorPopup.dataTypeLookup.get(varName).getDescription());
//
//                    String description = DataTypeSelectorPopup.dataTypeLookup.get(varName).getDescription();
//                    GWT.log(_name + " element: " + line + " : " + varName + " : " + description);
                    
                    if(descriptionPopup != null && descriptionPopup.isShowing()) {
        	        	descriptionPopup.hide();
//        	        	GWT.log("hiding descriptionPopup");
        	        }

                    _currentId = activeId;
                    _mmoveCount = 0;
                }
                _mmoveCount++;
            }
        }

        if ( activeId != null && activeId.equals(_currentId)) {

//        	GWT.log("_mmoveCount: " + _mmoveCount);
        	
        	if (line != null && _mmoveCount == 1) {
        		String varName = stripHtml(line.getInnerHTML());
        		String description = "";
                if (DataTypeSelectorPopup.dataTypeLookup.get(varName).getDescription() != null ) {
                	description = DataTypeSelectorPopup.dataTypeLookup.get(varName).getDescription();
                }
        		
        		if (description != null && !description.isEmpty()) {
        			int xCord = event.getClientX() + 10;
        			int yCord = event.getClientY();
        			descriptionPopup = new PopupPanel();
        			descriptionPopup.setStyleName(TOOLTIP_STYLENAME);
        			descriptionPopup.setPopupPosition(xCord, yCord);
        			descriptionPopup.setAutoHideEnabled(false);
//        			descriptionPopup.add(new Label(varName + ": " + description));
        			descriptionPopup.add(new Label(description));

        		    descriptionPopupTimer = new Timer() {
        		        @Override
        		        public void run() {
        		        	timerState = true;
        		        	if ( isSuggestBoxPopupShowing() && !_hasMouseOut) {
//        		        		GWT.log("isSuggestBoxPopupShowing? " + isSuggestBoxPopupShowing());
        		        		descriptionPopup.show();
        		            }
        		        	else {
//        		        		GWT.log("isSuggestBoxPopupShowing[NOT]? " + isSuggestBoxPopupShowing());
        		        		hideDescriptionPopup();
        		        	}
        		        }
        		    };
        		    descriptionPopupTimer.schedule(VISIBLE_DELAY);
        		    
        		}
        	}
        }
        
    }

    //gwt-SuggestBoxPopup
    private final native boolean isSuggestBoxPopupShowing() /*-{
		if ($doc.getElementsByClassName("gwt-SuggestBoxPopup").length) {
			return true;
		}
		return false;
	}-*/;
    
    @Override
    public void onMouseOut(MouseOutEvent event) {
        GWT.log(_name + " mouseout: "); // + event.getSource());
    	_hasMouseOut = true;
        hideDescriptionPopup();
    }
    
    /**
     * 
     */
    private void hideDescriptionPopup() {
    	if (descriptionPopup != null && descriptionPopup.isShowing()) {
//    		GWT.log(descriptionPopup + " is showing");
			descriptionPopup.hide();
			_mmoveCount = 0;
			_currentId = null;
		}
    	
    	if (timerState == true) {
    		descriptionPopupTimer.cancel();
    		GWT.log("canceling timer!");
    		timerState = false;
    	}
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {
    	_mmoveCount = 0;
    	_hasMouseOver = true;
    	_hasMouseOut = false;
    	
      GWT.log(_name + " event.getSource() mouseover: "); //  + event.getSource());
      
//      GWT.log("onMouseOver " + dataTypeSuggestionBox.getValueBox().toString());
        
//      data1suggestionBox.showSuggestionList();
//        boolean suggestionShowing = dataTypeSuggestionBox.getSuggestionDisplay().isSuggestionListShowing();
        
//      if ( suggestionShowing ) {
//          Window.alert("showing!");
//      }
        
//      NodeList<Element> elements = Document.get().getElementsByTagName("input");
    	
//        NodeList<Element> elements = Document.get().getElementsByTagName("div");
//        for (int i = 0; i < elements.getLength(); i++) {
////          GWT.log("elements1 " + elements.getItem(i).getClassName());
//            if (elements.getItem(i).getClassName().contains("suggestPopupContent")) {
//                Element childElement = elements.getItem(i).getFirstChildElement();
////                GWT.log("mouseover childElement " + childElement);
//                String activeId = childElement.getAttribute("aria-activedescendant");
//                GWT.log("mouseover active id " + activeId);
//                if ( activeId != null && ! activeId.equals(_currentId) ) {
//                    Element line = Document.get().getElementById(activeId);
//                    GWT.log("element: " + line + " : " + line.getInnerHTML());
//                    String varName = line.getInnerHTML();
//                    GWT.log(_name + " element: " + line + " : " + varName
//                        + " : " + DataTypeSelectorPopup.dataTypeLookup.get(varName).getDescription());
//                    _currentId = activeId;
//                }
////                GWT.log("elementsin " + elements.getItem(i).getClassName());
////                GWT.log("elementsin " + elements.getItem(i).getNodeName());
////              GWT.log("elementsin " + elements.getItem(i).getInnerHTML());
////                GWT.log("elementsin " + elements.getItem(i).getAttribute("aria-activedescendant"));
////              <div tabindex="0" role="menubar" class="" hidefocus="true" aria-activedescendant="gwt-uid-21" style="outline: 0px;"></div>
//            }
//            
//
//        }
    	
    	
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


    /* (non-Javadoc)
     * @see com.google.gwt.event.logical.shared.AttachEvent.Handler#onAttachOrDetach(com.google.gwt.event.logical.shared.AttachEvent)
     */
    @Override
    public void onAttachOrDetach(AttachEvent event) {
        GWT.log(_name + ( event.isAttached() ? " Attach:":" Detach:" )+event.getSource());
        if (!event.isAttached() && descriptionPopup != null) {
        	hideDescriptionPopup();
        }

//        NodeList<Element> elements = Document.get().getElementsByTagName("div");
//        for (int i = 0; i < elements.getLength(); i++) {
////          GWT.log("elements1 " + elements.getItem(i).getClassName());
//            if (elements.getItem(i).getClassName().contains("suggestPopupContent")) {
//                Element content = elements.getItem(i);
//            }
//        }
    }
    
    /**
     * Returns the input text with spaces instead of HTML tags or HTML escapes
     */
    String stripHtml(String str) {
    	return SKIP_HTML_RE.replace(str, "");
    }
}
