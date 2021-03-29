package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.google.gwt.cell.client.ValueUpdater;

//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;	
//import org.json.simple.parser.ParseException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

import com.google.gwt.user.client.Window;
//import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.VerticalPanel;

import gov.noaa.pmel.dashboard.shared.DataColumnType;

import com.google.gwt.user.client.ui.SuggestBox.SuggestionDisplay;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class DataTypeSelectorWidget extends DialogBox {

	private static Logger logger = Logger.getLogger(DataTypeSelectorWidget.class.getName());
    private AsyncCallback<UpdateInformation> callback;
    private Map<String, DataColumnType> knownTypes;
    private DataColumnType currentType;
    private ValueUpdater<String> updater;
    private int columnNumber;
    private String userColumnHeader;
    
    public static int WIDTH = 430;
	
    public static class UpdateInformation {
        public DataColumnType newType;
        public ValueUpdater<String> updater;
        public UpdateInformation(DataColumnType newType, ValueUpdater<String> updater) {
            this.newType = newType;
            this.updater = updater;
        }
    }
	/**
	 * This is the entry point method.
	 */
    public DataTypeSelectorWidget(final Map<String, DataColumnType> knownTypes,
                                  final int columnIndex, final String userColumnName,
                                  final AsyncCallback<UpdateInformation> callback) {
        super();
        this.knownTypes = knownTypes;
        this.columnNumber = columnIndex + 1;
        this.userColumnHeader = userColumnName;
        init();
        this.callback = callback;
    }
    
    void show(DataColumnType columnType, ValueUpdater<String> contextUpdater, int x, int y) {
        GWT.log("Show : " + columnType);
        this.currentType = columnType;
        this.updater = contextUpdater;
        this.setAnimationType(AnimationType.ONE_WAY_CORNER);
        this.setAnimationEnabled(true);
        this.setPopupPosition(x, y);
        this.show();
        UploadDashboard.logToConsole("visible? " + this.isShowing());
        DataColumnSpecsPage.preventScroll(true);
    }
	void show(UIObject from) {
//		feedbackChoiceBox.setFocus(true);
        GWT.log("Showing dt selector");
        this.setAnimationType(AnimationType.ONE_WAY_CORNER);
        this.setAnimationEnabled(true);
		this.showRelativeTo(from);
	}

	public void init() {
		final DialogBox dialogBox = this;
        dialogBox.setModal(true);
        dialogBox.setGlassEnabled(true);
        dialogBox.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                GWT.log("Closing DTS");
                DataColumnSpecsPage.preventScroll(false);
            }
        });
		
		Map<String, DataColumnType> dataTypeLookup = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, List<String>> unitsLookup = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        for (DataColumnType type : knownTypes.values()) {
            dataTypeLookup.put(type.getDisplayName(), type);
            unitsLookup.put(type.getDisplayName(), new ArrayList<>(type.getUnits()));
        }
		
		final Button sendButton = new Button("Send");
//		final TextBox dataTypeField = new TextBox();
//		dataTypeField.setText("Click to Select Data Type");
		final Label errorLabel = new Label();
		
//		final HorizontalPanel suggestPanel = new HorizontalPanel();
//		final HorizontalPanel dtSuggestPanel = new HorizontalPanel();
		
//		final MultiWordSuggestOracle stockOracle = new MultiWordSuggestOracle();
//		
//		// Create Suggest Data
//		stockOracle.add("alkalinity, alkalinity description, alkalinityunits");
//		stockOracle.add("alkalinity QC, alkalinity QC description, alkalinity QC units");
//		stockOracle.add("ammonia, ammonia description, ammonia units");
//		stockOracle.add("ammonia QC, ammonia QC description, units QC");
//		stockOracle.add("ammonium, ammonium description, ammonium units");
//		stockOracle.add("ammonium QC, ammonium QC description, ammonium QC units");
//		stockOracle.add("bottle QC, bottle QC description, bottle QC units");
//		stockOracle.add("chlorophyll using PC filter, chlorophyll using PC filter description, chlorophyll using PC filter units");
//		stockOracle.add("chlorophyll using GFF filter, chlorophyll using GFF filter description, chlorophyll using GFF filter units");
//		stockOracle.add("chlorophyll QC, chlorophyll QC description, chlorophyll QC units");

		
		final String description = "";
		HTML descrText = new HTML(description);
		HTMLPanel descriptionPanel = new HTMLPanel("");
		descriptionPanel.add(descrText);
		
		
		final TextBox textBox = new TextBox();
	    final DataTypeSuggestionDisplay suggestionDisplay = new DataTypeSuggestionDisplay();
		
		// create the suggestion box and pass it the data created above
		SuggestBox dataTypeSuggestionBox = new SuggestBox(getDataOracle(dataTypeLookup), textBox, suggestionDisplay);
