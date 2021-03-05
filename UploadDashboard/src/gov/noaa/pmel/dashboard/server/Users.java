
package gov.noaa.pmel.dashboard.server;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.UsersDao;
import gov.noaa.pmel.dashboard.server.model.InsertUser;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.server.util.Notifications;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.util.PasswordCrypt;
import gov.noaa.pmel.dashboard.util.PasswordUtils;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * @author kamb
 *
 */
public class Users {

    public enum UserRole {
        Groupie("oapdashboarduser"),
        Manager("groupmanager"),
        Admin("dashboardadmin");
        
        private String _key;
        private UserRole(String key) {
            _key = key;
        }
        public String roleKey() { return _key; }
    }
    
    private static Logger logger = LogManager.getLogger(Users.class);
    
    public static int addJettyUser(User user, String plainTextPasswd, UserRole role) throws DashboardException {
        try {
            String cryptPasswd = PasswordCrypt.generateJettyPasswd(user.username(), plainTextPasswd);
            return _addUser(user, cryptPasswd, role);
        } catch (GeneralSecurityException ex) {
            System.err.println(ex);
            throw new DashboardException(ex);
        }
    }
    public static int addUser(User user, String plainTextPasswd, UserRole role) throws DashboardException {
        try {
            validateUser(user);
            String cryptPasswd = PasswordCrypt.generateTomcatPasswd(plainTextPasswd);
            // This is just a sanity check.  It shouldn't really be necessary.
            if ( ! PasswordCrypt.tomcatPasswdMatches(cryptPasswd, plainTextPasswd)) {
                throw new GeneralSecurityException("Match Fail!");
            }
            return _addUser(user, cryptPasswd, role);
        } catch (GeneralSecurityException ex) {
            System.err.println(ex);
            throw new DashboardException(ex);
        }
    }
    /**
     * @param user
     */
    private static void validateUser(User user) {
        if ( StringUtils.emptyOrNull(user.username())) {
            throw new IllegalStateException("Null username for user");
        }
    }
    private static int _addUser(User user, String cryptPasswd, UserRole role) throws DashboardException {
        UsersDao udao = DaoFactory.UsersDao();
        try {
            InsertUser inUser = (InsertUser) user.asInsertUser()
                                    .authString(cryptPasswd)
                                    .createTime(new Date())
                                    .build();
            int dbId = udao.addUser(inUser);
            return dbId;
        } catch (Exception ex) {
            logger.warn(ex,ex);
            throw new DashboardException(ex);
        }
    }
    
    public static User validateUser(String username, String plainTextPasswd) 
            throws DashboardException, LoginException {
        User user = null;
        try {
            UsersDao udao = DaoFactory.UsersDao();
            user = udao.retrieveUser(username);
            if ( user != null ) {
                String crypt = udao.retrieveUserAuthString(user.dbId().intValue());
                if ( ! ( crypt.equals(plainTextPasswd) || PasswordCrypt.tomcatPasswdMatches(crypt, plainTextPasswd))) {
                    throw new LoginException("User password does not match.");
                }
            } else {
                throw new AccountNotFoundException("No user found for username \""+username+"\"");
            }
        } catch (SQLException | NoSuchAlgorithmException ex) {
            throw new DashboardException(ex);
        }
        return user;
    }

    /**
     * @param username
     * @param password
     */
    public static void changeUserPassword(String username, String currentPlainTextPasswd, 
                                          String newPlainTextPasswd) 
            throws DashboardException {
        try {
            User user = validateUser(username, currentPlainTextPasswd);
            setUserPassword(user, newPlainTextPasswd);
        } catch (Exception ex) {
            System.err.println(ex);
            throw new DashboardException(ex);
        }
    }
    
    public static void setUserPassword(User user, String newPlainTextPasswd) 
            throws CredentialException, DashboardException {
        try {
            PasswordUtils.validatePasswordStrength(newPlainTextPasswd);
            _setUserPassword(user, newPlainTextPasswd);
        } catch (SQLException | GeneralSecurityException ex) {
            System.err.println(ex);
            throw new DashboardException(ex);
        }
    }
    
