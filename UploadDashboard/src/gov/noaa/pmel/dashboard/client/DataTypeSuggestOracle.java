package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Callback;
import com.google.gwt.user.client.ui.SuggestOracle.Request;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

public class DataTypeSuggestOracle extends SuggestOracle {
	private static Logger logger = Logger.getLogger(DataTypeSuggestOracle.class.getName());
//	private List<String> data;
//	ArrayList<String> items = new ArrayList<>();
//	ArrayList items = new ArrayList();
	ArrayList<String> items = new ArrayList<>();
	
	
	/*
	 * default limit suggests.
	 */
	private static final int LIMIT_DEFAULT = 25;
	public static final int LENGTH_MIN = 4;
	public static final int LENGTH_MAX = 11;
	
	private static String escape(String ds) {
		SafeHtmlBuilder accum = new SafeHtmlBuilder();
		accum.appendEscaped(ds);
	    return accum.toSafeHtml().asString();
	  }

	public DataTypeSuggestOracle() { }

	/**
	 * used for each matching Suggestion returned separate display and replacement strings
	 */
	public static class ContainsSuggestion implements Suggestion, IsSerializable
	{
		private String _value;
		private String _displayString;

		/**
		 * Constructor used by RPC.
		 */
		public ContainsSuggestion() { }

		/**
		 * Constructor for <code>ContainsSuggestion</code>.
		 */
		public ContainsSuggestion(String value, String displayString)
		{
			_value = value;
			_displayString = displayString;
		}

		public String getDisplayString()
		{
			return _displayString;
		}

		public Object getValue()
		{
			return _value;
		}
		
		public String getReplacementString()
		{
			return _value;
		}
	}
	
	private Response defaultResponse;
	
	public void requestDefaultSuggestions(Request request, Callback callback) {
		logger.info("RequestDefaultSuggestions");
		logger.info("DefaultResponse: " + defaultResponse.getSuggestions());
		logger.info("Request: " + request.getQuery());
		if (defaultResponse != null) {
			callback.onSuggestionsReady(request, defaultResponse);
		} else {
			super.requestDefaultSuggestions(request, callback);
		}
	}

	/**
	 * match the query, generate a response and pass to callback
	 */
	public void requestSuggestions(Request request, Callback callback) {
		logger.info("requestSuggestions-2-param");
		logger.info("DefaultResponse: " + defaultResponse.getSuggestions());
		logger.info("Request: " + request.getQuery());
		requestSuggestions(request, null, callback);
	}

	public void requestSuggestions(Request request, String filter, Callback callback) {
		logger.info("requestSuggestions-3-param");
		logger.info("DefaultResponse: " + defaultResponse.getSuggestions());
		logger.info("Request: " + request.getQuery());
		
//		SuggestOracle.Response response = new SuggestOracle.Response();
		int limit = 72;
		HashMap<String,String> itemsDisplay = new HashMap<String,String>();
		final List<ContainsSuggestion> suggestions = new ArrayList<>(limit);
		
		logger.info("filter: " + filter); // ?
		
		// first get all filtered items
		for (int i = 0; i < items.size(); i++) {
			String itemStr = (String)items.get(i);
			String itemValue = itemStr.toLowerCase();
//			logger.info("itemStr: " + itemStr); // display value
		
			if (filter!=null && !filter.equals("")) {
				itemValue = itemStr.toLowerCase();
				if (contains(itemValue, filter.toLowerCase())) {
//					filteredItems.add(itemStr);
					itemsDisplay.put(itemStr, itemValue);
				}
			} else {
//				filteredItems.add(itemStr);
				itemsDisplay.put(itemStr, itemValue);
			}
		}
//		logger.info("filteredItems: " + filteredItems);
//		logger.info("itemsDisplay: " + itemsDisplay.entrySet());
		
		if(request.getQuery().isEmpty()) {
			request.setQuery(null);
            super.requestDefaultSuggestions(request, callback);
		}
		
		if ( (request != null) && request.getQuery().length() >= 2 ) {
//			int limit = 72;
//			if (request.getLimit() > 0) {
//				limit = request.getLimit();
//			} 
//			else {
//				limit = DataTypeSuggestOracle.LIMIT_DEFAULT;
//			}
			
//			final List<ContainsSuggestion> suggestions = new ArrayList<>(limit);
			
			String query = request.getQuery();
			logger.info("query: " + query);
			
//			queryOptions(query);

//			if (query == "*") {
//				logger.info("query == *");
////				List<String> realSuggestions = items;
//				Collection<Suggestion> realSuggestions = new ArrayList<Suggestion>(items);
//				suggestions.addAll(realSuggestions);
//			}
			
			for (Entry<String, String> elem : itemsDisplay.entrySet()) { 
				if (elem.getValue().contains("ignored")) {
					logger.info("elem.getValue(ignored): " + elem.getValue());
					logger.info("elem.getKey(ignored): " + elem.getKey());
					suggestions.add(new ContainsSuggestion(
							elem.getKey(), 
							OptionSuggestion(elem.getKey(), elem.getValue(), query)
							)
					);
				}
				if (elem.getValue().contains(query.toLowerCase())) {
					logger.info("elem: " + elem.getValue());
					suggestions.add(new ContainsSuggestion(
							elem.getKey(), 
							OptionSuggestion(elem.getKey(), elem.getValue(), query)
							)
					);

					if (suggestions.size() >= limit) {
						break;
					}
				}
			}
//			response.setSuggestions(suggestions);
			
		}
//		logger.info("filteredItems: " + filteredItems);
		Response response = new Response(suggestions);
		
		callback.onSuggestionsReady(request, response);
	}
	