//		SuggestionDisplay display = data1suggestionBox.getSuggestionDisplay();

		//set width
		dataTypeSuggestionBox.setWidth("100%");
		
		// Add suggestionbox to the root panel. 
		HorizontalPanel data1Panel = new HorizontalPanel();
		data1Panel.add(dataTypeSuggestionBox);
		
		final ListBox unitsListBox = new ListBox();
        unitsListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                ListBox unitsList = (ListBox)event.getSource();
                GWT.log("unitChange:["+unitsList.getSelectedIndex()+"]"+unitsList.getSelectedItemText());
                // not implemented yet
//                if ( "OTHER".equals(unitsList.getSelectedItemText()) {
//                    showAddUnitWidget();
            }
        });
		final HorizontalPanel listPanel = new HorizontalPanel();
		listPanel.add(unitsListBox);
		
		// Initially hidden
		listPanel.setVisible(false);
		
		dataTypeSuggestionBox.addSelectionHandler(new SelectionHandler<Suggestion>() {

			@Override
			public void onSelection(SelectionEvent<Suggestion> event) {
				String temp = dataTypeSuggestionBox.getText();
				logger.info("gettext: "+ temp);
				descrText.setHTML(getDataDescription(temp, dataTypeLookup));
				descriptionPanel.add(descrText);
				
				// Testing
				SuggestionDisplay temp2 = dataTypeSuggestionBox.getSuggestionDisplay();
				logger.info("temp2: "+ temp2.toString());

				
				// set listBox with units for datatype
				unitsListBox.clear();
				
				List<String> unitsList = getUnitsList(event.getSelectedItem().getReplacementString(), unitsLookup);
				if (unitsList == null || unitsList.isEmpty()) {
					listPanel.setVisible(false);
				}
				else {
					for ( String data : unitsList ) {
						unitsListBox.addItem(data);
			        }
					listPanel.setVisible(true);
				}
			}
		});
		
		
		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
//        this.add(dataTypeField);
//		RootPanel.get("nameFieldContainer").add(dataTypeField);
//		RootPanel.get("errorLabelContainer").add(errorLabel);

		// Focus the cursor on the name field when the app loads
//		dataTypeField.setFocus(true);
//		dataTypeField.selectAll();

		// Create the popup dialog box
		dialogBox.setText("Data Type Selector for [" + columnNumber + "] " + userColumnHeader);
		dialogBox.setAnimationEnabled(true);
		final Button selectButton = new Button("Select");
		// We can set the id of a widget by accessing its Element
		selectButton.getElement().setId("selectButton");
		
		final Button cancelButton = new Button("Cancel");
		cancelButton.getElement().setId("cancelButton");
		
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		

	    VerticalPanel dialogVPanel = new VerticalPanel();
        dialogVPanel.setWidth("100%");
	    
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Description:</b>"));
		dialogVPanel.add(descriptionPanel);
		dialogVPanel.add(new HTML("<br>"));
	    
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Enter DataType:</b>"));
		dialogVPanel.add(data1Panel);
		dialogVPanel.add(new HTML("<br>"));
		
		dialogVPanel.addStyleName("dialogVPanel");
        dialogVPanel.setWidth(WIDTH+"px");
		dialogVPanel.add(new HTML("<b>Units:</b>"));
		dialogVPanel.add(listPanel);
		dialogVPanel.add(new HTML("<br>"));
		final HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setWidth("100%");
		buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
		buttonPanel.add(cancelButton);
		buttonPanel.add(selectButton);
		dialogVPanel.add(buttonPanel);
		dialogBox.setWidget(dialogVPanel);

		
		
		// Add a handler to select the DialogBox
		selectButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
                GWT.log("currentType:"+currentType);
				String textFromSuggestBox = dataTypeSuggestionBox.getText();
                GWT.log("text box: " + textFromSuggestBox);
                DataColumnType newSelectedType = dataTypeLookup.get(textFromSuggestBox);
                GWT.log("column type: " + newSelectedType);
                if ( currentType.equals(newSelectedType)) {
                    GWT.log("No change.");
                    return;
                }
				String textFromUnitsList = unitsListBox.getSelectedItemText();
				
				String selectedDataType = "";
				
				if (dataTypeLookup.containsKey(textFromSuggestBox)) {
					selectedDataType = textFromSuggestBox;
				}
				else {
					Window.alert("Error: Unknown Data Type");
				}
				
				if (!isEmptyString(textFromUnitsList)) {
					selectedDataType += " " + textFromUnitsList;
				}
				
                if ( newSelectedType == null ) {
                    Double sortOrder = 102.;
                    String displayName = textFromSuggestBox;
                    String newDescription = textFromSuggestBox;
                    boolean isCritical = false;
                    newSelectedType = new DataColumnType(textFromSuggestBox, sortOrder, 
                                                         displayName, newDescription, isCritical, 
                                                         Arrays.asList(new String[] {textFromUnitsList}));
                } else {
                    if ( ! newSelectedType.setSelectedUnit(textFromUnitsList)) {
                        GWT.log("Unknown unit " + textFromUnitsList + " for var " + newSelectedType.getDisplayName());
                    }
                }
                currentType = newSelectedType;
                callback.onSuccess(new UpdateInformation(newSelectedType, updater));
