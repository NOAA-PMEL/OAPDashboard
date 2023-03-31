/**
 * 
 */
package gov.noaa.pmel.dashboard.server.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.Date;

import org.apache.logging.log4j.LogManager;

import gov.noaa.pmel.tws.util.TimeUtils;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.StringUtils;

import org.apache.logging.log4j.Logger;

import com.twilio.rest.api.v2010.account.Message;

/**
 * @author kamb
 *
 */
public class Notifications {

	static Logger logger = LogManager.getLogger(Notifications.class);

	private static final int MAX_SMS_MSG_LENGTH = 160;

    private static final String DEFAULT_EMAIL = "sdis.pmel@noaa.gov";
    private static final String ADMIN_EMAIL = "oar.pmel.sdis.admin@noaa.gov";
    
	public static final String OADB_RETURN_ADDR = "\"OAP Dashboard\" <" + DEFAULT_EMAIL + ">"; // noreply@pmel.noaa.gov>";

	public static void SendSMS(String message, Iterable<String> phoneNumbers) {
		for (String phoneNumber : phoneNumbers) {
			SendSMS(message, phoneNumber);
			try { Thread.sleep(200); } catch (Exception ex) {} // ignore
		}
	}
	
	public static void SendSMS(String message, String phoneNumber) {
		logger.info("sending " + message + " sms to: " + phoneNumber);
		if (StringUtils.emptyOrNull(message) || StringUtils.emptyOrNull(phoneNumber)) {
			throw new IllegalArgumentException("Missing SMS parameter: #" + phoneNumber + ", msg: " + message);
		}
		try {
			String sendMsg = message;
			if (message.length() > MAX_SMS_MSG_LENGTH) {
				logger.warn("Truncating over-long ("+message.length() + "/" + MAX_SMS_MSG_LENGTH + ") SMS message: " + message);
				sendMsg = message.substring(0, MAX_SMS_MSG_LENGTH);
			}
            Message sms = TwilioService.SendSMS(phoneNumber, sendMsg);
			logger.debug("sms status:"+ sms.getStatus());
		} catch (Exception ex) {
			logger.warn(ex,ex);
		}
	}
    
    // No pre-send logging, in case we're panicking over the logging.
	private static void justSendSMS(String message, String phoneNumber) {
		if (StringUtils.emptyOrNull(message) || StringUtils.emptyOrNull(phoneNumber)) {
			throw new IllegalArgumentException("Missing SMS parameter: #" + phoneNumber + ", msg: " + message);
		}
        String trimmedMessage = message.trim();
        if ( trimmedMessage.length() > MAX_SMS_MSG_LENGTH ) {
            trimmedMessage = trimmedMessage.substring(0, MAX_SMS_MSG_LENGTH);
        }
        String cleanedNumber = phoneNumber.trim();
        cleanedNumber = cleanNumber(cleanedNumber);
		try {
			String sendMsg = trimmedMessage;
			Message sms = new TwilioService().sendSMS(cleanedNumber, sendMsg);
		} catch (Exception ex) {
            ex.printStackTrace();
			logger.warn(ex,ex);
		}
	}
    
    private static String cleanNumber(String phone_number) {
        if ( phone_number == null || phone_number.trim().isEmpty()) {
            throw new IllegalArgumentException("Null or empty phone number.");
        }
        String cleaned = phone_number.trim();
        String plus = cleaned.startsWith("+") ? "+" : "";
        cleaned = phone_number.replaceAll("[^\\d]", "");
        cleaned = plus + cleaned;
        return cleaned;
    }
	
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

    public static void Alert(String subject, Throwable t) {
        SendEmail(subject, buildExceptionMessage(t), "linus.kamb@noaa.gov");
    }
        
    /**
     * @param t
     * @return
     */
    private static String buildExceptionMessage(Throwable t) {
        StringBuilder b = new StringBuilder(String.valueOf(t));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        t.printStackTrace(ps);
        String trace = new String(baos.toByteArray());
        b.append(trace);
        return b.toString();
    }

    public static void AdminEmail(String subject, String message) {
        String defaultAdmin = ApplicationConfiguration.getProperty("oap.email.from", "\"OAP Dashboard System\" <"+DEFAULT_EMAIL+">");
        String adminList = ApplicationConfiguration.getLatestProperty("oap.admin.email.list", defaultAdmin);
        SendEmail(subject, message, adminList);
    }
    
	public static void SendEmail(String subject, String message, Iterable<String> toList) {
		SendEmail(subject, message, buildRecipientListString(toList));
	}
	
	public static String getSmtpServer() {
		return ApplicationConfiguration.getProperty("email.smtp_host", "smtp.pmel.noaa.gov");
	}
	
	public static void SendEmailIf(String subject, String message, String toList) {
        if ( ! StringUtils.emptyOrNull(toList)) {
            SendEmail(subject, message, toList);
        }
	}
	public static void SendEmail(String subject, String message, String toList) {
        String from = ApplicationConfiguration.getProperty("oap.email.from", "\"OAP Dashboard System\" <"+DEFAULT_EMAIL+">");
        SendEmail(subject, message, toList, from);
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
    
	public static void PANIC(String msg) {
	    String dateString = TimeUtils.formatISO8601(new Date());
        String panicFileName = "PANIC.log";
        File panicFile = new File(panicFileName);
        String panicMsg = msg;
        String datedMsg = dateString + " : " + panicMsg;
        System.out.println("#########");
        System.out.println("######### PANIC!");
        System.out.println("######### " + datedMsg);
        System.out.println("######### Check : " + panicFile.getAbsolutePath());
        System.out.println("#########");
        try (FileWriter fout = new FileWriter(panicFile);) {
            fout.append(datedMsg).append("\n");
        } catch (Throwable t) {
            System.out.println("######### Exception writing to PANIC.log : " + t);
            t.printStackTrace();
        }
        try {
            TwilioService.SendSMS("2067957934", "SDIS:" + panicMsg);
        } catch (Throwable t) {
            System.out.println("######### Exception sending SMS Notification : " + t);
            t.printStackTrace();
        }
	}
	
    static void usage() {
        System.out.println("Usage: notify [-s|-m] arguments" );
        System.out.println("\t-s : send SMS. args: <telno> <message>");
        System.out.println("\t-m : send email. args: <to_addr_list> <subject> <message>");
    }
    /**
	 * @param args
	 */
	public static void main(String[] args) {
		String me = "2067957934";
		String eugene = "2062952338";
		
        if ( args.length < 3 ) {
            usage();
            System.exit(-1);
        }
        if ( "-s".equals(args[0]) && args.length >= 3) {
            SendSMS(args[2], args[1]);
        } else if ( "-m".equals(args[0]) && args.length == 4 ) {
            SendEmail(args[2], args[3], args[1], 
					 "\"SDIS\" <pmel.sdis@noaa.gov>");
        } else {
            usage();
        }
	}

}
