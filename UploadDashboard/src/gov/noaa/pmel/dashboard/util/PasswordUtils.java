/**
 * 
 */
package gov.noaa.pmel.dashboard.util;

import java.util.Random;

import javax.security.auth.login.CredentialException;

import gov.noaa.pmel.tws.util.ApplicationConfiguration;

/**
 * @author kamb
 *
 */
public class PasswordUtils {

    private static final int PW_MIN_LENGTH = 12;
    
    /**
     * @param newPlainTextPasswd
     */
    public static void validatePasswordStrength(String newPlainTextPasswd) throws CredentialException {
        boolean validate = ApplicationConfiguration.getProperty("oap.user.password.validate", true);
        if ( validate ) {
            getPasswordChecker().validate(newPlainTextPasswd);
        }
    }
    
    private static interface PasswordChecker {
        public void validate(String password) throws CredentialException;
    }
    
    // From https://stackoverflow.com/questions/48345922/reference-password-validation
    // which says "Don't do it this way!
    private static final String pw2 = "^(?=\\P{Ll}*\\p{Ll})(?=\\P{Lu}*\\p{Lu})(?=\\P{N}*\\p{N})(?=[\\p{L}\\p{N}]*[^\\p{L}\\p{N}])[\\s\\S]{"+PW_MIN_LENGTH+",}$";
    
    private static PasswordChecker getPasswordChecker() {
        return new PasswordChecker() {
            @Override
            public void validate(String password) throws CredentialException {
                boolean good = password.matches(pw2);
                if ( !good ) {
                    throw new CredentialException("Password unacceptable.");
                }
            }
        };
    }
    
    /**
     * @return
     */
    public static String generateSecurePassword() {
        StringBuilder pw = new StringBuilder();
        char c = '_';
        for (int i = 0; i < PW_MIN_LENGTH; i++) {
            c = randomChar(pw.toString(), i+1);
            pw.append(c);
        }
        return pw.toString();
    }
    /**
     * @param c
     * @param i
     * @return
     */
    private static char randomChar(String current, int i) {
        char c;
        if ( i % 5 == 0 ) {
            c = randomNum(current);
        } else if ( i % 4 == 0 ) {
            c = randomSym(current);
        } else if ( i % 7 == 0 ) {
            c = randomCap(current);
        } else {
            c = randomChar(current);
        }
        return c;
    }
    /**
     * @param last
     * @return
     */
    private static char randomChar(String current) {
        char c = (char)(((int)(new Random().nextFloat() * 26)) + 'a');
        if ( current.indexOf(c) >= 0 ) {
            c = randomChar(current);
        }
        return c;
    }
    /**
     * @param last
     * @return
     */
    private static char randomCap(String current) {
        char c = (char)(((int)(new Random().nextFloat() * 26)) + 'A');
        if ( current.indexOf(c) >= 0 ) {
            c = randomCap(current);
        }
        return c;
    }
    /**
     * @param last
     * @return
     */
    private static char[] symbols = new char[] { '!', '$', '%', '#', '&', '_', '*', '^' };
    private static char randomSym(String current) {
        char c = symbols[(int)(new Random().nextFloat() * 8)];
        if ( current.indexOf(c) >= 0 ) {
            c = randomSym(current);
        }
        return c;
    }
    /**
     * @param last
     * @return
     */
    private static char randomNum(String current) {
        char c = (char)(((int)(new Random().nextFloat() * 10)) + '0');
        if ( current.indexOf(c) >= 0 ) {
            c = randomNum(current);
        }
        return c;
    }
}
