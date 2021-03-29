package gov.noaa.pmel.dashboard.client;

import java.util.Collection;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionCallback;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

public class DataTypeSuggestionDisplay extends DefaultSuggestionDisplay {

	private static Logger logger = Logger.getLogger(DataTypeSelector.class.getName());
	
	private Widget suggestionMenu;
	
	public DataTypeSuggestionDisplay() {
		super();
	}

	@Override
	protected Widget decorateSuggestionList(Widget suggestionList) {
		// TODO Auto-generated method stub
		// return super.decorateSuggestionList(suggestionList);
		suggestionMenu = suggestionList;
        return suggestionList;
	}

	@Override
	protected void moveSelectionDown() {
		// TODO Auto-generated method stub
		super.moveSelectionDown();
		scrollSelectedItemIntoView();
	}

	@Override
	protected void moveSelectionUp() {
		// TODO Auto-generated method stub
		super.moveSelectionUp();
		scrollSelectedItemIntoView();
	}

	// SET popup same size as textbox
	@Override
	protected void showSuggestions(SuggestBox suggestBox, Collection<? extends Suggestion> suggestions, boolean isDisplayStringHTML, boolean isAutoSelectEnabled, SuggestionCallback callback) {
		// TODO Auto-generated method stub
		super.showSuggestions(suggestBox, suggestions, isDisplayStringHTML, isAutoSelectEnabled, callback);
		getPopupPanel().setWidth((suggestBox.getElement().getAbsoluteRight() - suggestBox.getAbsoluteLeft()) + Unit.PX.getType());
	}
	
	private void scrollSelectedItemIntoView() {
        getSelectedMenuItem().getElement().scrollIntoView();
    }
	
	private native MenuItem getSelectedMenuItem() /*-{
    	var menu = this.@com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay::suggestionMenu;
    	return menu.@com.google.gwt.user.client.ui.MenuBar::selectedItem;
	}-*/;

}
