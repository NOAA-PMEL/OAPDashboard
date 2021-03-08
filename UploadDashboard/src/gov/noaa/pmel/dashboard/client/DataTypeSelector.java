package gov.noaa.pmel.dashboard.client;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Logger;

//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;	
//import org.json.simple.parser.ParseException;

import com.google.gwt.core.client.EntryPoint;
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
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

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
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionDisplay;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class DataTypeSelector implements EntryPoint {

	private static Logger logger = Logger.getLogger(DataTypeSelector.class.getName());
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		
		Map<String, String> data1lookup = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		data1lookup.put( "AA", "DescriptionA" );
		data1lookup.put( "BB", "Descriptionb" );
		data1lookup.put( "BC AABB", "Descriptionbc aabb" );
		data1lookup.put( "CC", "Descriptionc" );
		data1lookup.put( "DD", "Descriptiond" );
		data1lookup.put( "DDA", "Descriptiondda" );
		data1lookup.put( "DDB", "DescriptionddB" );
		data1lookup.put( "DDC", "" );
		data1lookup.put( "DDD", null );
		data1lookup.put( "EE", "Descriptione" );
		data1lookup.put( "FF", "Descriptionf" );
		data1lookup.put( "HH", "Descriptiong" );
		data1lookup.put( "II", "Descriptionh" );
		data1lookup.put( "AA BBAACC", "DescriptionAA BBAACC" );
		data1lookup.put( "BB AABBCC", "DescriptionBB AABBCC" );
		data1lookup.put( "BC AABCEE", "DescriptionBC AABCEE" );
		data1lookup.put( "CC CCSSEE", "DescriptionCC CCSSEE" );
		data1lookup.put( "DDA ADDV", "DescriptionDDA ADDV" );
		data1lookup.put( "EE", "DescriptionEE2" );
		
		Map<String, List<String>> data2lookup = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
		data2lookup.put("AA", new ArrayList<String>(Arrays.asList("unita1","unita2")));
		data2lookup.put("BB", new ArrayList<String>(Arrays.asList("unitb1","unitb2","unitb3")));
		data1lookup.put( "BC AABB", "Descriptionb" );
		data2lookup.put("CC", new ArrayList<String>(Arrays.asList("unitc1","unitc2")));
		data2lookup.put("DD", new ArrayList<String>(Arrays.asList("")));
		data2lookup.put("DDA", null);
		data2lookup.put("DDB", new ArrayList<String>(Arrays.asList()));
		data2lookup.put("DDD", new ArrayList<String>(Arrays.asList("unitd1","unitd2","unitd3","unitd4")));
		data2lookup.put("EE", new ArrayList<String>(Arrays.asList("unite1","unite2")));
		data2lookup.put("FF", new ArrayList<String>(Arrays.asList("unitf1","unitf2")));
		data2lookup.put("HH", new ArrayList<String>(Arrays.asList("unitg1","unitg2")));
		data2lookup.put("II", new ArrayList<String>(Arrays.asList("unith1","unith2")));
		
		final Button sendButton = new Button("Send");
		final TextBox dataTypeField = new TextBox();
		dataTypeField.setText("Click to Select Data Type");
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
		
//		
////		SuggestBox stockSuggestBox = new SuggestBox(stockOracle);
////		stockSuggestBox.setTitle("description goes here");
//		
//		// Create a handler for the sendButton and nameField
////		class MySuggestHandler implements ClickHandler, ChangeHandler, KeyUpHandler, KeyDownHandler {
//		class MySuggestHandler implements KeyUpHandler, KeyDownHandler, FocusHandler, DoubleClickHandler {
//			/**
//			 * Fired when the user clicks on the sendButton.
//			 */
//			public void onClick(ClickEvent event) {
//				GWT.log("SingleClick");
//			}
//			
//			public void onChange(ChangeEvent event) {
//				GWT.log("eventChanged");
//			}
//
//			/**
//			 * Fired when the user types in the nameField.
//			 */
//			public void onKeyUp(KeyUpEvent event) {
//				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
////					Window.alert(((TextBox)event.getSource()).getValue());
//					Window.alert("Hi keyup");
//				}
//			}
//			
//			public void onKeyDown(KeyDownEvent event) {
//		         if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER){
////		            Window.alert(((TextBox)event.getSource()).getValue());
//		            Window.alert("hi keydown");
//		         }
//			}
////			public void onValueChange(ValueChangeEvent<String> event) {
////	            if (!stockOracle.toString().contains(event.getValue())) {
////	            	stockSuggestBox.setValue("");
////	                Window.alert("value changed!") ;
////	            }
////	        }
//
//			@Override
//			public void onFocus(FocusEvent event) {
//				// TODO Auto-generated method stub
//				stockSuggestBox.getValueBox().selectAll();
//				GWT.log("onfocus " + stockSuggestBox.getValueBox().toString());
////				getTextBox().selectAll();
//			}
//
//			@Override
//			public void onDoubleClick(DoubleClickEvent event) {
//				// TODO Auto-generated method stub
//				GWT.log("onDoubleClick " + stockSuggestBox.getValueBox().toString());
//				stockSuggestBox.setFocus(true);
//				stockSuggestBox.showSuggestionList();
//			}
//		}
//
//		
//		
////		stockSuggestBox.addValueChangeHandler(new ValueChangeHandler<String>() {
////
////	        @Override
////	        public void onValueChange(ValueChangeEvent<String> event) {
////	            if (!stockOracle.toString().contains(event.getValue())) {
////	            	stockSuggestBox.setValue("");
////	                Window.alert("value changed!") ;
////	            }
////	        }
////	    });
//		
//		// Add a handler to send the name to the server
//		MySuggestHandler myHandler = new MySuggestHandler();
//		stockSuggestBox.getValueBox().addFocusHandler(myHandler);
////		stockSuggestBox.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>());
//		stockSuggestBox.addKeyUpHandler(myHandler);
//		stockSuggestBox.addKeyDownHandler(myHandler);
////		stockSuggestBox.addValueChangeHandler(handler);
//		
//		// Assemble suggestBox panel.
//		suggestPanel.add(stockSuggestBox);
		
		
		
		
		
		
		
		
		
		
		// create the suggestion box and pass it the data created above
		SuggestBox data1suggestionBox = new SuggestBox(getDataOracle(data1lookup));
//		SuggestionDisplay display = data1suggestionBox.getSuggestionDisplay();

		
		//set width
		data1suggestionBox.setWidth("200");
		
		// Add suggestionbox to the root panel. 
		HorizontalPanel data1Panel = new HorizontalPanel();
		data1Panel.add(data1suggestionBox);
		
		final ListBox listBox = new ListBox();
		final HorizontalPanel listPanel = new HorizontalPanel();
		listPanel.add(listBox);
		
		// Initially hidden
		listPanel.setVisible(false);
		

		
		
		data1suggestionBox.addSelectionHandler(new SelectionHandler<Suggestion>() {

			@Override
			public void onSelection(SelectionEvent<Suggestion> event) {
				String temp = data1suggestionBox.getText();
				logger.info("gettext: "+ temp);
				descrText.setHTML(getDataDescription(temp, data1lookup));
				descriptionPanel.add(descrText);
				
				
				// Testing
				SuggestionDisplay temp2 = data1suggestionBox.getSuggestionDisplay();
				logger.info("temp2: "+ temp2.toString());

				
				// set listBox with units for datatype
				listBox.clear();
				
				List<String> unitsList = getData2(event.getSelectedItem().getReplacementString(), data2lookup);
				if (unitsList.isEmpty()) {
					listPanel.setVisible(false);
				}
				else {
					for ( String data : getData2(event.getSelectedItem().getReplacementString(), data2lookup) ){
						listBox.addItem(data);
			        }

					listPanel.setVisible(true);
				}
			}
			
		});
		
		
		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel.get("nameFieldContainer").add(dataTypeField);
//		RootPanel.get("errorLabelContainer").add(errorLabel);

		// Focus the cursor on the name field when the app loads
		dataTypeField.setFocus(true);
		dataTypeField.selectAll();

		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Data Type Selector");
		dialogBox.setAnimationEnabled(true);
		final Button selectButton = new Button("Select");
		// We can set the id of a widget by accessing its Element
		selectButton.getElement().setId("selectButton");
		
		final Button cancelButton = new Button("Cancel");
		cancelButton.getElement().setId("cancelButton");
		
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		

	    VerticalPanel dialogVPanel = new VerticalPanel();
	    
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Description:</b>"));
		dialogVPanel.add(descriptionPanel);
		dialogVPanel.add(new HTML("<br>"));
	    
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Enter DataType:</b>"));
		dialogVPanel.add(data1Panel);
		dialogVPanel.add(new HTML("<br>"));
		
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Units:</b>"));
		dialogVPanel.add(listPanel);
		dialogVPanel.add(new HTML("<br>"));
		
		final HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setWidth("100%");
		buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
		buttonPanel.add(cancelButton);
		buttonPanel.add(selectButton);
		dialogVPanel.add(buttonPanel);
		dialogBox.setWidget(dialogVPanel);

		
		
		// Add a handler to select the DialogBox
		selectButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				String textFromSuggestBox = data1suggestionBox.getText();
				String textFromUnitsList = listBox.getSelectedItemText();
				
				String selectedDataType = "";
				
				if (data1lookup.containsKey(textFromSuggestBox)) {
					selectedDataType = textFromSuggestBox;
				}
				else {
					Window.alert("Error: Unknown Data Type");
				}
				
				if (!isEmptyString(textFromUnitsList)) {
					selectedDataType += " " + textFromUnitsList;
				}
				
				dataTypeField.setText(selectedDataType);

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
				logger.info("event.getSource() focus: " + event.getSource());
				listBox.clear();
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
				
				List<String> unitsList = getData2(checkValue, data2lookup);
				
				if (unitsList.isEmpty()) {
					listBox.clear();
					listPanel.setVisible(false);
				}
//				else {
//					for ( String data : unitsList ){
//						listBox.addItem(data);
//			        }
//		
//					listPanel.setVisible(true);
//				}

				
			}
			@Override
			public void onDoubleClick(DoubleClickEvent event) {
				// TODO Auto-generated method stub
				logger.info("event.getSource() doubleclik: " + event.getSource());
				logger.info("onDoubleClick " + data1suggestionBox.getValueBox().toString());
//				data1suggestionBox.setFocus(true);
				data1suggestionBox.setText(null);  
				data1suggestionBox.showSuggestionList();
				
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
				String textToServer = dataTypeField.getText();
//				if (!FieldVerifier.isValidName(textToServer)) {
//					errorLabel.setText("Please enter at least four characters");
//					return;
//				}

				// Then, we send the input to the server.
				sendButton.setEnabled(false);
				textToServerLabel.setText(textToServer);
				serverResponseLabel.setText("");
				dialogBox.center();
				data1suggestionBox.setFocus(true);
				
			}

			@Override
			public void onMouseOver(MouseOverEvent event) {
				// TODO Auto-generated method stub
				logger.info("event.getSource() mouseover: " + event.getSource());
				logger.info("onMouseOver " + data1suggestionBox.getValueBox().toString());
				
				data1suggestionBox.showSuggestionList();
				boolean suggestionShowing = data1suggestionBox.getSuggestionDisplay().isSuggestionListShowing();
				
//				if ( suggestionShowing ) {
//					Window.alert("showing!");
//				}
				
//				NodeList<Element> elements = Document.get().getElementsByTagName("input");
				NodeList<Element> elements = Document.get().getElementsByTagName("div");
				for (int i = 0; i < elements.getLength(); i++) {
					logger.info("elements1 " + elements.getItem(i).getClassName());
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
				logger.info("onContextMenu " + data1suggestionBox.getValueBox().toString());
			}
		}

		// Add a handlers to pass the constructed datatype between dataTypeField and data1suggestionBox
		MyHandler handler = new MyHandler();
		dataTypeField.addClickHandler(handler);
		dataTypeField.addKeyUpHandler(handler);
		data1suggestionBox.getValueBox().addBlurHandler(handler);
		data1suggestionBox.getValueBox().addDoubleClickHandler(handler);
