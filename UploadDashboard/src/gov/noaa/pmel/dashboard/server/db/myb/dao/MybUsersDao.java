package gov.noaa.pmel.dashboard.server.db.myb.dao;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import gov.noaa.pmel.dashboard.server.Users;
import gov.noaa.pmel.dashboard.server.db.dao.UsersDao;
import gov.noaa.pmel.dashboard.server.db.myb.MybatisConnectionFactory;
import gov.noaa.pmel.dashboard.server.db.myb.mappers.UserMapper;
import gov.noaa.pmel.dashboard.server.model.InsertUser;
import gov.noaa.pmel.dashboard.server.model.User;


public class MybUsersDao implements UsersDao {

    @Override
    public void addAccessRole(String username) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.addAccessRole(username, "oapdashboarduser");
			session.commit();
		}
    }
    
	@Override
	public int addUser(InsertUser user) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.insertNewUser(user);
            umapper.addAuthUser(user);
			umapper.addAccessRole(user.username(), "oapdashboarduser");
			session.commit();
			return user.dbId().intValue();
		}
	}
    
    @Override
    public void setUserPassword(int userId, String newAuthString) throws SQLException {
        _setUserPassword(userId, newAuthString, null);
    }
    @Override
    public void resetUserPassword(int userId, String newAuthString) throws SQLException {
        _setUserPassword(userId, newAuthString, Users.getRequirePwChangeFlag());
    }
    private static void _setUserPassword(int userId, String newAuthString, String pwChangeRequiredFlag) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
            umapper.updateUserAuth(userId, newAuthString);
            umapper.updateUserChangedPassword(userId, pwChangeRequiredFlag);
            session.commit();
        }
    }
    
    @Override
    public String retrieveUserAuthString(int userId) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
            String auth = umapper.retrieveHashByUserId(userId);
            return auth;
        }
    }
        
	@Override
	public List<User> retrieveAll() throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			List<User> users = umapper.retrieveAll();
			return users;
		}
	}
	
	@Override
	public User retrieveUser(String username) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			User user = umapper.retrieveByUsername(username);
			return user;
		}
	}
	
	@Override
	public User retrieveUserById(Integer userDbId) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			User user = umapper.retrieveByUserId(userDbId);
			return user;
		}
	}

	@Override
	public User retrieveUserByEmail(String email) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			User user = umapper.retrieveByEmail(email);
			return user;
		}
	}

    @Override
    public void updateUser(User changedUser) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.updateUser(changedUser);
			session.commit();
		}
    }
    
    @Override
    public void userLogin(User user) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.updateLastLogin(user.dbId().intValue());
			session.commit();
		}
    }
    
	@Override
	public void deleteUserByUsername(String username) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
            umapper.removeAccessRole(username, "oapdashboarduser");
            umapper.removeAuthUser(username);
			umapper.deleteUserByUsername(username);
			session.commit();
		}
	}
    
    @Override
    public void removeAccessRole(String username) {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
            umapper.removeAccessRole(username, "oapdashboarduser");
            session.commit();
        }
    }

/*	
	@Override
	public List<User> retrieveAll() {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			List<User> users = umapper.retrieveAll();
			return users;
		}
	}

	@Override
	public String getUserPasswordHashByUsername(String username) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			String userHash = umapper.retrieveHashByUsername(username);
			return userHash;
		}
	}
		
	@Override
	public String getUserPasswordHashByUserId(Integer userid) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			String userHash = umapper.retrieveHashByUserId(userid);
			return userHash;
		}
	}
	@Override
	public void userLogin(User user) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.updateLastLogin(user.dbId());
			session.commit();
		}
	}

	@Override
	public boolean updateUser(User updatedUserInfo) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			int result = umapper.updateUser(updatedUserInfo);
			if ( result == 1 ) {
				session.commit();
			} else {
				logger.warn("User update for " + updatedUserInfo.username() + " returned " + result);
			}
			return result == 1;
		}
	}

	@Override
	public boolean updateUserAuth(int dbId, String authString) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			int result = umapper.updateUserAuth(dbId, authString);
			if ( result == 1 ) {
				session.commit();
			} else {
				logger.warn("Password change for " + dbId + " returned " + result);
			}
			return result == 1;
		}
	}

	@Override
	public boolean deleteUser(String userid) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			User user = umapper.retrieveByUsername(userid);
			if ( user != null ) {
				int result = umapper.deleteUser(user);
				if ( result == 1 ) {
					session.commit();
				} else {
					logger.warn("delete user " + userid + " returned " + result);
				}
				return result == 1;
			} else {
				logger.warn("Attempt to delete non-existing user: "+ userid);
				return false;
			}
		}
	}

*/

}
