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

public class TwilioService {
    
    private static Logger logger = LogManager.getLogger(TwilioService.class);
    
    // Find your Account SID and Auth Token at twilio.com/console
    // and set the environment variables. See http://twil.io/secure
    /** The Constant ACCOUNT_SID. Find it at twilio.com/user/account */
    private static final String ACCOUNT_SID = "AC3002ba55efdf4bd6b77ed1957b1bef66";

    /** The Constant AUTH_TOKEN. Find it at twilio.com/user/account */
    private static final String AUTH_TOKEN = "29155f2ac2073c70251037482eb5aa33";

    private static final String fromNumber = "+14342608932";
            
    public static Message SendSMS(String phoneNumber, String message) {
        return new TwilioService().sendSMS(phoneNumber, message);
    }
    
    public TwilioService() {
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
        Message message = SendSMS("2067957934", "Who ya gonna call?");
        System.out.println(message.getSid());
    }
}