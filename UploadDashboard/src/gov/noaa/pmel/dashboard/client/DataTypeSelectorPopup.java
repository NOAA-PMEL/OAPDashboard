/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.KeyEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.PopupPanel.AnimationType;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextBox;

import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.UserInfo;

/**
 * An question embedded within a PopupPanel.
 * 
 * @author Linus Kamb
 */
public class DataTypeSelectorPopup extends Composite {

	interface DataTypeSelectorPopupUiBinder extends UiBinder<Widget, DataTypeSelectorPopup> {
	}

	private static DataTypeSelectorPopupUiBinder uiBinder = 
			GWT.create(DataTypeSelectorPopupUiBinder.class);

    @UiField Label prologue;
    @UiField Label descLbl;
    @UiField Label selectorLbl;
    @UiField Label unitsLbl;
    @UiField HTML descText;
    @UiField(provided=true) MySuggestBox dataSelector;
    @UiField ListBox unitsListBox;
	@UiField Button selectButton;
	@UiField Button cancelButton;
    @UiField Panel unitsPnl;
    @UiField Button showAllButton;

	private PopupPanel parentPanel;
	Boolean sendIt;
	HandlerRegistration handler;

    private DataColumnType currentType;

    private ValueUpdater<String> updater;

    private Map<String, DataColumnType> knownTypes;

    private int columnNumber;

    private String userColumnHeader;

    private AsyncCallback<UpdateInformation> callback;
    
    public static Map<String, DataColumnType> dataTypeLookup;
    private Map<String, List<String>> unitsLookup;
    
    final String DEFAULT_SUGGEST_VALUE = "ignored";
    
	/**
	 * Widget for asking a question in a PopupPanel 
	 * that is modal and does not auto-hide.
	 * 
	 * @param callback
	 * 		calls the onSuccess method of this callback with the answer: 
	 * 		true for the yes button, false for the no button, or null if 
	 * 		the window was (somehow) closed without pressing either the
	 * 		yes or no button.  The onFailure method of this callback is
	 * 		never called.
	 */
	public DataTypeSelectorPopup(final Map<String, DataColumnType> knownTypes,
                                 final int columnIndex, final String userColumnName,
                                 final AsyncCallback<UpdateInformation> callback) {
        this.knownTypes = knownTypes;
        this.columnNumber = columnIndex + 1;
        this.userColumnHeader = userColumnName;
        this.callback = callback;
        
		dataTypeLookup = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		unitsLookup = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        for (DataColumnType type : knownTypes.values()) {
            dataTypeLookup.put(type.getDisplayName(), type);
            unitsLookup.put(type.getDisplayName(), new ArrayList<>(type.getUnits()));
        }
            
        TextBox theBox = new TextBox();
        theBox.addHandler(new MyHandler("selector") , KeyUpEvent.getType());
        theBox.addHandler(new MyHandler("selector") , BlurEvent.getType());
		theBox.addHandler(new MyHandler("selector") , FocusEvent.getType());
        
		dataSelector = new MySuggestBox(getDataOracle(dataTypeLookup), theBox);
        
		initWidget(uiBinder.createAndBindUi(this));
//        FocusHandler fh = new FocusHandler() {
//            @Override
//            public void onFocus(FocusEvent focusEvent) {
//                ((TextBox)focusEvent.getSource()).setReadOnly(false);
//            }
//        };
//        cpBx.addFocusHandler(fh);
		
		parentPanel = new PopupPanel(false, true);
		parentPanel.setWidget(this);
        
        parentPanel.setAnimationType(AnimationType.ONE_WAY_CORNER);
        parentPanel.setAnimationEnabled(true);
        parentPanel.setGlassEnabled(true);
        parentPanel.setGlassStyleName("dataTypeSelectorGlass");
        
        unitsPnl.setVisible(false);

		selectButton.setText("Select");
		cancelButton.setText("Cancel");
		sendIt = null;

        init();
	}

