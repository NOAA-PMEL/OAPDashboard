/**
 * 
 */
package gov.noaa.pmel.dashboard.actions;

import java.io.StringWriter;
import java.util.Date;

import org.jdom2.Document;

import com.kamb.utils.xml.GenXer;

import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.shared.DashboardOADSMetadata;

/**
 * @author kamb
 *
 */
public class OADSMetadata {

	private static class Extents {
		double maxValue;
		double minValue;
		Extents(double max, double min) {
			maxValue = max;
			minValue = min;
		}
	}
	private static class GeoTemporalExtents {
		Extents latExtents;
		Extents lonExtents;
		Extents timeExtents;
		GeoTemporalExtents(Extents lats, Extents lons, Extents times) {
			latExtents = lats;
			lonExtents = lons;
			timeExtents = times;
		}
	}
	static GeoTemporalExtents extractGeospatialTemporalExtents(StdUserDataArray stdArray) {
		double maxLat = Double.MIN_VALUE;
		double minLat = Double.MAX_VALUE;
		double maxLon = Double.MIN_VALUE;
		double minLon = Double.MAX_VALUE;
		double maxTime = Double.MIN_VALUE;
		double minTime = Double.MAX_VALUE;
		Double[] lats = stdArray.getSampleLatitudes();
		Double[] lons = stdArray.getSampleLongitudes();
		Double[] times = stdArray.getSampleTimes();
		for (int i=0; i<stdArray.getNumSamples(); i++) {
			double lat = lats[i].doubleValue();
			double lon = lons[i].doubleValue();
			double time = times[i].doubleValue();
			if ( lat > maxLat ) { maxLat = lat; }
			if ( lat < minLat ) { minLat = lat; }
			if ( lon > maxLon ) { maxLon = lon; }
			if ( lon < minLon ) { minLon = lon; }
			if ( time > maxTime ) { maxTime = time; }
			if ( time < minTime ) { minTime = time; }
		}
		GeoTemporalExtents extents = new GeoTemporalExtents(new Extents(maxLat, minLat), 
		                                                    new Extents(maxLon, minLon), 
		                                                    new Extents(maxTime, minTime));
		return extents;
	}
	
	public static DashboardOADSMetadata extractOADSMetadata(StdUserDataArray stdArray) {
		DashboardOADSMetadata oads = new DashboardOADSMetadata(stdArray.getDatasetId());
		GeoTemporalExtents gtExtents = extractGeospatialTemporalExtents(stdArray);
		oads.westernBound(gtExtents.lonExtents.minValue);
		oads.easternBound(gtExtents.lonExtents.maxValue);
		oads.northernBound(gtExtents.latExtents.maxValue);
		oads.southernBound(gtExtents.latExtents.minValue);
		oads.startDate(new Date((long)(1000*gtExtents.timeExtents.minValue)));
		oads.endDate(new Date((long)(1000*gtExtents.timeExtents.minValue)));
		return oads;
	}

	public static String createOadsMetadataXml(DashboardOADSMetadata mdata) {
		StringWriter writer = new StringWriter();
		GenXer xml = new GenXer(writer);
		xml.startXdoc();
		xml.startElement("metadata");
		if ( mdata.submitter() != null ) {
			
		}
		writer.flush();
		return writer.toString();
	}

	public static Document createOadsMetadataDoc(DashboardOADSMetadata mdata) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
