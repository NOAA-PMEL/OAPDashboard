
package gov.noaa.pmel.dashboard.server;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

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
        User,
        Manager,
        Admin
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
            String cryptPasswd = PasswordCrypt.generateTomcatPasswd(plainTextPasswd);
            if ( ! PasswordCrypt.tomcatPasswdMatches(cryptPasswd, plainTextPasswd)) {
                throw new GeneralSecurityException("Match Fail!");
            }
            return _addUser(user, cryptPasswd, role);
        } catch (GeneralSecurityException ex) {
            System.err.println(ex);
            throw new DashboardException(ex);
        }
    }
    private static int _addUser(User user, String cryptPasswd, UserRole role) throws DashboardException {
        UsersDao udao = DaoFactory.UsersDao();
//        boolean dConfig = false;
        boolean reviewer = false;
        try {
//            addUserToDashboardConfig(user, role);
//            dConfig = true;
            udao.insertReviewer(user.username(), user.firstName()+" "+user.lastName(), user.email());
            reviewer = true;
            InsertUser inUser = user.asInsertUser().authString(cryptPasswd).build();
            int dbId = udao.insertUser(inUser);
            udao.addAccessRole(inUser.username());
            return dbId;
        } catch (Exception ex) {
            System.err.println(ex);
//            if ( dConfig ) {
//                try { removeDashboardConfigUser(user); }
//                catch ( Throwable t ) { System.err.println(t); }
//            }
            if ( reviewer ) {
                try { udao.removeReviewer(user.username()); }
                catch ( Throwable t ) { System.err.println(t); }
            }
            throw new DashboardException(ex);
        }
    }
    
    public static User validateUser(String username, String plainTextPasswd) throws DashboardException, LoginException {
        User user = null;
        try {
            UsersDao udao = DaoFactory.UsersDao();
            user = udao.retrieveUser(username);
            if ( user != null ) {
                String crypt = udao.retrieveUserAuthString(user.dbId().intValue());
                if ( ! ( crypt.equals(plainTextPasswd) || PasswordCrypt.tomcatPasswdMatches(crypt, plainTextPasswd))) {
                    throw new CredentialException("User password does not match.");
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
    public static void changeUserPassword(String username, String currentPlainTextPasswd, String newPlainTextPasswd) throws DashboardException {
        try {
            User user = validateUser(username, currentPlainTextPasswd);
            setUserPassword(user, newPlainTextPasswd);
        } catch (Exception ex) {
            System.err.println(ex);
            throw new DashboardException(ex);
        }
    }
    
    public static void setUserPassword(User user, String newPlainTextPasswd) throws DashboardException {
        try {
            PasswordUtils.validatePasswordStrength(newPlainTextPasswd);
            String newCryptPasswd = PasswordCrypt.generateTomcatPasswd(newPlainTextPasswd);
            UsersDao udao = DaoFactory.UsersDao();
            udao.setUserPassword(user.dbId().intValue(), newCryptPasswd);
        } catch (Exception ex) {
            System.err.println(ex);
            throw new DashboardException(ex);
        }
    }
        
//    private static void addUserToDashboardConfig(User user, UserRole role) throws IOException {
//        DashboardConfigStore cfg = DashboardConfigStore.get(false);
//        cfg.addUser(user.username(), role.name());
//    }
//    
//    private static void removeDashboardConfigUser(User user) throws IOException {
//        DashboardConfigStore cfg = DashboardConfigStore.get(false);
//        cfg.removeUser(user.username());
//    }
    
    public static User getUser(String userid) throws DashboardException {
        UsersDao udao = DaoFactory.UsersDao();
        try {
            User user = udao.retrieveUser(userid);
            return user;
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
    
    public static void deleteUser(String userid) throws DashboardException {
        UsersDao udao = DaoFactory.UsersDao();
        try {
            udao.deleteUserByUsername(userid);
            udao.removeAccessRole(userid);
            udao.removeReviewer(userid);
        } catch (SQLException ex) {
            throw new DashboardException(ex);
        }
    }
    
    /**
     * @throws Exception 
     * 
     */
    public static void resetPassword(String username_or_email) throws Exception {
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
            setUserPassword(user, newPassword);
            String notificationMsg = getPasswordResetMessage(user, newPassword);
            Notifications.SendEmail("OAP Dashboard password reset", notificationMsg, email, Notifications.OADB_RETURN_ADDR);
        } catch (Exception ex) {
            logger.info(ex);
            throw ex;
        }
    }
    /**
     * @param user
     * @param newPassword
     * @return
     */
    private static String getPasswordResetMessage(User user, String newPassword) {
        return "Password has been reset for user " + user.firstName() + " " + user.lastName() + ".\n" +
                "New password is " + newPassword;
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
}
