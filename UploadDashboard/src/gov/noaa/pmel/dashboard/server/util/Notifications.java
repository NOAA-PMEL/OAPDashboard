/**
 * 
 */
package gov.noaa.pmel.dashboard.server.util;

import javax.mail.Message.RecipientType;

import org.apache.logging.log4j.LogManager;

//import gov.noaa.pmel.tsunami.server.notify.TwilioSMSnotifier;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.StringUtils;

import org.apache.logging.log4j.Logger;


/**
 * @author kamb
 *
 */
public class Notifications {

	static Logger logger = LogManager.getLogger(Notifications.class);

	public static final String OADB_RETURN_ADDR = "\"OAP Dashboard\" <noreply@pmel.noaa.gov>";

	private static final int MAX_SMS_MSG_LENGTH = 160;

//	public static void SendSMS(String message, Iterable<String> phoneNumbers) {
//		for (String phoneNumber : phoneNumbers) {
//			SendSMS(message, phoneNumber);
//			try { Thread.sleep(200); } catch (Exception ex) {} // ignore
//		}
//	}
//	
//	public static void SendSMS(String message, String phoneNumber) {
//		logger.info("sending " + message + " sms to: " + phoneNumber);
//		if (StringUtils.emptyOrNull(message) || StringUtils.emptyOrNull(phoneNumber)) {
//			throw new IllegalArgumentException("Missing SMS parameter: #" + phoneNumber + ", msg: " + message);
//		}
//		try {
//			String sendMsg = message;
//			if (message.length() > MAX_SMS_MSG_LENGTH) {
//				logger.warn("Truncating over-long ("+message.length() + "/" + MAX_SMS_MSG_LENGTH + ") SMS message: " + message);
//				sendMsg = message.substring(0, MAX_SMS_MSG_LENGTH);
//			}
//			TwilioSMSnotifier notifier = new TwilioSMSnotifier();
//			Sms sms = notifier.sendSMS(sendMsg, phoneNumber);
//			logger.debug("sms status:"+ sms.getStatus());
//		} catch (Exception ex) {
//			logger.warn(ex,ex);
//		}
//	}
	
	private static String buildRecipientListString(Iterable<String> addrs) {
		if ( addrs == null ) {
			throw new IllegalArgumentException("No email recipients provided.");
		}
		String comma = "";
		StringBuilder addrList = new StringBuilder();
		for (String addr : addrs) {
			addrList.append(comma).append(addr);
			comma=",";
		}
		return addrList.toString();
	}

	public static void SendEmail(String subject, String message, Iterable<String> toList, String from) {
		SendEmail(subject, message, buildRecipientListString(toList), from);
	}
	
	public static String getSmtpServer() {
		return ApplicationConfiguration.getProperty("email.smtp_host", "smtp.pmel.noaa.gov");
	}
	
	public static void SendEmail(String subject, String message, String toList, String from) {
		logger.info("sending " + subject + " email to: " + toList);
		if (StringUtils.emptyOrNull(toList)) {
			throw new IllegalArgumentException("empty to list");
		}
		try {
            OapMailSender oams = new OapMailSender();
            oams.sendMessage(toList, subject, message);
		} catch (Exception e) {
			logger.warn("Failed to send \""+ subject + "\" message to: " + toList, e);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String me = "2067957934";
		String eugene = "2062952338";
		
		// System.out.println("Notify:"+ApplicationConfiguration.getProperty("eids.notifications"));
		 SendEmail("OAP Data Dashboard Test notification", "Hi John!\n I just wanted to let you know, I'm back working on the data archive dashboard, "
		         + "which will include (occasional) messages to you that new bundles have been uploaded to the SFTP site.\n\nThanks - Linus", "linus.kamb@gmail.com,linus.kamb@noaa.gov", 
					 "\"OAP Dashboard System\" <linus.kamb@noaa.gov>");

	}

}
