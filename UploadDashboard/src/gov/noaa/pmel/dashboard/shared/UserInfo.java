/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

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
public class UserInfo implements Serializable, IsSerializable {
    
    /* generated */
    private static final long serialVersionUID = -7147883315638859714L;
    
    protected String _username;
    protected String _firstName;
    protected String _middle;
    protected String _lastName;
    
    protected String _email;
    
    protected String _telephone;
    protected String _telExtension;
    
    protected String _organization;
    
    protected UserInfo() {
        //  GWT
    }
    
    public UserInfo(String username, String firstName, String middle, String lastName, 
                    String email, String telephone, String telExtension, String organization) {
        _username = username;
        _firstName = firstName;
        _middle = middle;
        _lastName = lastName;
        _email = email;
        _telephone = telephone;
        _telExtension = telExtension;
        _organization = organization;
    }
    
    // for access on the client.
    public String username() { return _username; }

    public String firstName() { return _firstName; }

    public String middle() { return _middle; }

    public String lastName() { return _lastName; }

    public String email() { return _email; }

    public String telephone() { return _telephone; }

    public String telExtension() { return _telExtension; }

    public String organization() { return _organization; }

}
