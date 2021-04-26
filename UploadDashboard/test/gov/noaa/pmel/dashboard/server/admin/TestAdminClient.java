/**
 * 
 */
package gov.noaa.pmel.dashboard.server.admin;

/**
 * @author kamb
 *
 */
public class TestAdminClient extends AdminClient {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        try {
            String[] debugArgs = 
                new String[] { "add", "-u", "whooya", "-fn", "Whoya", "-mn", "G", "-ln", "Call",
                               "-e", "who@ya.ca", "-t", "206.777.8484",
                               "-o", "NOAA Pacific Marine Environment Laboratory", "-d", "dunkel" }; // , "-x" };
            AdminClient.main(debugArgs);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }

}
