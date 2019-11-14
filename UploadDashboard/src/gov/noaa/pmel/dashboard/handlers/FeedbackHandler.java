/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.server.util.Notifications;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;

/**
 * @author kamb
 *
 */
public class FeedbackHandler {

    private static Logger logger = LogManager.getLogger(FeedbackHandler.class);
    
    /**
     * @param pageUsername
     * @param message
     */
    public static void logFeedbackMessage(String pageUsername, String type, String message) {
        logger.info(type + " Feedback msg from: " + pageUsername + ": " +  message);
    }

    /**
     * @param pageUsername
     * @param message
     */
    public static void notifyFeedbackMessage(String pageUsername, String type, String message) {
        String recipientProperty = ApplicationConfiguration.getProperty("oap.feedback.recipients", "linus.kamb@noaa.gov");
        String[] recipients = recipientProperty.split("[, ;]");
        List<String> toList  = Arrays.asList(recipients);
        System.out.println("Sending feedback msg to: " + toList);
        logger.info("Sending feedback msg to: " + toList);
        String subject = type + " feedback for OADashboard"; 
        String  msg = "TYPE: " + type + "\n" +
                      "FROM: " + pageUsername + "\n" +
                      "SENT: " + new Date().toString() + "\n" +
                      " ------ MSG ------ \n" +
                      message;
        Notifications.SendEmail(subject, msg, toList);
    }

}
