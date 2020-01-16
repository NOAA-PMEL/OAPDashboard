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
import lombok.Singular;

/**
 * @author kamb
 *
 */
@Setter(AccessLevel.NONE)
public class Timeseries {
    
    @Data
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode(callSuper=true)
    public static class TimeseriesPoint extends AbstractMeasurement {

        @Builder
        private TimeseriesPoint(String name, Number value, String units, Date time, GeoLocation where) {
            super(name, value, units, time, where);
        }
        
        public Date time() { return super.when(); }
        
    }
    
    private List<TimeseriesPoint> _measurements;
    
    private GeoLocation _where;
    
    public static Timeseries from(double[] values, Date[] times, String name, String units, GeoLocation where) {
        Timeseries ts = null;
//                new TimeseriesBuilder()
//                            .build();
        return ts;
    }
}
