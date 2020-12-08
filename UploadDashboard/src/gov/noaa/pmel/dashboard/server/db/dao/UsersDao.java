/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.dao;

import java.sql.SQLException;
import java.util.List;

import gov.noaa.pmel.dashboard.server.model.InsertUser;
import gov.noaa.pmel.dashboard.server.model.User;

/**
 * @author kamb
 *
 */
public interface UsersDao {

    public void addAccessRole(String username) throws SQLException;

    // also adds access role in transaction
    public int addUser(InsertUser newUser) throws SQLException;
    
    public List<User> retrieveAll() throws SQLException;
    
    public User retrieveUser(String username) throws SQLException;

    public User retrieveUserById(Integer userDbId) throws SQLException;
    
    public User retrieveUserByEmail(String email) throws SQLException;
    
    public void updateUser(User changedUser) throws SQLException;
    
    public void setUserPassword(int userId, String newAuthString) throws SQLException;
    public String retrieveUserAuthString(int userId) throws SQLException;
    
    public void userLogin(User user) throws SQLException;
    
    public void deleteUserByUsername(String username) throws SQLException;

    public void removeAccessRole(String userid);

    /**
     * Sets user password, also sets need to change flag.
     * @param intValue
     * @param newCryptPasswd
     * @throws SQLException 
     */
    public void resetUserPassword(int userid, String newCryptPasswd) throws SQLException;

}
