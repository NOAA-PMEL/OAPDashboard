/**
 * 
 */
package gov.noaa.pmel.dashboard.server.admin;

import gov.noaa.pmel.dashboard.server.model.User;

/**
 * 
 */
public class TestSendNewUserEmail {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            User newUser = User.builder().email("linus.kamb@noaa.gov").username("lkamb").build();
            AdminClient.sendNewUserEmail(newUser, "abc123uAndMe!");
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }

}
