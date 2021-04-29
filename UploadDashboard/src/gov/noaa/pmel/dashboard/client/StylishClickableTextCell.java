/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.text.shared.SafeHtmlRenderer;

/**
 * @author kamb
 *
 */
public class StylishClickableTextCell extends ClickableTextCell {

    private String _baseStyle;
    private String _currentStyle;
    
    /**
     * 
     */
    public StylishClickableTextCell(String baseStyle) {
        _baseStyle = baseStyle;
        _currentStyle = baseStyle;
    }

//    /**
//     * @param renderer
//     */
//    public StylishClickableTextCell(SafeHtmlRenderer<String> renderer) {
//        super(renderer);
//        // TODO Auto-generated constructor stub
//    }

    public void addStyle(String add_style) {
        if ( add_style == null || add_style.trim() == "" ) { return; }
        if ( _currentStyle.contains(add_style)) { return; }
        _currentStyle += " " + add_style;
    }
    
    public void removeStyle(String remove_style) {
        if ( remove_style == null || remove_style.trim() == "" ) { return; }
        this._currentStyle = this._currentStyle.replaceAll(remove_style, "").trim();

    }
    public void setStyle(String new_style) {
        if ( new_style == null ) { new_style = ""; }
        _currentStyle = new_style;
    }
    
    public void resetStyle() {
        _currentStyle = _baseStyle;
    }
    
    public String currentStyle() {
        return _currentStyle;
    }
}
