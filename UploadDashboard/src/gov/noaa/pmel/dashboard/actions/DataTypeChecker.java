/**
 * 
 */
package gov.noaa.pmel.dashboard.actions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;

/**
 * @author kamb
 *
 */
public class DataTypeChecker {

    private static Logger logger = LogManager.getLogger(DataTypeChecker.class);
    
    public DataTypeChecker() {
    }

    public static boolean couldBeTrajectory(DashboardDatasetData ddd) {
        if ( ! ( hasLatitude(ddd) && hasLongitude(ddd))) { 
            logger.info("data does not appear to have location information.");
            return false; 
        }
        if ( ! hasDateTime(ddd)) {
            logger.info("data does not appear to have time information.");
            return false; 
        }
//        boolean hasDepth = ddd.hasSampleDepth();
//        Double[] depths = hasDepth ? ddd.getSampleDepths() : null;
        double[][] latlons = getSampleLatLons(ddd);
        double lastLat = Double.NaN;
        double lastLon = Double.NaN;
        
        for (int idx = 0; idx < latlons.length; idx++ ) {
            double thisLat = latlons[idx][0];
            double thisLon = latlons[idx][1];
            if ( thisLat == lastLat &&
                 thisLon == lastLon) {
                logger.info("two successive locations the same.");
                return false;
            }
            lastLat = thisLat;
            lastLon = thisLon;
        }
        return true;
    }

    private static double[][] getSampleLatLons(DashboardDatasetData ddd) {
        int lonColIdx = indexOf(ddd, DashboardUtils.LONGITUDE);
        if ( lonColIdx < 0 ) { throw new IllegalStateException("No longitude column found."); }
        int latColIdx = indexOf(ddd, DashboardUtils.LATITUDE);
        if ( latColIdx < 0 ) { throw new IllegalStateException("No latitude column found."); }
        ArrayList<ArrayList<String>>dataRows = ddd.getDataValues();
        double[][] latlons = new double[dataRows.size()][2];
        int rowIdx = 0;
        for (ArrayList<String> row : dataRows) {
            double lat = Double.parseDouble(row.get(latColIdx));
            latlons[rowIdx][0] = lat;
            double lon = Double.parseDouble(row.get(lonColIdx));
            latlons[rowIdx][1] = lon;
            rowIdx += 1;
        }
        return latlons;
    }

    private static boolean hasDateTime(DashboardDatasetData ddd) {
        return hasDate(ddd) && hasTime(ddd);
    }
    
    private static boolean hasDate(DashboardDatasetData ddd) {
        return ( lookFor(ddd, DashboardUtils.TIMESTAMP) != null ) ||
               ( lookFor(ddd, DashboardUtils.DATE) != null );
    }
    
    private static boolean hasTime(DashboardDatasetData ddd) {
        return ( lookFor(ddd, DashboardUtils.TIMESTAMP) != null ) ||
               ( lookFor(ddd, DashboardUtils.TIME_OF_DAY) != null ) ||
               ( lookFor(ddd, DashboardUtils.HOUR_OF_DAY) != null && lookFor(ddd, DashboardUtils.MINUTE_OF_HOUR) != null);
    }

    private static boolean hasLongitude(DashboardDatasetData ddd) {
        DataColumnType column = lookFor(ddd, DashboardUtils.LONGITUDE);
        return column != null;
    }

    private static boolean hasLatitude(DashboardDatasetData ddd) {
        DataColumnType column = lookFor(ddd, DashboardUtils.LATITUDE);
        return column != null;
    }

    private static DataColumnType lookFor(DashboardDatasetData ddd, DataColumnType columnType) {
        DataColumnType foundColumn = null;
        for (DataColumnType dataColumn : ddd.getDataColTypes()) {
            if ( dataColumn.typeNameEquals(columnType)) {
                foundColumn = dataColumn;
                break;
            }
        }
        return foundColumn;
    }

    private static int indexOf(DashboardDatasetData ddd, DataColumnType columnType) {
        int foundIndex = -1;
        int idx = 0;
        for (DataColumnType dataColumn : ddd.getDataColTypes()) {
            if ( dataColumn.typeNameEquals(columnType)) {
                foundIndex = idx;
                break;
            }
            idx++;
        }
        return foundIndex;
    }

    public static boolean couldBeProfile(DashboardDatasetData ddd) {
        if ( ! hasCastIdColumn(ddd)) {
            logger.info("data does not appear to have profile or cast id.");
            return false;
        }
        return true;
    }

    private static boolean hasStationIdColumn(DashboardDatasetData ddd) {
        return indexOf(ddd, DashboardUtils.STATION_ID) >= 0;
    }

    private static boolean hasCastIdColumn(DashboardDatasetData ddd) {
        return indexOf(ddd, DashboardUtils.CAST_ID) >= 0;
    }

    public static boolean couldBeProfileTimeseries(DashboardDatasetData ddd) {
        boolean couldBe = false;
        return couldBe;
    }
    
    private static void dumpProperties() {
        Properties props = System.getProperties();
        Enumeration<Object> keys = props.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            System.out.println(key + " : " + props.get(key));
        }
    }
    
    public static void check(String filePath, String fileFormat) {
        try ( BufferedReader profileReader = new BufferedReader(new FileReader(filePath));  ) {
            String filename = "upload.file";
            TreeMap<String, DashboardDatasetData> dmap = dfh.createDatasetsFromInput(profileReader, fileFormat, "jkamb", filename,
                                                                                     filename, new Date().toString(), null, null);
            for (String datasetId : dmap.keySet()) {
                DashboardDatasetData ddd = dmap.get(datasetId);
                System.out.println(ddd.getDataColTypes());
                System.out.println(datasetId + " - Could be profile:"+ couldBeProfile(ddd));
                System.out.println(datasetId + " - Could be trajectory:"+ couldBeTrajectory(ddd));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static DashboardConfigStore store;
    static DataFileHandler dfh;
    
    public static void main(String[] args) {
        String profileId = "CHABA092013";
        String profileFileName = "/local/data/ocean/oap/PRISM_NANOOS-split-orig/"+profileId+".tsv";
        String socatId = "33GG20171112";
        String socatFileName = "/local/data/ocean/socat/"+socatId+".tsv";
        String mooringId = "NH914";
        String mooringFileName = "/local/data/ocean/moorings/bgc/0146024/2.2/data/1-data/CCE_water_samples_200912_201504_v20161208.csv";
        try {
            store = DashboardConfigStore.get(false);
            dfh = store.getDataFileHandler();
            check(profileFileName, DashboardUtils.TAB_FORMAT_TAG);
            check(socatFileName, DashboardUtils.TAB_FORMAT_TAG);
            check(mooringFileName, DashboardUtils.COMMA_FORMAT_TAG);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