//		data1suggestionBox.getValueBox().getElement().setAttribute("style" , "color: yellow;");
		data1suggestionBox.getValueBox().addFocusHandler(handler);
		data1suggestionBox.getValueBox().addMouseOverHandler(handler);
		
		
//		NodeList<Element> elems = Document.get().getElementsByTagName("div");
//				//getElementsByTagName("td");
//		int count = 0;
//		for (int i = 0; i < elems.getLength(); i++) {
//			
//			Element elem = elems.getItem(i);
//			logger.info("elem " + elem.getAttribute("role").toString());
//			Label l = Label.wrap(elem);
//			l.addClickHandler(new ClickHandler() {
//				@Override
//				public void onClick(ClickEvent event) {
//					Window.alert("yay!");
//				}
//			});
//			count = i++;
//			logger.info("div incremental  " + i);
//		}
//		logger.info("div count " + count);
		
	}
	
	
//	public void handleRowsSelectionStyles(ClickEvent event) {
//	    int selectedRowIndex = fieldTable.getCellForEvent(event).getRowIndex();
//	    int rowCount = fieldTable.getRowCount();
//	    for (int row = 0; row < rowCount; row++) {
//	        Element rowElem = fieldTable.getRowFormatter().getElement(row);
//	        rowElem.setClassName(row == selectedRowIndex ? "row selected" : "row");
//	    }
//	}
	
	private DataTypeSuggestOracle getDataOracle(Map<String, String> data1lookup) {  
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
	
	private String getDataDescription(String displayName, Map<String, String> data1lookup) { 
		String itemDescription = "";
		if (data1lookup.get(displayName) != null || (data1lookup.get(displayName) != null && data1lookup.containsKey(displayName))) {
			itemDescription = data1lookup.get(displayName);
        }
        
        return itemDescription;
    }
	
	private List<String> getData2(String displayName, Map<String, List<String>> data2lookup) {

		List<String> datalist = new ArrayList<String>();
		logger.info("displayName: "+ data2lookup.get(displayName));

		if (data2lookup.get(displayName) != null || (data2lookup.get(displayName) != null && data2lookup.containsKey(displayName))) {
			for ( String item : data2lookup.get(displayName) ){
				if (isEmptyString(item)) {
					data2lookup.get(displayName).remove(item);
				}
	        }
			datalist = data2lookup.get(displayName);
		}
		
		return datalist;
	}
	
	boolean isEmptyString(String string) {
	    return string == null || string.isEmpty();
	}
	
}
