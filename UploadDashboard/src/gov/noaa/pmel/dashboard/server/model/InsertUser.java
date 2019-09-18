/**
 * 
 */
package gov.noaa.pmel.dashboard.server.model;

import java.util.Date;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
@ToString(exclude="_authString")
@EqualsAndHashCode(callSuper=true)
public class InsertUser extends User {

    String _authString;

    @Builder(builderMethodName="newUser")
    public InsertUser(Long dbId, Date createTime, Date modifiedTime, String username, Date lastLogin,
                      String firstName, String lastName, String email, String authString) {
        super(dbId, createTime, modifiedTime, username, lastLogin, firstName, lastName, email);
        _authString = authString;
    }
    
}
