/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.myb.mappers;

import java.sql.SQLException;
import java.util.List;

import gov.noaa.pmel.dashboard.server.model.InsertUser;
import gov.noaa.pmel.dashboard.server.model.User;

/**
 * @author kamb
 *
 */
public interface UserMapper {

	public void insertNewUser(InsertUser user) throws SQLException;
    public void addAuthUser(InsertUser user);
    public void removeAuthUser(String username);
    
	public void addAccessRole(String username, String role)  throws SQLException;
    public void removeAccessRole(String username, String string);
    
	public void insertReviewer(String username, String realName, String email);
	public void deleteReviewer(String username);
    
	public void insertUserOnly(User user);
	
	public void insertUserAuth(int _dbId, String _authString);
	
	public int updateUser(User user);
	public void updateUserAuth(int userId, String authString);
	public void updateUserChangedPassword(int userId, String changeRequiredFlag);
	
	public void updateLastLogin(int userId);
	
	public List<User> retrieveAll();
	
	public User retrieveByUserId(Integer userid) throws SQLException;
	public User retrieveByUsername(String username) throws SQLException;
	public User retrieveByEmail(String email) throws SQLException;
	
	public String retrieveHashByUsername(String username);
	public String retrieveHashByUserId(Integer userid);
	
	public int deleteUser(User user);
	
	public void deleteUserByDbId(int userDbId);
	
	public void deleteUserByUsername(String username);
	
	public void trickyDelete(String username);
    
}