	private String OptionSuggestion(String displ, String val, String query)
    {
        String displayMatchName = displ;
		int begin = val.indexOf(query.toLowerCase());
        if (begin >= 0) {
            int end = begin + query.length();
            String match = displ.substring(begin, end);
            displayMatchName = displ.replaceFirst(match, "<strong>" + match + "</strong>");
        } else {
        	displayMatchName = displ;
        }
        return displayMatchName;
    }
	
	private boolean contains(String original, String filter) {
		if (filter.length() == 0) {
			return true;
		}
		if (filter.length() > original.length()) {
			return false;
		}
		for (int i = 0; i < original.length() - filter.length() + 1; i++) {
			if (original.charAt(i) == filter.charAt(0)) {
				boolean matches = true;
				for (int j = 0; j < filter.length(); j++) {
					if (original.charAt(i + j) != filter.charAt(j)) {
						matches = false;
						break;
					}
				}
				if (matches) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Sets the default suggestion collection.
	 *
	 * @param suggestionList the default list of suggestions
	 */
	public void setDefaultSuggestions(Collection<Suggestion> suggestionList) {
		this.defaultResponse = new Response(suggestionList);
	}
	/**
	 * A convenience method to set default suggestions using plain text strings.
	 *
	 * Note to use this method each default suggestion must be plain text.
	 *
	 * @param suggestionList the default list of suggestions
	 */
	public final void setDefaultSuggestionsFromText(Collection<String> suggestionList) {
		Collection<Suggestion> accum = new ArrayList<Suggestion>();
		for (String candidate : suggestionList) {
			accum.add(createSuggestion(candidate, SafeHtmlUtils.htmlEscape(candidate)));
		}
		setDefaultSuggestions(accum);
	}
	
	/**
	 * Creates the suggestion based on the given replacement and display strings.
	 *
	 * @param replacementString the string to enter into the SuggestBox's text box
	 *          if the suggestion is chosen
	 * @param displayString the display string
	 *
	 * @return the suggestion created
	 */
	protected ContainsSuggestion createSuggestion(
			String replacementString, @IsSafeHtml String displayString) {
		return new ContainsSuggestion(replacementString, displayString);
	}

	/**
	 * Adds a suggestion to the oracle
	 */
	public void add(String suggestion)
	{
		items.add(suggestion);
	}

	/**
	 * Adds all suggestions specified
	 */
	public void addAll(Collection<String> collection)
	{
		items.addAll(collection);
	}

	/**
	 * interpreted as HTML
	 */
	public boolean isDisplayStringHTML() {
		return true;
	}
}
