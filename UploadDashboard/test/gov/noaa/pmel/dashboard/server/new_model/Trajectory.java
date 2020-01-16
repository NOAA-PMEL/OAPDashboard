/**
 * 
 */
package gov.noaa.pmel.dashboard.server.new_model;

import java.util.Date;
import java.util.List;

import gov.noaa.pmel.tws.types.GeoLocation;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

/**
 * @author kamb
 *
 */
public class Trajectory {

    @Data
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode(callSuper=true)
    public static class TrajectoryMeasurement extends AbstractMeasurement {

        @Builder
        private TrajectoryMeasurement(String name, Number value, String units, Date time, GeoLocation where) {
            super(name, value, units, time, where);
        }
        
        public Date time() { return super.when(); }
        public GeoLocation location() { return super.where(); }
        
    }

    private List<TrajectoryMeasurement> _measurements;
    
    public static Trajectory from(double[] values, GeoLocation[] locations, Date[] times, String name, String units) {
       return null;
    }
}
