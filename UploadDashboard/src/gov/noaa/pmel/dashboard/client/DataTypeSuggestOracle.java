package gov.noaa.pmel.dashboard.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.SuggestOracle;

public class DataTypeSuggestOracle extends SuggestOracle {
	private static Logger logger = Logger.getLogger(DataTypeSuggestOracle.class.getName());
//	private List<String> data;
//	ArrayList<String> items = new ArrayList<>();
	HashMap<String,String> itemsDisplay = new HashMap<String,String>();
	
	
	/*
	 * default limit suggests.
	 */
//	private static final int LIMIT_DEFAULT = 25;
//	public static final int LENGTH_MIN = 4;
//	public static final int LENGTH_MAX = 11;
	
	private static String escape(String ds) {
		SafeHtmlBuilder accum = new SafeHtmlBuilder();
		accum.appendEscaped(ds);
	    return accum.toSafeHtml().asString();
	  }

    private String escapeRx(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if ( c == '(' || c == ')' ) {
                sb.append("\\");
            }
            sb.append(c);
        }
        return sb.toString();
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
	
    @Override
	public void requestDefaultSuggestions(Request request, Callback callback) {
		logger.info("RequestDefaultSuggestions");
		logger.info("DefaultResponse: " + defaultResponse);
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
    @Override
	public void requestSuggestions(Request request, Callback callback) {
		logger.info("requestSuggestions-2-param");
        logger.info("callback: " + callback);
		logger.info("Request: " + request.getQuery());
		
		int limit = 72;
		final List<ContainsSuggestion> suggestions = new ArrayList<>(limit);

        String query = request.getQuery().trim();
		logger.info("query: " + query);
			
//			if (query == "*") {
//				logger.info("query == *");
////				List<String> realSuggestions = items;
//				Collection<Suggestion> realSuggestions = new ArrayList<Suggestion>(items);
//				suggestions.addAll(realSuggestions);
//			}
			
			for (Entry<String, String> elem : itemsDisplay.entrySet()) { 
				if (elem.getValue().contains("ignored")) {
//					logger.info("elem.getValue(ignored): " + elem.getValue());
//					logger.info("elem.getKey(ignored): " + elem.getKey());
                    continue;
				}
				if (elem.getValue().contains(query.toLowerCase())) {
//					logger.info("elem: " + elem.getValue());
					suggestions.add(new ContainsSuggestion(
							elem.getKey(), 
							OptionSuggestion(elem.getKey(), elem.getValue(), query)
							)
					);

					if (suggestions.size() >= limit-1) {
						break;
					}
				}
			}
			suggestions.add(new ContainsSuggestion("ignored", "IGNORED"));
			if (suggestions.size() == 1) {
			    suggestions.add(new ContainsSuggestion("(unknown)", "(unknown)"));
			}
            
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
            displayMatchName = displ.replaceFirst(escapeRx(match), "<strong>" + match + "</strong>");
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
//		items.add(suggestion);
        itemsDisplay.put(suggestion, suggestion.toLowerCase());
	}

	/**
	 * Adds all suggestions specified
	 */
	public void addAll(Collection<String> collection)
	{
//		items.addAll(collection);
        for (String item : collection) {
            add(item);
        }
	}

	/**
	 * interpreted as HTML
	 */
	public boolean isDisplayStringHTML() {
		return true;
	}
}
