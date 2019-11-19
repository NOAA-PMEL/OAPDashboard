/**
 * 
 */
package gov.noaa.pmel.dashboard.server.model;

import java.util.Date;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
@Builder(toBuilder=true)
public class User {

    private transient Long _dbId;
    private transient Date _createTime;
    private transient Date _modifiedTime;
    
    private String _username;
    private Date _lastLogin;
    
    private String _firstName;
    private String _lastName;
    
    private String _email;
    
    public String fullName() {
        return _firstName + " " + _lastName;
    }
    
    public InsertUser.InsertUserBuilder asInsertUser() {
        return InsertUser.newUser()
                .dbId(_dbId)
                .createTime(_createTime)
                .modifiedTime(_modifiedTime)
                .username(_username)
                .lastLogin(_lastLogin)
                .firstName(_firstName)
                .lastName(_lastName)
                .email(_email);
    }
}
