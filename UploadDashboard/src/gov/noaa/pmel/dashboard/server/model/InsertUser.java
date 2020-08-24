/**
 * 
 */
package gov.noaa.pmel.dashboard.server.model;

import java.util.Date;
import java.util.List;

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
                      String firstName, String middle, String lastName, String email, 
                      String telephone, String telExt, String organization, String authString, List<String> _roles) {
        super(dbId, createTime, modifiedTime, username, lastLogin, firstName, middle, lastName, 
              email, telephone, telExt, organization, _roles);
        _authString = authString;
    }
    
}
