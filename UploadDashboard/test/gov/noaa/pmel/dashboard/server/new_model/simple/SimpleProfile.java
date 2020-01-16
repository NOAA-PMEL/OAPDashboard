/**
 * 
 */
package gov.noaa.pmel.dashboard.server.new_model.simple;

import gov.noaa.pmel.tws.types.GeoLocation;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

/**
 * @author kamb
 *
 */
@Data
@Builder
@Setter(AccessLevel.NONE)
public class SimpleProfile {
    
    private double[] _values;
    private int[] _depths;
    private long[] _times;
    
    private long _time;
    private GeoLocation _location;

}
