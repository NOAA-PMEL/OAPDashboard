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
}
