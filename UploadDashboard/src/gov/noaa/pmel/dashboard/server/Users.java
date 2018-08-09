
package gov.noaa.pmel.dashboard.server;

import java.security.GeneralSecurityException;
import java.sql.SQLException;

import gov.noaa.pmel.dashboard.server.db.dao.DaoFactory;
import gov.noaa.pmel.dashboard.server.db.dao.UsersDao;
import gov.noaa.pmel.dashboard.server.model.InsertUser;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.util.PasswordCrypt;
import gov.noaa.pmel.tws.util.JWhich;

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
     * @param args
     */
    public static void main(String[] args) {
        try {
            JWhich.which("org.apache.tomcat.util.res.StringManager");
            JWhich.which("org.apache.catalina.realm.DigestCredentialHandlerBase");
            String username = "lkamb";
            Users.deleteUser(username);
//            Users.deleteUser("j-"+username);
            User newUser = User.builder().username(username).firstName("Linus").lastName("Kamb").email("linus@noaa.gov").build();
            @SuppressWarnings("unused")
            int dbId = Users.addUser(newUser, "drowssap", UserRole.Manager);
//            if ( dbId > 0 ) { System.exit(dbId); }
//            int jId = Users.addJettyUser(newUser.toBuilder().username("j"+newUser.username()).build(), "foodap", UserRole.Manager);
            User user = Users.getUser(username);
            System.out.println(user);
            user = Users.getUser("j"+username);
            System.out.println(user);
            User changedUser = user.toBuilder().firstName("Changed").build();
            Users.updateUser(changedUser);
            user = Users.getUser(username);
            System.out.println(user);
//            Users.deleteUser(username);
//            user = Users.getUser(username);
//            System.out.println(user);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }

    public static User validateUser(String userid, String passwd) {
        // TODO Auto-generated method stub
        return null;
    }

}
