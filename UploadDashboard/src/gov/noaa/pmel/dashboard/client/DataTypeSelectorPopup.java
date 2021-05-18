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
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.PopupPanel.AnimationType;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.UIObject;

import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;

/**
 * Column DataType Selector in a Popup with an Auto-complete suggestion box.
 * 
 * @author Linus Kamb
 * @author Dale Gamble
 */
public class DataTypeSelectorPopup extends Composite implements AttachEvent.Handler {

	interface DataTypeSelectorPopupUiBinder extends UiBinder<Widget, DataTypeSelectorPopup> {
	}

    protected static final String INVALID_CHOICE_MSG = 
           "You must select one of the known types or IGNORED.<br/><br/>" +
           "Adding new variable types is not currently supported.";
            

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
    TextBox theBox;

	private PopupPanel parentPanel;
	HandlerRegistration handler;

    private DataColumnType currentType;

    private ValueUpdater<String> updater;

    private Map<String, DataColumnType> knownTypes;

    private int columnIdx;
    private int columnNumber;
    
    private String userColumnHeader;

    private AsyncCallback<UpdateInformation> callback;
    
    public static Map<String, DataColumnType> dataTypeLookup;
    private Map<String, List<String>> unitsLookup;
    
    final String DEFAULT_SUGGEST_VALUE = "ignored";

    private Timer showInvalidChoiceTimer;
    
    public static int WIDTH = 665;
    
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
        this.columnIdx = columnIndex;
        this.columnNumber = columnIndex + 1;
        this.userColumnHeader = userColumnName;
        this.callback = callback;
        
		dataTypeLookup = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		unitsLookup = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        for (DataColumnType type : knownTypes.values()) {
            dataTypeLookup.put(type.getDisplayName(), type);
            unitsLookup.put(type.getDisplayName(), new ArrayList<>(type.getUnits()));
        }
            
        theBox = new TextBox();
        MyHandler theHandler = new MyHandler("theBox");
        theBox.addHandler(theHandler , KeyUpEvent.getType());
        
		dataSelector = new MySuggestBox(getDataOracle(dataTypeLookup), theBox, this);
        
		initWidget(uiBinder.createAndBindUi(this));
		
        prologue.setText("Select data type for column " + columnNumber + ": " + userColumnHeader);
        
		parentPanel = new PopupPanel(false, true);
		parentPanel.setWidget(this);
        
        parentPanel.setAnimationType(AnimationType.ONE_WAY_CORNER);
        parentPanel.setAnimationEnabled(true);
        parentPanel.setGlassEnabled(true);
        parentPanel.setGlassStyleName("dataTypeSelectorGlass");
        
        unitsPnl.setVisible(false);

		selectButton.setText("Select");
		cancelButton.setText("Cancel");
		// enabled on selection from the list.
        selectButton.setEnabled(false);
        
        init();
	}
    
    @Override
    public void onAttachOrDetach(AttachEvent event) {
        GWT.log("MyDeco attach:"+event.isAttached());
        UIObject that = this;
        if (!event.isAttached() ) {
            showInvalidChoiceTimer = new Timer() {
                @Override
                public void run() {
                    String temp = dataSelector.getText();
                    DataColumnType type = dataTypeLookup.get(temp);
                    if ( type == null ) {
                        GWT.log("Invalid choice:"+temp);
                        UploadDashboard.showMessageAt(INVALID_CHOICE_MSG, that);
                    }
                }
            };
            showInvalidChoiceTimer.schedule(150);
        } else {
            if (showInvalidChoiceTimer != null) {
                showInvalidChoiceTimer.cancel();
                showInvalidChoiceTimer = null;
            }
        }
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
                
                selectButton.setEnabled(true);
                
                List<String> theList = getUnitsList(event.getSelectedItem().getReplacementString(), unitsLookup);
                String labelText = "date".equalsIgnoreCase(temp) ? "Format:" : "Units:";
                setUnits(theList, 0, labelText);
            }
        });
        unitsListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                GWT.log("units changed:"+event);
                selectButton.setEnabled(true);
            }
        });
    }
    
    void setUnits(List<String> unitsList, int selectedIndex, String labelText) {
        GWT.log("set units:"+unitsList + 
                ( unitsList != null ? " (" + unitsList.size() + ")" : "()" ) +
                ", idx:"+ selectedIndex);
        if (unitsList == null || unitsList.isEmpty() || unitsList.get(0).equals("")) {
            unitsListBox.clear();
            unitsPnl.setVisible(false);
        }
        else {
            for ( String data : unitsList ) {
                unitsListBox.addItem(data);
            }
            unitsListBox.setSelectedIndex(selectedIndex);
            unitsLbl.setText(labelText);
            unitsPnl.setVisible(true);
        }
    }

	private static DataTypeSuggestOracle getDataOracle(Map<String, DataColumnType> datatypeMap) {
      DataTypeSuggestOracle dataOracle = new DataTypeSuggestOracle();

      // set suggestion list for empty query
      dataOracle.setDefaultSuggestionsFromText(datatypeMap.keySet());
      
      for ( String theKey : datatypeMap.keySet() ){
          dataOracle.add(theKey);
      }

      return dataOracle;  
  }

    private static String getDataDescription(String displayName, Map<String, DataColumnType> datatypeMap) { 
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
		theBox.setFocus(true);
        if ( ! ( columnType.equals(DashboardUtils.UNKNOWN) ||
                 columnType.equals(DashboardUtils.OTHER))) {
            dataSelector.setValue(columnType.getDisplayName());
            descText.setText(columnType.getDescription());
        }
        String labelText = columnType.typeNameEquals(DashboardUtils.DATE) ? "Format:" : "Units:";
        setUnits(columnType.getUnits(), columnType.getSelectedUnitIndex(), labelText);
        UploadDashboard.logToConsole("visible? " + parentPanel.isShowing());
        DataColumnSpecsPage.preventScroll(true);
    }

	@UiHandler("selectButton")
	void yesOnClick(ClickEvent e) {
		if (!isSuggested(dataSelector.getValueBox().getText(), dataTypeLookup)) {
			dataSelector.getValueBox().setText(DEFAULT_SUGGEST_VALUE);
		}
		String selected = dataSelector.getValueBox().getText();
        DataColumnType newType = dataTypeLookup.get(selected).duplicate();
        GWT.log("newType:"+newType);
        if ( newType.getUnits().size() > 0 ) {
            int selectedUnits = unitsListBox.getSelectedIndex();
            GWT.log("selectedIdx:"+selectedUnits);
            newType.setSelectedUnitIndex(selectedUnits);
        }
        UpdateInformation result = new UpdateInformation(columnIdx, newType, updater);
        callback.onSuccess(result);
		parentPanel.hide();
	}

    @UiHandler("cancelButton")
	void noOnClick(ClickEvent e) {
        reset();
		parentPanel.hide();
	}

    @UiHandler("showAllButton")
    void showAllOnClick(ClickEvent e) {
        selectButton.setEnabled(false);
        showAllChoices();
    }
    
    /**
     * 
     */
    void showAllChoices() {
        reset();
        dataSelector.showSuggestionList();
        dataSelector.setFocus(true);
    }
    
    public void reset() {
        dataSelector.setText(null);  
        descText.setText("");
        setUnits(null, 0, "Units:");
        unitsPnl.setVisible(false);
        selectButton.setEnabled(false);
    }
    
    public String getSelection() { return dataSelector.getValue().trim(); } // ???
    public String getUnits() { return unitsListBox.getSelectedItemText(); }
    
    
}
