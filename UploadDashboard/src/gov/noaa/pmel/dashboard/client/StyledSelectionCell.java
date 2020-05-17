/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

/**
 * @author kamb
 *
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.cell.client.AbstractInputCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.client.SafeHtmlTemplates.Template;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class StyledSelectionCell extends AbstractInputCell<String, String> {

    interface Template extends SafeHtmlTemplates {
        @Template("<option value=\"{0}\">{0}</option>")
        SafeHtml deselected(String option);

        @Template("<option value=\"{0}\" selected=\"selected\">{0}</option>")
        SafeHtml selected(String option);
    }

    private static Template template;

    private final HashMap<String, Integer> indexForOption = new HashMap<String, Integer>();

    private final List<String> options;

    private String style;

    public StyledSelectionCell(List<String> options) {
        super("change");
        if (template == null) {
            template = GWT.create(Template.class);
        }
        this.options = new ArrayList<String>(options);
        int index = 0;
        for (String option : options) {
            indexForOption.put(option, index++);
        }
    }

    public StyledSelectionCell(List<String> options, String style) {
        super("change");
        this.style = style;
        if (template == null) {
            template = GWT.create(Template.class);
        }
        this.options = new ArrayList<String>(options);
        int index = 0;
        for (String option : options) {
            indexForOption.put(option, index++);
        }
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value,
            NativeEvent event, ValueUpdater<String> valueUpdater) {
        super.onBrowserEvent(context, parent, value, event, valueUpdater);
        String type = event.getType();
        if ("change".equals(type)) {
            Object key = context.getKey();
            SelectElement select = parent.getFirstChild().cast();
            String newValue = options.get(select.getSelectedIndex());
            setViewData(key, newValue);
            finishEditing(parent, newValue, key, valueUpdater);
            if (valueUpdater != null) {
                valueUpdater.update(newValue);
            }
        }
    }

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
        // Get the view data.
        Object key = context.getKey();
        String viewData = getViewData(key);
        if (viewData != null && viewData.equals(value)) {
            clearViewData(key);
            viewData = null;
        }

        int selectedIndex = getSelectedIndex(viewData == null ? value
                : viewData);
        if (style != null && !"".equals(style)) {
            String html = "<select tabindex=\"-1\" class=\"" + style + "\">";
            sb.appendHtmlConstant(html);
        } else {
            sb.appendHtmlConstant("<select tabindex=\"-1\">");
        }
        int index = 0;
        for (String option : options) {
            if (index++ == selectedIndex) {
                sb.append(template.selected(option));
            } else {
                sb.append(template.deselected(option));
            }
        }
        sb.appendHtmlConstant("</select>");
    }

    private int getSelectedIndex(String value) {
        Integer index = indexForOption.get(value);
        if (index == null) {
            return -1;
        }
        return index.intValue();
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        if ( style == null ) { style = ""; }
        this.style = style.trim();
    }
    
    public void addStyle(String addedStyle) {
        if ( addedStyle == null || addedStyle.trim() == "" ) { return; }
        this.style = (addedStyle + " " + this.style).trim();
    }
    
    public void removeStyle(String removedStyle) {
        if ( removedStyle == null || removedStyle.trim() == "" ) { return; }
        this.style = this.style.replaceAll(removedStyle, "").trim();
    }

}