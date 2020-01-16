/**
 * 
 */
package gov.noaa.pmel.dashboard.server.new_model;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Singular;

/**
 * @author kamb
 *
 */
@Getter
@AllArgsConstructor
public abstract class Observation {
    
    @Singular 
    private Collection<AbstractMeasurement> _measurements;
    
    private ObservationDimension _majorDimension;
}
