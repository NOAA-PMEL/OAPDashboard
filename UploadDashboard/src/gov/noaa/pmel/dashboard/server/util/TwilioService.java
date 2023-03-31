/**
 * 
 */
package gov.noaa.pmel.dashboard.server.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author kamb
 *
 */
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.ApplicationConfiguration.PropertyNotFoundException;

public class TwilioService {
    
    private static Logger logger = LogManager.getLogger(TwilioService.class);
    
    // Find your Account SID and Auth Token at twilio.com/console
    // and set the environment variables. See http://twil.io/secure
    /** The Constant ACCOUNT_SID. Find it at twilio.com/user/account */
    private String ACCOUNT_SID;

    /** The Constant AUTH_TOKEN. Find it at twilio.com/user/account */
    private String AUTH_TOKEN;

    private String fromNumber = "+14342608932";
            
    public static Message SendSMS(String phoneNumber, String message) throws PropertyNotFoundException {
        return new TwilioService().sendSMS(phoneNumber, message);
    }
    
    public TwilioService() throws PropertyNotFoundException {
        ACCOUNT_SID = ApplicationConfiguration.getProperty("oap.notifications.sms.sid");
        AUTH_TOKEN = ApplicationConfiguration.getProperty("oap.notifications.sms.auth");
        fromNumber = ApplicationConfiguration.getProperty("oap.notifications.sms.number");
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }
    
    public Message sendSMS(String phoneNumber, String message) {
        Message smsMsg = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(fromNumber),
                message)
            .create();
        return smsMsg;
    }

    public static void main(String[] args) {
//        JWhich.which("org.apache.http.client.HttpClient");
        try {
            Message message = SendSMS("2067957934", "Who ya gonna call?");
            System.out.println(message.getSid());
        } catch (Exception ex) {
            logger.warn(ex,ex);
            System.exit(-1);
        }
    }
}