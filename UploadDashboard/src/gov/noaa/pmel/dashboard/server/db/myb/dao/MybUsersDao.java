package gov.noaa.pmel.dashboard.server.db.myb.dao;

import java.sql.SQLException;

import org.apache.ibatis.session.SqlSession;

import gov.noaa.pmel.dashboard.server.db.dao.UsersDao;
import gov.noaa.pmel.dashboard.server.db.myb.MybatisConnectionFactory;
import gov.noaa.pmel.dashboard.server.db.myb.mappers.UserMapper;
import gov.noaa.pmel.dashboard.server.model.InsertUser;
import gov.noaa.pmel.dashboard.server.model.User;


public class MybUsersDao implements UsersDao {

//	private static Logger logger = Logging.getLogger(MybUsersDao.class);
	
	@Override
	public int insertUser(InsertUser user) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.insertNewUser(user);
            umapper.addAuthUser(user);
			session.commit();
			return user.dbId().intValue();
		}
	}
    
    @Override
    public void resetUserPassword(int userId, String newAuthString) throws SQLException {
        try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
            UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
            umapper.updateUserAuth(userId, newAuthString);
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
    public void addAccessRole(String username) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.addAccessRole(username, "oapdashboarduser");
			session.commit();
		}
        
    }
    @Override
    public void insertReviewer(String username, String realName, String email) {
		try (SqlSession session = MybatisConnectionFactory.getFlagsDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.insertReviewer(username, realName, email);
			session.commit();
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
    public void updateUser(User changedUser) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.updateUser(changedUser);
			session.commit();
		}
    }
    
	@Override
	public void deleteUserByUsername(String username) throws SQLException {
		try (SqlSession session = MybatisConnectionFactory.getDashboardDbSessionFactory().openSession();) {
			UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
			umapper.deleteUserByUsername(username);
			session.commit();
		}
	}
    
    @Override
    public void removeReviewer(String username) {
        try (SqlSession session = MybatisConnectionFactory.getFlagsDbSessionFactory().openSession();) {
            UserMapper umapper = (UserMapper) session.getMapper(UserMapper.class);
            umapper.deleteReviewer(username);
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
