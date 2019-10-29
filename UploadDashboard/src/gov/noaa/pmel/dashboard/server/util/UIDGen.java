/**
 * 
 */
package gov.noaa.pmel.dashboard.server.util;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;

/**
 * @author kamb
 *
 */
public class UIDGen {
    
    // Function to generate a short url from integer ID  
    public static String idToShortURL(long n)  
    {  
        // Map to store 62 possible characters  
        char map62[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();  
        char map36[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();  
      
        StringBuffer shorturl = new StringBuffer();  
      
        // Convert given integer id to a base 62 number  
        while (n > 0)  
        {  
            // use above map to store actual character  
            // in short url  
            int mod = (int)(n % 36);
            char c = map36[mod];
            if ( c == 'I' ) c = '1';
            else if ( c == 'O' || c == 'Q' ) c = '0';
            shorturl.append(c); 
            n = n / 36;  
        }  
      
        // Reverse shortURL to complete base conversion  
        return shorturl.reverse().toString();  
    }  
      
    // Function to get integer ID back from a short url  
    public static long shortURLtoID(String shortURL)  
    {  
        long id = 0; // initialize result  
      
        // A simple base conversion logic  
        for (int i = 0; i < shortURL.length(); i++)  
        {  
            if ('a' <= shortURL.charAt(i) &&  
                       shortURL.charAt(i) <= 'z')  
            id = id * 62 + shortURL.charAt(i) - 'a';  
            if ('A' <= shortURL.charAt(i) &&  
                       shortURL.charAt(i) <= 'Z')  
            id = id * 62 + shortURL.charAt(i) - 'A' + 26;  
            if ('0' <= shortURL.charAt(i) &&  
                       shortURL.charAt(i) <= '9')  
            id = id * 62 + shortURL.charAt(i) - '0' + 52;  
        }  
        return id;  
    }  
      
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Driver Code 
    public static void main (String[] args) throws IOException
    { 
        try {
            String ts = idToShortURL(new Date().getTime());
            System.out.println(ts);
//            SecureRandom sr = new SecureRandom();
//            byte[] bytes = new byte[15];
//            sr.nextBytes(bytes);
//            UUID tooShort = UUID.nameUUIDFromBytes(bytes);
//            System.out.println(tooShort);
            MessageDigest salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes("UTF-8"));
            String digest = bytesToHex(salt.digest());
            System.out.println(digest + "[" + digest.length() + "]");

            UUID uuid = UUID.randomUUID();
            System.out.println("UUID: " + uuid.toString());
            long n = uuid.getLeastSignificantBits();
            System.out.println("LSB: " + n);
            String shorturl = idToShortURL(n);
            System.out.println("Generated short url is " + shorturl);
            System.out.println("Id from url is " + shortURLtoID(shorturl));
        } catch (Exception ex ) {
            ex.printStackTrace();
        }
    }
}
