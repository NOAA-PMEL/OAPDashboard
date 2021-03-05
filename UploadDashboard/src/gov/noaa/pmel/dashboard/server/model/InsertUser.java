/**
 * 
 */
package gov.noaa.pmel.dashboard.server.model;


import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
@ToString(exclude="_authString")
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder=true,builderMethodName="newUser")
public class InsertUser extends User {

    /* generated */
    private static final long serialVersionUID = 4690911395566060351L;
    
    private String _authString;
    
}
