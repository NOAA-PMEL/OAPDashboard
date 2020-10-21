/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author kamb
 *
 */
public class PreviewPlotImage implements Serializable, IsSerializable {
	
	private static final long serialVersionUID = -122264836136311787L;
	
	public String fileName;
	public String imageTitle;

	@SuppressWarnings("unused") // GWT
	private PreviewPlotImage() {
	}
	
	public PreviewPlotImage(String plotFileName) {
		fileName = plotFileName;
		imageTitle = buildTitle(plotFileName);
	}

	public PreviewPlotImage(String plotFileName, String title) {
		fileName = plotFileName;
		imageTitle = title;
	}

	private static String buildTitle(String plotFileName) {
		int extIdx = plotFileName.lastIndexOf('.');
        if ( extIdx < 0 ) { return plotFileName; }
		String name = plotFileName.substring(0, extIdx);
        int subIdx = name.indexOf('_');
        if ( subIdx < 0 ) { return name; }
		name = name.substring(subIdx+1);
		if ( isSpecial(name)) {
			return specialCase(name);
		}
		String[] variables = name.split("__");
		return variables[0] + " vs " + variables[1];
	}
	private static boolean isSpecial(String name) {
		return name.indexOf("__") < 0 ||
				"map".equals(name);
	}				
	private static String specialCase(String name) {
		if ( "map".equals(name)) {
			return "Location map";
		} else if ( name.indexOf("show_time") >= 0 ) {
			return "Observation dates";
		}
		return name;
	}
    
	@Override
	public String toString() {
	    return imageTitle + ":" + fileName;
	}
}