    public static void _setUserPassword(User user, String newPlainTextPasswd) 
            throws GeneralSecurityException, SQLException {
        String newCryptPasswd = PasswordCrypt.generateTomcatPasswd(newPlainTextPasswd);
        UsersDao udao = DaoFactory.UsersDao();
        udao.setUserPassword(user.dbId().intValue(), newCryptPasswd);
    }
        
    public static void _resetUserPassword(User user, String newPlainTextPasswd) 
            throws GeneralSecurityException, SQLException {
        String newCryptPasswd = PasswordCrypt.generateTomcatPasswd(newPlainTextPasswd);
        UsersDao udao = DaoFactory.UsersDao();
        udao.resetUserPassword(user.dbId().intValue(), newCryptPasswd);
    }
        
    public static User getUser(String userid) throws DashboardException {
        UsersDao udao = DaoFactory.UsersDao();
        try {
            User user = udao.retrieveUser(userid);
            return user;
        } catch (SQLException ex) {
            throw new DashboardException(ex);
        }
    }
    public static List<User> getAllUsers() throws DashboardException {
        UsersDao udao = DaoFactory.UsersDao();
        try {
            List<User> users = udao.retrieveAll();
            return users;
        } catch (SQLException ex) {
            throw new DashboardException(ex);
        }
    }
    
    public static void updateUser(User changedUser) throws DashboardException {
        UsersDao udao = DaoFactory.UsersDao();
        try {
            udao.updateUser(changedUser);
        } catch (SQLException ex) {
            throw new DashboardException(ex);
        }
    }
    
    public static void userLogin(User user) throws DashboardException {
        UsersDao udao = DaoFactory.UsersDao();
        try {
            udao.userLogin(user);
        } catch (SQLException ex) {
            throw new DashboardException(ex);
        }
    }
    
    public static void deleteUser(String userid) throws DashboardException {
        UsersDao udao = DaoFactory.UsersDao();
        try {
            udao.deleteUserByUsername(userid);
        } catch (SQLException ex) {
            throw new DashboardException(ex);
        }
    }
    
    /**
     * @throws Exception 
     * 
     */
    public static void resetPassword(String username_or_email) throws DashboardException {
        try {
            if ( StringUtils.emptyOrNull(username_or_email)) {
                throw new IllegalArgumentException("No username or email address provided.");
            }
            User user = username_or_email.indexOf('@') > 0 ?
                            findUserByEmail(username_or_email) :
                            findUserByUsername(username_or_email);
            if ( user == null ) {
                throw new IllegalStateException("No user found for " + username_or_email);
            }
            String newPassword = PasswordUtils.generateSecurePassword();
            String email = user.email();
            _resetUserPassword(user, newPassword);
            String notificationMsg = getPasswordResetMessage(user, newPassword);
            Notifications.SendEmail("OAP Dashboard password reset", notificationMsg, email, Notifications.OADB_RETURN_ADDR);
        } catch (Exception ex) {
            logger.info(ex);
            throw new DashboardException(ex.getMessage(), ex);
        }
    }
    public static void sendUsername(String username_or_email) throws DashboardException {
        try {
            if ( StringUtils.emptyOrNull(username_or_email)) {
                throw new IllegalArgumentException("No username or email address provided.");
            }
            User user = username_or_email.indexOf('@') > 0 ?
                            findUserByEmail(username_or_email) :
                            findUserByUsername(username_or_email);
            if ( user == null ) {
                throw new IllegalStateException("No user found for " + username_or_email);
            }
            String email = user.email();
            String notificationMsg = getUsernameMessage(user);
            Notifications.SendEmail("OAP Dashboard user", notificationMsg, email, Notifications.OADB_RETURN_ADDR);
        } catch (Exception ex) {
            logger.info(ex);
            throw new DashboardException(ex.getMessage(), ex);
        }
    }
    /**
     * @param user
     * @param newPassword
     * @return
     */
    private static String getPasswordResetMessage(User user, String newPassword) {
        return "Your password has been reset.\n" +
                "The temporary password is " + newPassword + "\n\n" +
				"You will be required to change your password when you login. \n\n" + 
                "To log in, go to the SDIS login page at " + 
				    ApplicationConfiguration.getProperty("oap.production.url", 
                                                          "https://www.pmel.noaa.gov/sdig/oap/Dashboard") +
                    "\n\n" +
				"If you did not request your password to be reset, please contact the system administrator at " +
				"oar.pmel.sdis.admin@noaa.gov immediately.";
    }
    private static String getUsernameMessage(User user) {
        return "The username for this account is: " + user.username() + ".\n" +
				"If you did not request your username, please contact the system administrator at " +
				"oar.pmel.sdis.admin@noaa.gov immediately.";
    }
    /**
     * @param username
     * @return
     */
    private static User findUserByUsername(String username) {
        User user = null;
        try {
            UsersDao udao = DaoFactory.UsersDao();
            user = udao.retrieveUser(username);
        } catch (SQLException sqx) {
            logger.warn("SQLException querying for user " + username, sqx);
            throw new RuntimeException("Server Error");
        }
        return user;
    }
    /**
     * @param username_or_email
     * @return
     */
    private static User findUserByEmail(String email) {
        User user = null;
        try {
            UsersDao udao = DaoFactory.UsersDao();
            user = udao.retrieveUserByEmail(email);
        } catch (SQLException sqx) {
            logger.warn("SQLException querying for user by email " + email, sqx);
            throw new RuntimeException("Server Error");
        }
        return user;
    }
    
