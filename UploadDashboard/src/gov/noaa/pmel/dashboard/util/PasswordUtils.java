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
    
    public static String passwordRules() {
        return "Passord must be at least " + PW_MIN_LENGTH + " characters long, and it must contain at least one each of\n" +
                "\t lower-case characters\n" +
                "\t upper-case characters\n" +
                "\t numbers\n" +
                "\t symbols  '!', '$', '%', '#', '&', '_', '*', '^'\n" + 
                "NOTE: To avoid mysterious failures, it's best to enclose a provided password in single quotes, as: 'password'";
    }
    
    private static void test(String pw) {
        try {
            validatePasswordStrength(pw);
        } catch (Exception ex) {
            System.out.println("Password unacceptable:\""+pw+"\"");
            System.out.println(passwordRules());
        }
    }
    public static void main(String[] args) {
        test("ch@ngeM3now!");
    }
    private static interface PasswordChecker {
        public void validate(String password) throws CredentialException;
    }
    
    // From https://stackoverflow.com/questions/48345922/reference-password-validation
    // which says "Don't do it this way!
    // explained:
//    ^ Assert position at the start of the line.
//    (?=\P{Ll}*\p{Ll}) Ensure at least one lowercase letter (in any script) exists.
//    (?=\P{Lu}*\p{Lu}) Ensure at least one uppercase letter (in any script) exists.
//    (?=\P{N}*\p{N}) Ensure at least one number character (in any script) exists.
//    (?=[\p{L}\p{N}]*[^\p{L}\p{N}]) Ensure at least one of any character (in any script) that isn't a letter or digit exists.
    // NOTE that this is more lenient (allowing more symbols) than the "rules" suggest.
//    [\s\S]{8,} Matches any character 8 or more times.
    // in this case using the static constant for min pw length
//    $ Assert position at the end of the line.
    private static final String pwRules1 = "^(?=\\P{Ll}*\\p{Ll})(?=\\P{Lu}*\\p{Lu})(?=\\P{N}*\\p{N})(?=[\\p{L}\\p{N}]*[^\\p{L}\\p{N}])[\\s\\S]{"+PW_MIN_LENGTH+",}$";
    
    private static PasswordChecker getPasswordChecker() {
        return new PasswordChecker() {
            @Override
            public void validate(String password) throws CredentialException {
                boolean good = password.matches(pwRules1);
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
