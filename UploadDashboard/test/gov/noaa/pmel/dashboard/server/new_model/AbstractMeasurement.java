/**
 * 
 */
package gov.noaa.pmel.dashboard.server.new_model;

import java.util.Date;

import gov.noaa.pmel.tws.types.GeoLocation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
@Getter(AccessLevel.PROTECTED)
@AllArgsConstructor
public class AbstractMeasurement implements Measurement {

    private String _name;
    private Number _value;
    private String _units;
//    private String _type;
    private Date _when;
    private GeoLocation _where;

}
