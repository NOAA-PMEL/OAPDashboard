/**
 * 
 */
package gov.noaa.pmel.dashboard.server.model;

import java.util.Date;
import java.util.List;

import gov.noaa.pmel.dashboard.shared.UserInfo;
import gov.noaa.pmel.oads.util.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * @author kamb
 *
 */
@Data
@EqualsAndHashCode(callSuper=true)
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@SuperBuilder(toBuilder=true)
public class User extends UserInfo {

    private transient Long _dbId;
    private transient Date _createTime;
    private transient Date _modifiedTime;
    
//    private String _username;
    
    private Date _lastLogin;
    private Date _lastPwChange;
    private String _requiresPwChange;
    
//    private String _firstName;
//    private String _middle;
//    private String _lastName;
    
//    private String _email;
    
//    private String _telephone;
//    private String _telExtension;
    
//    private String _organization;
    
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
                .requiresPwChange(_requiresPwChange)
                .firstName(_firstName)
                .lastName(_lastName)
                .middle(_middle)
                .email(_email)
                .telephone(_telephone)
                .telExtension(_telExtension)
                .organization(_organization);
    }
    
    public UserInfo asUserInfo() {
        return new UserInfo(_username, _firstName, _middle, _lastName, 
                            _email, _telephone, _telExtension, _organization);
    }
    
    public boolean hasRole(String roleName) {
        return _roles != null && _roles.contains(roleName);
    }
    
    /**
     * Returns A String representation of the telephone number, plus extension if exists.
     * If there is no phone number, an empty String is returned.
     * 
     * @return A String representation of the telephone number, plus extension if exists.
     */
    public String telephoneString() {
        StringBuilder phone = new StringBuilder();
        if ( StringUtils.emptyOrNull(_telephone)) {
            return "";
        }
        phone.append(_telephone);
        if ( StringUtils.emptyOrNull(_telExtension)) {
            return phone.toString();
        }
        phone.append(",ext:").append(_telExtension);
        return phone.toString();
    }

    /**
     * @return
     */
    public boolean requiresPasswordChange() {
        return _requiresPwChange != null;
    }
}
