/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.shared.NotFoundException;

/**
 * @author kamb
 *
 */
/*
  db_id serial NOT NULL,
  create_time timestamp with time zone NOT NULL DEFAULT now(),
  modified_time timestamp with time zone NOT NULL DEFAULT now(),
  username character varying(32) NOT NULL,
  last_login timestamp with time zone,
  first_name character varying(32) NOT NULL,
  last_name character varying(64) NOT NULL,
  email character varying(256) NOT NULL,
 */
public class UserDaoRawImpl extends DaoBase {

    private String _userDbUrl;
    
    private UserDaoRawImpl(String dbUrl) {
        _userDbUrl = dbUrl;
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(_userDbUrl);
    }
    
    public User getUserByUsername(String username) throws SQLException, NotFoundException {
        User user = null;
        String sql = "select * from users where username = ?";
        try ( Connection c = getConnection();
              PreparedStatement pstmt = c.prepareStatement(sql); ) {
            pstmt.setString(1, username);
            try ( ResultSet rs = pstmt.executeQuery(); ) {
                if ( ! rs.next()) {
                    throw new NotFoundException("User " + username + " not found.");
                }
                user = buildUserFromRs(rs);
                if ( rs.next()) {
                    throw new IllegalStateException("Nore than one user found for username: " + username);
                }
            }
        }
        return user;
    }

    private static User buildUserFromRs(ResultSet rs) throws SQLException {
        User user = User.builder()
                        .dbId(rs.getLong("db_id"))
                        .createTime(rs.getTimestamp("create_time"))
                        .modifiedTime(rs.getTimestamp("modified_time"))
                        .username(rs.getString("username"))
                        .lastLogin(rs.getTimestamp("last_login"))
                        .firstName(rs.getString("first_name"))
                        .lastName(rs.getString("last_name"))
                        .email(rs.getString("email"))
                        .build();
        return user;
    }
}
