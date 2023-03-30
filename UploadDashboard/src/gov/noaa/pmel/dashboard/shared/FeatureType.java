/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

/**
 * @author kamb
 *
 */
public enum FeatureType {
    UNSPECIFIED("unspecified"),
    TIMESERIES("timeSeries"),
    TRAJECTORY("trajectory"),
    PROFILE("profile"),
    TIMESERIES_PROFILE("timeSeriesProfile"), 
    TRAJECTORY_PROFILE("trajectoryProfile"),
    OTHER("other");
    
    String _dsgTypeName;
    
    FeatureType(String dsgTypeName) {
        _dsgTypeName = dsgTypeName;
    }
    
    public String dsgTypeName() { return _dsgTypeName; }
    
    public boolean isDSG() {
        return this != UNSPECIFIED && this != OTHER;
    }
    
    public static boolean isDSG(String featureTypeName) {
        try {
            return FeatureType.valueOf(featureTypeName.toUpperCase()).isDSG();
        } catch (Exception ex) {
            System.err.println(ex);
            return false;
        }
    }
}