//				dataTypeField.setText(selectedDataType);
			}
		});
		
		// Add a handler to cancel the DialogBox
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();

			}
		});
		
		
		// Create a handler for the dataTypeField and data1suggestionBox
		class MyHandler implements ClickHandler, KeyUpHandler, FocusHandler, BlurHandler, DoubleClickHandler, MouseOverHandler, ContextMenuHandler {
			/**
			 * Fired when the user clicks on the sendButton.
			 */
			public void onClick(ClickEvent event) {
				GWT.log("OK!");
				sendNameToServer();
			}

			/**
			 * Fired when the user types in the nameField.
			 */
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					sendNameToServer();
				}
			}
			
			@Override
			public void onFocus(FocusEvent event) {
				// TODO Auto-generated method stub
				logger.fine("event.getSource() focus: " + event.getSource());
				unitsListBox.clear();
				listPanel.setVisible(false);
				
//				data1suggestionBox.showSuggestionList();
//				boolean suggestionShowing = data1suggestionBox.getSuggestionDisplay().isSuggestionListShowing();
//				
//				if ( suggestionShowing ) {
//					Window.alert("showing!");
//				}
//				

			}
			@Override
			public void onBlur(BlurEvent event) {
				// TODO Auto-generated method stub
				logger.info("event.getSource() blur: " + event.getSource());
				
				logger.info("event.getSource() value: " + ((TextBox)event.getSource()).getValue());
				String checkValue = (String)((TextBox)event.getSource()).getValue();
				
				List<String> unitsList = getUnitsList(checkValue, unitsLookup);
				unitsListBox.clear();
				
				if (unitsList.isEmpty()) {
					listPanel.setVisible(false);
				} else {
					for ( String data : unitsList ) {
						unitsListBox.addItem(data);
			        }
					listPanel.setVisible(true);
				}

				
			}
			@Override
			public void onDoubleClick(DoubleClickEvent event) {
				// TODO Auto-generated method stub
				logger.info("event.getSource() doubleclik: " + event.getSource());
				logger.info("onDoubleClick " + dataTypeSuggestionBox.getValueBox().toString());
//				data1suggestionBox.setFocus(true);
				dataTypeSuggestionBox.setText(null);  
				dataTypeSuggestionBox.showSuggestionList();
				
//				data1suggestionBox = new SuggestBox(getDataOracle(data1lookup));
//				data1suggestionBox.getSuggestOracle().setDefault
			}

			/**
			 * Send the constructed datatype from the dataTypeField back to the dialog suggestbox.
			 * 
			 */
			private void sendNameToServer() {
				// First, we validate the input.
				errorLabel.setText("");
				String textToServer = dataTypeSuggestionBox.getText();
//				if (!FieldVerifier.isValidName(textToServer)) {
//					errorLabel.setText("Please enter at least four characters");
//					return;
//				}

				// Then, we send the input to the server.
				sendButton.setEnabled(false);
				textToServerLabel.setText(textToServer);
				serverResponseLabel.setText("");
				dialogBox.center();
				dataTypeSuggestionBox.setFocus(true);
				
			}

			@Override
			public void onMouseOver(MouseOverEvent event) {
				// TODO Auto-generated method stub
//				logger.fine("event.getSource() mouseover: " + event.getSource());
//				logger.fine("onMouseOver " + dataTypeSuggestionBox.getValueBox().toString());
				
//				data1suggestionBox.showSuggestionList();
				boolean suggestionShowing = dataTypeSuggestionBox.getSuggestionDisplay().isSuggestionListShowing();
				
//				if ( suggestionShowing ) {
//					Window.alert("showing!");
//				}
				
//				NodeList<Element> elements = Document.get().getElementsByTagName("input");
				NodeList<Element> elements = Document.get().getElementsByTagName("div");
				for (int i = 0; i < elements.getLength(); i++) {
//					logger.info("elements1 " + elements.getItem(i).getClassName());
					if (elements.getItem(i).getClassName().contains("suggestPopupContent")) {
						Element childElement = elements.getItem(i).getFirstChildElement();
						logger.info("childElement " + childElement.getAttribute("aria-activedescendant"));
						logger.info("elementsin " + elements.getItem(i).getClassName());
						logger.info("elementsin " + elements.getItem(i).getNodeName());
//						logger.info("elementsin " + elements.getItem(i).getInnerHTML());
						logger.info("elementsin " + elements.getItem(i).getAttribute("aria-activedescendant"));
//						<div tabindex="0" role="menubar" class="" hidefocus="true" aria-activedescendant="gwt-uid-21" style="outline: 0px;"></div>
					}
					

				}
//				Document.get().getElementsByClassName();
				
				
//				NodeList<Element> elems = Document.get().getElementsByTagName("div");
//				NodeList<Element> elems = Document.get().
//				//getElementsByTagName("td");
//				int count = 0;
//				for (int i = 0; i < elems.getLength(); i++) {
//
//					Element elem = elems.getItem(i);
//					logger.info("elem " + elem.getClassName());
////					Label l = Label.wrap(elem);
////					l.addClickHandler(new ClickHandler() {
////						@Override
////						public void onClick(ClickEvent event) {
////							Window.alert("yay!");
////						}
////					});
//					count = i++;
//					logger.info("div incremental  " + i);
//				}
//				logger.info("div count " + count);
				
			}

			@Override
			public void onContextMenu(ContextMenuEvent event) {
				// TODO Auto-generated method stub
				logger.info("event.getSource() contectmenu: " + event.getSource());
				logger.info("onContextMenu " + dataTypeSuggestionBox.getValueBox().toString());
			}
		}

		// Add a handlers to pass the constructed datatype between dataTypeField and data1suggestionBox
		MyHandler handler = new MyHandler();
