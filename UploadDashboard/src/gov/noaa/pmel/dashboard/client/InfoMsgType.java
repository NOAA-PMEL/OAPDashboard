/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

public enum InfoMsgType {
    PLAIN("images/blank_1px.gif", "emptyDialogIcon"),
    QUESTION("images/questionMark_64px.png"),
    WARNING("images/warning_64px.png"),
    CRITICAL("images/warning_64px-redblack.png");
    
    private String _iconSrc;
    private String _iconStyleClass = "askDialogIcon";
    
    private InfoMsgType(String iconSrc) {
        this._iconSrc = iconSrc;
    }
    private InfoMsgType(String iconSrc, String iconStyleClass) {
        this(iconSrc);
        _iconStyleClass = iconStyleClass;
    }
    String iconSrc() { return _iconSrc; }
    String iconStyleClass() { return _iconStyleClass; }
}