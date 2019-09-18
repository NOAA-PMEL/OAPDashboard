/**
 * 
 */
package gov.noaa.pmel.dashboard.server.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.ApplicationConfiguration.PropertyNotFoundException;
import gov.noaa.pmel.tws.util.TimeUtils;

/**
 * @author kamb
 *
 */
public class OapMailSender {

    private static Logger logger = Logger.getLogger(OapMailSender.class);
    
    private static final String PROGRAM_NAME = "oap_mail";
    private static final String MAILER = "pmel/OapMailer";
    
    public static void usage() {
        System.out.println("usage: " + PROGRAM_NAME + " -s <IMEI> -a <command_file> [recipient]");
    }
    
    private static Properties setup() {
        String smtp_server = ApplicationConfiguration.getProperty("oap.email.smtp.server", "smtp.gmail.com");
        // Set up properties for the mail session
        Properties props = new Properties();
        // either use SSL / smtps and port 465, or smtp + STARTTLS and port 587
        String protocol = ApplicationConfiguration.getProperty("oap.email.smtp.protocol", "smtps");
        String port = protocol.equals("smtp") ? "587" : "465";
        props.put("mail.transport.protocol", protocol);
        props.put("mail."+protocol+".host", smtp_server);
        props.put("mail."+protocol+".port", port ); 
        props.put("mail."+protocol+".auth", "true");
        props.put("mail."+protocol+".starttls.enable", "true");
        props.put("mail."+protocol+".starttls.required", "true");
        boolean debug = ApplicationConfiguration.getProperty("oap.email.debug", false);
        props.put("mail.debug",  new Boolean(debug));
        return props;
    }
    
    private Session session ;
    private String from ;
    private String acct ;
    private String dapw ;
    private boolean logSentMessages;
    
    public OapMailSender() throws PropertyNotFoundException {
        logSentMessages = ApplicationConfiguration.getProperty("oap.email.log_sent_messages", false);
        acct = ApplicationConfiguration.getProperty("oap.email.account", "linus.kamb@noaa.gov" ); // "\"OAPDashboard\" <no_reply@pmel.noaa.gov>");
        from = ApplicationConfiguration.getProperty("oap.email.from", "OAPDashboard <no_reply@pmel.noaa.gov>");
        dapw = ApplicationConfiguration.getProperty("oap.email.password");
    }
    
    public void sendMessage(String to, String subject, String message) throws Exception {
		logger.info("Attempting to send msg " + subject + " to: " + to);
        Properties mailProps = setup();
        session = Session.getInstance(mailProps, null);
        session.setDebug(ApplicationConfiguration.getLatestProperty("oap.email.debug", false));
        session.setDebugOut(System.out);
        try ( Transport transport = session.getTransport()) {
            String passwd = dapw;
            transport.connect(acct, passwd);
            _sendMessage(transport, to, subject, message, (String[])null);
        }
    }
        
    private void sendAttachment(String to, String subject, String attachmentName) throws Exception {
        Properties mailProps = setup();
        session = Session.getInstance(mailProps, null);
        session.setDebug(ApplicationConfiguration.getLatestProperty("oap.email.debug", false));
        session.setDebugOut(System.out);
        try ( Transport transport = session.getTransport()) {
            String passwd = dapw;
            transport.connect(acct, passwd);
            _sendMessage(transport, to, subject, "textmail", attachmentName);
        }
    }

	private void _sendMessage(Transport transport, String to, String subject, String body, String... attachments)
			throws Exception {
		try {
		MimeMessage msg = new MimeMessage(session);

		// set headers
        if ( from != null ) {
    		msg.setFrom(InternetAddress.parse(from, false)[0]);
        }
		msg.setHeader("X-Mailer", MAILER);
		msg.setSentDate(new Date());
		msg.setRecipients(RecipientType.TO, InternetAddress.parse(to, false));
		// set title and body
		msg.setSubject(subject);
        MimeBodyPart mimeBody = new MimeBodyPart();
		mimeBody.setText(body);
        
        MimeMultipart mm = new MimeMultipart();
        mm.addBodyPart(mimeBody);
        
        if ( attachments != null ) {
            for (String attachment : attachments) {
        		MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(attachment, "application/octet-stream", null);
                mm.addBodyPart(attachmentPart);
            }
        }
        
        msg.setContent(mm);
		
		logger.info("Sending " + msg.getSubject() + " from: " + from + " to: " + Arrays.asList(msg.getAllRecipients()));

		// off goes the message...
		transport.sendMessage(msg, msg.getAllRecipients());

        if ( logSentMessages ) {
            saveMsg(msg);
        }
        
//		logger.debug("Message mailed from " + from);
		} catch (Exception mex) {
            logger.warn("Exception sending message:" + mex);
			mex.printStackTrace();
			throw mex;
		}
	}
    private static void saveMsg(MimeMessage msg) throws Exception {
        try {
            String logDirName = ApplicationConfiguration.getProperty("oap.email.sent_message_archive", "sent_messages");
            File logDir = new File(logDirName);
            if ( ! logDir.exists()) {
                logDir.mkdirs();
            }
            String floatId = msg.getSubject().replaceAll(" ", "");
            File floatDir = new File(logDirName, floatId);
            if ( !floatDir.exists()) {
                floatDir.mkdirs();
            }
            String messageId = TimeUtils.format_ISO_COMPRESSED(new Date());
            File messageFile = new File(floatDir, messageId+".eml");
            try ( OutputStream os = new FileOutputStream(messageFile)) {
                msg.writeTo(os);
            }
        } catch (IOException ex) {
            logger.warn("Exception saving sent message: " + ex);
        }
    }

    public static void main(String[] args) {
        try {
            String datasetId = "06AQ20150817";
            String userRealName = "Linus Kamb";
            String subject = "TESTING: Archive bundle posted for dataset ID: " + datasetId;
            String message = "A dataset archive bundle for " + userRealName + " was posted to the SFTP site for pickup.\n"
                           + "The archive bundle is available for pickup at ncei_sftp@sftp.pmel.noaa.gov/data/oap/" + "06AQ20150817_baggit.zip";
                String toList = ApplicationConfiguration.getProperty("oap.archive.notification.list");
                new OapMailSender().sendMessage(toList, subject, message);

        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }
    /**
     * @param args
     */
    public static void _main(String[] args) {
        if ( System.getProperty("configuration_dir") == null ) {
            System.setProperty("configuration_dir", "bin/conf");
        }
        String attachment = null;
        String recipient = null;
        String serialNumber = null;
        if ( args.length < 5 ) {
            usage();
            System.exit(2);
        }
        try {
            recipient = args[args.length-1];
            if ( recipient.indexOf('@') <= 0 ) {
                throw new IllegalArgumentException("Bad recipient email address: " + recipient );
            }
            for (int i = 0; i < args.length-2; i+=2 ) {
                if ( "-s".equals(args[i])) {
                    serialNumber = args[i+1];
                } else if ( "-a".equals(args[i])) {
                    attachment = args[i+1];
                } else {
                    System.err.println("Unexpected command line flag: " + args[i]);
                }
            }
            if ( attachment == null || serialNumber == null || recipient == null ) {
                System.err.println("Missng required parameter.");
                usage();
                System.exit(2);
            }
            File attachmentFile = new File(attachment);
            if ( ! attachmentFile.exists()) {
                throw new FileNotFoundException("Cannot find file " + attachmentFile.getAbsolutePath());
            }
            ApplicationConfiguration.Initialize("argo");
            OapMailSender ams = new OapMailSender();
            ams.sendAttachment(recipient, serialNumber, attachment);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
            usage();
        }

    }

}
