/**
 * 
 */
package gov.noaa.pmel.dashboard.server.model;

import java.util.Date;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder=true)
public class User {

    private transient Long _dbId;
    private transient Date _createTime;
    private transient Date _modifiedTime;
    
    private String _username;
    private Date _lastLogin;
    
    private String _firstName;
    private String _middle;
    private String _lastName;
    
    private String _email;
    
    private List<String> _roles;
    
    public String fullName() {
        return _firstName + " " + _lastName;
    }
    
    public User(Long dbId) {
        _dbId = dbId;
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
    
    public boolean hasRole(String roleName) {
        return _roles != null && _roles.contains(roleName);
    }
}
