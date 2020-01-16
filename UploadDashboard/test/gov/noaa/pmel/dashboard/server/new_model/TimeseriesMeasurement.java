/**
 * 
 */
package gov.noaa.pmel.dashboard.server.new_model;

import java.util.Date;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
@EqualsAndHashCode(callSuper=true)
public class TimeseriesMeasurement extends AbstractMeasurement {

    private Date _when;

    @Builder
    private TimeseriesMeasurement(String name, String type, Number value, Date time) {
        super(name, type, value);
        _when = time;
    }
    
}