    /**
     * 
     */
    private void init() {
        dataSelector.addSelectionHandler(new SelectionHandler<Suggestion>() {

            @Override
            public void onSelection(SelectionEvent<Suggestion> event) {
                String temp = dataSelector.getText();
                GWT.log("gettext: "+ temp);
                descText.setHTML(getDataDescription(temp, dataTypeLookup));
                
                // set listBox with units for datatype
                unitsListBox.clear();
                unitsPnl.setVisible(false);
                
                List<String> theList = getUnitsList(event.getSelectedItem().getReplacementString(), unitsLookup);
                if (theList == null || theList.isEmpty()) {
                    unitsPnl.setVisible(false);
                }
                else {
                    for ( String data : theList ) {
                        unitsListBox.addItem(data);
                    }
                    unitsPnl.setVisible(true);
                }
            }
        });
    }

	private DataTypeSuggestOracle getDataOracle(Map<String, DataColumnType> datatypeMap) {
      DataTypeSuggestOracle dataOracle = new DataTypeSuggestOracle();

      // set suggestion list for empty query
      dataOracle.setDefaultSuggestionsFromText(datatypeMap.keySet());
      
      // initialize dataOracle with prepopulated
      dataOracle.add(DEFAULT_SUGGEST_VALUE);
      
      for ( String theKey : datatypeMap.keySet() ){
    	  if (theKey.toLowerCase() == DEFAULT_SUGGEST_VALUE.toLowerCase()) {
    		  continue;
    	  }
          dataOracle.add(theKey);
      }

      return dataOracle;  
  }

    private String getDataDescription(String displayName, Map<String, DataColumnType> datatypeMap) { 
        String itemDescription = "";
        if (datatypeMap.get(displayName) != null ) {
            itemDescription = datatypeMap.get(displayName).getDescription();
        } else {
            itemDescription = "Unknown variable: " + displayName;
        }
        
        return itemDescription;
    }
    
    private List<String> getUnitsList(String displayName, Map<String, List<String>> unitsLookup) {

        List<String> unitsList = unitsLookup.containsKey(displayName) ?
                                    unitsLookup.get(displayName) :
                                    new ArrayList<>();
//        GWT.log("units list for " + displayName + " : "+ unitsList);

        for ( String item : unitsList ) {
            if (isEmptyString(item)) {
                // remove it from the selection box list, 
                // but NOT from the type definition.
                // Because shit depends on there being an empty string in the units list.
                unitsList.remove(item);
            }
        }
        
        return unitsList;
    }
    
    static boolean isEmptyString(String string) {
        return string == null || string.isEmpty();
    }
    
    static boolean isSuggested(String displayName, Map<String, DataColumnType> datatypeMap) { 
//    	GWT.log("textbox value is " + displayName + " : "+ datatypeMap);
        if (datatypeMap.get(displayName) != null ) {
            return true;
        } 
        else {
            return false;
        }
    }
    
    void show(DataColumnType columnType, ValueUpdater<String> contextUpdater, int x, int y) {
        GWT.log("Show : " + columnType);
        this.currentType = columnType;
        this.updater = contextUpdater;
        parentPanel.setPopupPosition(x, y);
        parentPanel.show();
        UploadDashboard.logToConsole("visible? " + parentPanel.isShowing());
        DataColumnSpecsPage.preventScroll(true);
    }

	void show(String userColumn) {
        setPrologue("Data Type for column " + userColumn);
//        Timer t = new Timer() {
//            @Override
//            public void run() {
//        		cpBx.setFocus(true);
//            }
//        };
//        t.schedule(500);
		parentPanel.center();
	}
    
	private void setPrologue(String username) {
	    prologue.setText("Update profile for user: " + username);
	}

	@UiHandler("selectButton")
	void yesOnClick(ClickEvent e) {
		if (isSuggested(dataSelector.getValueBox().getText(), dataTypeLookup)) {
			dataSelector.getValueBox().setText(DEFAULT_SUGGEST_VALUE);
		}

		sendIt = true;
		parentPanel.hide();
	}

    @UiHandler("cancelButton")
	void noOnClick(ClickEvent e) {
		sendIt = false;
		parentPanel.hide();
	}

    @UiHandler("showAllButton")
    void showAllOnClick(ClickEvent e) {
        showAllChoices();
    }
    
    /**
     * 
     */
    private void showAllChoices() {
        dataSelector.setText(null);  
        dataSelector.showSuggestionList();
        dataSelector.setFocus(true);
    }
    
    public void reset() {
    }
    
    public String getSelection() { return dataSelector.getValue().trim(); } // ???
    public String getUnits() { return unitsListBox.getSelectedItemText(); }
    
    
}