    /**
     * @param parameterMap
     */
    public static void requestAccount(Map<String, String[]> parameterMap) {
        String message = buildNewAcctReqMsg(parameterMap);
        logger.info("New account request:"+ message);
        String toList = ApplicationConfiguration.getProperty("oap.admin.email.list", "linus.kamb@noaa.gov");
        Notifications.SendEmail("New SDIS Account Request", message, toList);
    }
    /**
     * @param parameterMap
     * @return
     */
    private static String buildNewAcctReqMsg(Map<String, String[]> parameterMap) {
        StringBuilder sb = new StringBuilder("New Account Request:\n");
        for (Entry<String, String[]> entry : parameterMap.entrySet()) {
            String comma = "";
            sb.append(entry.getKey()).append("\t: ");
            for (String v : entry.getValue()) {
                sb.append(comma).append(v);
                comma = ", " ;
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
//    // Map of username to user info
//	private static Map<String,DashboardUserInfo> _userInfoMap;
//    private synchronized static Map<String, DashboardUserInfo> getUserMap() {
//		// Read and assign the authorized users 
////        if ( _userInfoMap == null ) {  // XXX for now, always fetch, otherwise new users aren't found until restart
//    		_userInfoMap = new HashMap<String,DashboardUserInfo>();
//            try {
//                List<User> users = getAllUsers();
//                for (User u : users) {
//                    DashboardUserInfo ui = new DashboardUserInfo(u.username());
//                    String userRoles = "";
//                    for (String role: u.roles()) {
//                        userRoles += " " + role;
//                    }
//                    ui.addUserRoles(userRoles.trim());
//                    _userInfoMap.put(u.username(), ui);
//                }
//            } catch (DashboardException dex) {
//                logger.warn(dex,dex);
//            }
//    		for ( DashboardUserInfo info : _userInfoMap.values() ) {
//    			logger.info("    user info: " + info.toString());
//    		}
////        }
//        return _userInfoMap;
//    }
	/**
	 * Validate a username from the user info map
	 *  
	 * @param username
	 * 		username
	 * @return
	 * 		true if successful
	 */
	public static boolean validateUser(String username) {
		if ( (username == null) || username.isEmpty() )
			return false;
		String name = DashboardUtils.cleanUsername(username);
		DashboardUserInfo userInfo = getUserInfo(name);
		if ( userInfo == null )
			return false;
		return true;
	}


	/**
     * @param name
     * @return
	 * @throws DashboardException 
     */
    private static DashboardUserInfo getUserInfo(String name) {
        User u = null;
        try {
            u = getUser(name);
        } catch (DashboardException ex) {
            ex.printStackTrace();
            return null;
        }
        if ( u == null ) { return null; }
        DashboardUserInfo ui = new DashboardUserInfo(u.username());
        String userRoles = "";
        for (String role: u.roles()) {
            userRoles += " " + role;
        }
        ui.addUserRoles(userRoles.trim());
        return ui;
    }
    /**
	 * Determines if username has manager privilege over othername. 
	 * This can be from username being an administrator, a manager
	 * of a group othername belongs to, having the same username,
	 * or othername being invalid (most likely an unspecified user),
	 * so long as username is an authorized user.
	 * 
	 * @param username
	 * 		manager username to check; if not a valid user, returns false
	 * @param othername
	 * 		group member username to check; if not a valid user, 
	 * 		returns true if username is a valid user
	 * @return
	 * 		true if username is an authorized user and has manager
	 * 		privileges over othername
	 */
	public static boolean userManagesOver(String username, String othername) {
        if ( username.equals(othername) || isManager(username)) { return true; } // XXX TODO: This isn't really right here.
        else { return false; }
//		DashboardUserInfo userInfo = getUserMap().get(DashboardUtils.cleanUsername(username));
//		if ( userInfo == null ) { // XXX TODO: Should be an Exception!
//		    logger.warn("No userInfo found for user : " + username);
//			return false;
//		}
//		return userInfo.managesOver(_userInfoMap.get(DashboardUtils.cleanUsername(othername)));
	}

	/**
	 * @param username
	 * 		name of the user
	 * @return
	 * 		true is this user is an admin or a manager of a group
	 * 		(regardless of whether there is anyone else in the group)
	 */
	public static boolean isManager(String username) {
		DashboardUserInfo userInfo = getUserInfo(DashboardUtils.cleanUsername(username));
		if ( userInfo == null )
			return false;
		return userInfo.isManager();
	}

	/**
	 * @param username
	 * 		name of the user
	 * @return
	 * 		true is this user is an admin
	 * @throws DashboardException 
	 */
	public static boolean isAdmin(String username) throws DashboardException {
        User user = getUser(username);
        if ( user == null ) { return false; }
        return user.hasRole(UserRole.Admin.roleKey());
	}

    public static String getRequirePwChangeFlag() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
    
    /**
     * @param datasetId
     * @return
     * @throws IOException 
     * @throws IllegalArgumentException 
     * @throws DashboardException 
     */
    public static User getDataSubmitter(String datasetId) 
            throws IllegalArgumentException, IOException, DashboardException {
        DashboardDataset dataset = DashboardConfigStore.get().getDataFileHandler().getDatasetFromInfoFile(datasetId);
        String owner = dataset.getOwner();
        User user = Users.getUser(owner);
        return user;
    }

    public static void main(String[] args) {
        try {
            
            UsersDao udao = DaoFactory.UsersDao();
            User user = getUser("jqpone");
            System.out.println(user);
            System.out.println("User is admin: " + isAdmin("jqpone"));
//            deleteUser("jqpone");
//            User newUser = User.builder()
//                    .username("jqpone")
//                    .firstName("Johnny")
//                    .middle("Q")
//                    .lastName("Pone")
//                    .email("a@B.co")
//                    .build();
//            addUser(newUser, "foobar", UserRole.Groupie);
            List<User> users = udao.retrieveAll();
            System.out.println(users);
//            User u = udao.retrieveUser("linus");
//            System.out.println(u + " : " + u.hasRole(UserRole.Admin.roleKey()));
//            u = udao.retrieveUserByEmail("linus.kamb@noaa.gov");
//            System.out.println(u + " : " + u.hasRole(UserRole.Admin.roleKey()));
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }
}
