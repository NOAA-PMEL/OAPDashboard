/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;


/**
 * @author kamb
 *
 */
public class ObservationType {

    // Should be consistent with MetadataEditor.OracleController.ncei_observationTypes .. Maybe.
    public static final String[] types = new String[] {
            "Time-series (moorings, etc.)",
            "Surface measurements (underway, discrete)",
            "Profile (CTD, bottle, etc.)",
             "Gliders, etc.",
            "Pump cast",
            "Model output",
            "Field experiment",
            "Laboratory experiment",
            "Fish examination",
            "Biological tows",
            "Marine mammal observation",
            "Other"
    };
    public static final String[] oldTypes = new String[] {
            "Surface underway",
            "Surface (discrete samples)",
            "Profile (CTD continuous)",
            "Profile (discrete samples)",
             "Profile (gliders, etc.)",
            "Time-series",
            "Time-series (profile)",
            "Pump cast",
            "Model output",
            "Field experiment",
            "Laboratory experiment",
            "Fish examination",
            "Biological tows",
            "Marine mammal observation",
            "Other"
    };
    public static final String UNSPECIFIED = "n/a";
    
    /*
    public enum FeatureType {
        UNSPECIFIED("unspecified"),
        TIMESERIES("timeSeries"),
        TRAJECTORY("trajectory"),
        PROFILE("profile"),
        TIMESERIES_PROFILE("timeSeriesProfile"), 
        TRAJECTORY_PROFILE("trajectoryProfile"),
        OTHER("other");
    */
    /**
     * @param observationTypeName
     * @return Appropriate DSG FeatureType
     */
    public static FeatureType featureTypeOf(String observationTypeName) {
        if ( observationTypeName == null || observationTypeName.trim().length() == 0 ) {
            return FeatureType.UNSPECIFIED;
        }
        String obs = observationTypeName.toLowerCase();
        if ( obs.contains("surface")) {
            return FeatureType.TRAJECTORY;
        }
        if ( obs.contains("undulating")) {
            return FeatureType.TRAJECTORY_PROFILE;
        }
        if ( obs.equals("time-series (profile)")) {
            return FeatureType.TIMESERIES_PROFILE;
        }
        if ( obs.equals("time-series")) {
            return FeatureType.TIMESERIES;
        }
        if ( obs.contains("profile")) {
            return FeatureType.PROFILE;
        }
        System.out.println("OTHER feature type for observation: " + observationTypeName);
        return FeatureType.OTHER;
    }
}
