/**
 * 
 */
package gov.noaa.pmel.dashboard.shared;

import java.io.IOException;
import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author kamb
 *
 */
public class SessionException extends IOException implements Serializable, IsSerializable {

    private static final long serialVersionUID = 1L;

    public SessionException() { super(); }

    public SessionException(String message) { super(message); }

    public SessionException(Throwable rootCause) { super(rootCause); }

    public SessionException(String message, Throwable rootCause) { super(message, rootCause); }

}
