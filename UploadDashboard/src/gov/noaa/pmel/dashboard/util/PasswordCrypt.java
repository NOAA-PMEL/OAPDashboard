/**
 * 
 */
package gov.noaa.pmel.dashboard.util;

import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import org.apache.catalina.realm.SecretKeyCredentialHandler;

import gov.noaa.pmel.tws.util.JWhich;

/**
 * @author kamb
 *
 */
public class PasswordCrypt {

    static String algorithm = "PBKDF2WithHmacSHA512";
    static int iterations = 100000;
    static int saltLength = 16;
    static int keyLength = 256;

    public static String generateTomcatPasswd(String forPassword) throws GeneralSecurityException {
        // XXX Wrong version of this can come from GWT if it's higher in the classpath.
//        JWhich.which("org.apache.tomcat.util.res.StringManager"); 
        SecretKeyCredentialHandler credible = new SecretKeyCredentialHandler();
        credible.setAlgorithm(algorithm);
        credible.setIterations(iterations);
        credible.setSaltLength(saltLength);
        credible.setKeyLength(keyLength);
        String creds = credible.mutate(forPassword);
//        if ( ! tomcatPasswdMatches(creds, forPassword)) {
//            throw new GeneralSecurityException("Password generation failure: Match Fail!");
//        }
        return creds;
    }
    
    public static boolean tomcatPasswdMatches(String crypt, String raw) throws NoSuchAlgorithmException {
        SecretKeyCredentialHandler credible = new SecretKeyCredentialHandler();
        credible.setAlgorithm(algorithm);
        credible.setIterations(iterations);
        credible.setSaltLength(saltLength);
        credible.setKeyLength(keyLength);
        return credible.matches(raw, crypt);
    }
    public static String generateJettyPasswd(String username, String forPassword) throws GeneralSecurityException {
        try {
            String credentialsClassName = "org.eclipse.jetty.util.security.Credential";
            JWhich.which(credentialsClassName);
            Class<?> credible = Class.forName(credentialsClassName);
            Class<?>[] classes = credible.getClasses();
            Class<?> crypto = null;
            for (Class<?> c : classes ) {
                System.out.println(c.getName());
                if ( c.getName().equals("org.eclipse.jetty.util.security.Credential$Crypt")) {
                    crypto = c;
                    break;
                }
            }
            if ( crypto == null ) {
                throw new ClassNotFoundException("Could not find Jetty Cryptor class.");
            }
            Method crypter = crypto.getMethod("crypt", String.class, String.class);
            String creds = (String) crypter.invoke(null, username, forPassword);
            return creds;
        } catch (Exception ex) {
            throw new GeneralSecurityException(ex);
        }
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        String passwd = args.length > 0 ? args[0] : "password";
                
        String crypt = "6d9e04ea1979cbce94913043258080c9$100000$3ab634810abf91f8db19752590f37b2ab4234fe4c5fa53d1c4b75a157e5f08b8";
        crypt = "acfa5a1b03aa02dfef694698a4cd427c$100000$aca45514f9438829aa176f0c027845509633522e401fac8f406146725a73a9da";
//        crypt = "716bff1f86c88400762268019dfbd519$100000$8563bfcd1d81dbd689c02b450896274e76d173b9d91b84130dacec3a498e8885";
        try {
//            JWhich.which("gov.noaa.pmel.tsunami.util.JWhich");
//            JWhich.which("org.apache.logging.log4j.Logger");
//            JWhich.which("org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload");
//            System.out.println(generateTomcatPasswd(passwd));
            System.out.println("matches: " + tomcatPasswdMatches(crypt, passwd));
        } catch (GeneralSecurityException ex) {
            ex.printStackTrace();
        }
        
//        String[] pgenArgs = new String[] { "-a", algorithm, 
//                                           "-i", String.valueOf(iterations), 
//                                           "-s", String.valueOf(saltLength), 
//                                           "-k", String.valueOf(keyLength), 
//                                           "-h", "org.apache.catalina.realm.SecretKeyCredentialHandler", passwd };
//        try {
//            Class<?> cls = Class.forName("org.apache.catalina.realm.RealmBase");
//            Class<?>[] paramTypes = new Class[1];
//            String[] foo = new String[0];
//            paramTypes[0] = foo.getClass();
//            
//            Class<?>[] argTypes = new Class[] { String[].class };
//            Method main = cls.getDeclaredMethod("main", argTypes);
//            main.invoke(null, (Object)pgenArgs);
//            
//            System.out.println(passwd + ":"+creds);
//            
//            System.out.println("current:"+credible.matches(passwd, creds));
//            System.out.println("past:"+credible.matches(passwd, past));
//            
////            main.invoke(null, pgenArgs);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            // TODO: handle exception
//        }
    }

}
