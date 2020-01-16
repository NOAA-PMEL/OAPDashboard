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
public class SimpleTrajectory {

    private double[] _values;
    private long[] _times;
    private GeoLocation[] _locations;
}