//		dataTypeField.addClickHandler(handler);
//		dataTypeField.addKeyUpHandler(handler);
		dataTypeSuggestionBox.getValueBox().addBlurHandler(handler);
		dataTypeSuggestionBox.getValueBox().addDoubleClickHandler(handler);
//		data1suggestionBox.getValueBox().getElement().setAttribute("style" , "color: yellow;");
		dataTypeSuggestionBox.getValueBox().addFocusHandler(handler);
		dataTypeSuggestionBox.getValueBox().addMouseOverHandler(handler);
		
		
	}
	
	
	private DataTypeSuggestOracle getDataOracle(Map<String, DataColumnType> data1lookup) {  
//        MultiWordSuggestOracle dataOracle = new MultiWordSuggestOracle();
        DataTypeSuggestOracle dataOracle = new DataTypeSuggestOracle();
        
//        for (Map.Entry<String, String> d : data1lookup.entrySet()) {
//        	logger.info("Key: "+ d.getKey() + " & Value: " + d.getValue());
//        }
        
        // set suggestion list for empty query
        dataOracle.setDefaultSuggestionsFromText(data1lookup.keySet());
        
        for ( String theKey : data1lookup.keySet() ){
        	dataOracle.add(theKey);
        }

        return dataOracle;  
    }
	
	private String getDataDescription(String displayName, Map<String, DataColumnType> data1lookup) { 
		String itemDescription = "";
		if (data1lookup.get(displayName) != null ) {
			itemDescription = data1lookup.get(displayName).getDescription();
        } else {
            itemDescription = "Unknown variable: " + displayName;
        }
        
        return itemDescription;
    }
	
	private List<String> getUnitsList(String displayName, Map<String, List<String>> unitsLookup) {

		List<String> unitsList = unitsLookup.containsKey(displayName) ?
		                            unitsLookup.get(displayName) :
		                            new ArrayList<>();
		logger.info("units list for " + displayName + " : "+ unitsList);

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
	
}
