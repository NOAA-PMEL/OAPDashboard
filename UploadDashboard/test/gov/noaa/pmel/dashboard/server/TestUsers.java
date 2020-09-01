/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import gov.noaa.pmel.dashboard.server.Users.UserRole;
import gov.noaa.pmel.dashboard.server.model.User;

/**
 * @author kamb
 *
 */
public class TestUsers {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            User user = User.builder()
                    .username("devmode")
                    .firstName("Devonius")
                    .middle("D")
                    .lastName("Modulus")
                    .email("ronnie@a.bc")
                    .organization("Origination")
                    .telephone("123.456-7890")
                    .build();
            Users.addUser(user, "pl@in@sDa7", UserRole.Groupie);
//            user = Users.getUser("testy");
//            user = user.toBuilder().organization("originally").build();
//            Users.updateUser(user);
//            user = Users.getUser("testy");
//            System.out.println(user);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }

}
