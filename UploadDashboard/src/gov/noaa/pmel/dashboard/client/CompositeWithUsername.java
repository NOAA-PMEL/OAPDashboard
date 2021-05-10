/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;

/**
 * A Composite with a username property.
 * 
 * @author Karl Smith
 */
public abstract class CompositeWithUsername extends Composite {

	public static final String WELCOME_INTRO = "Logged in as ";
    
	private String username = "";
    private String pageName = "";

    @UiField ApplicationHeaderTemplate header;
    
//    abstract void setPageInfo(); // XXX replace setUsername
    
    protected CompositeWithUsername(String pageHistoryName) {
        pageName = pageHistoryName;
    }
    
    protected ApplicationHeaderTemplate header() {
        return header;
    }
    
	/**
	 * @return 
	 * 		the username; never null but may be empty
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username 
	 * 		the username to set; if null, an empty string is assigned
	 */
	public void setUsername(String username) {
		if ( username == null )
			this.username = "";
		else
			this.username = username;
	}
    
	public String pageName() { return pageName; }
    
//    @UiField protected ApplicationFooter footer;
//	public ApplicationFooter getFooter() {
	public void setBuildVersion(String version) {
        Element build = Document.get().getElementById("appBuildDisplay");
        if ( build != null ) {
            build.setInnerHTML(version);
        } else {
            GWT.log("Did not find appBuildDisplay element");
        }
	}

    void showing() {
        // alert the page that it is being displayed in case it has anything to do
    }
}
