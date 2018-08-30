/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

/**
 * @author kamb
 *
 */
public class DashboardException extends Exception {

    private static final long serialVersionUID = 1455951929215117749L;

    public DashboardException() {
        super();
    }

    public DashboardException(String message) {
        super(message);
    }

    public DashboardException(Throwable cause) {
        super(cause);
    }

    public DashboardException(String message, Throwable cause) {
        super(message, cause);
    }

}
