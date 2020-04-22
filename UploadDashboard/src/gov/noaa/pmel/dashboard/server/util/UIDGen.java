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

import com.fasterxml.uuid.Generators;

/**
 * @author kamb
 *
 */
public class UIDGen {
    
        // Map to store 62 possible characters  
    static char map62[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();  
    static char map36[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();  
    static char map33[] = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789".toCharArray();  
    static char fix33[] = "AAAAAAAABBBBBCDDDDDDDDD0123456789".toCharArray();  
    static int charMapLength = 33;
      
    public static String idToShortURL(long n) {
        return idToShortURL(n, map33);
    }
    
    private static String idToShortURL(long n, char[] map) {
        StringBuffer shorturl = new StringBuffer();  
      
        int mapLen =  map.length;
        System.out.println("==== " + mapLen);
        
        String comma = "";
        // Convert given integer id to a base mapLen number  
        while (n > 0)  
        {  
            int mod = (int)(n % mapLen);
            char c = map[mod];
            System.out.println(comma + " " + mod + ":" + c);
//            comma = ",";
            shorturl.append(c); 
            n = n / mapLen;  
        }  
      
        // Reverse shortURL to complete base conversion  
        return shorturl.reverse().toString();  
        
    }
    private static String idToShortURLmap62(long n)  
    {  
        StringBuffer shorturl = new StringBuffer();  
      
        String comma = "";
        
        // Convert given integer id to a base 62 number  
        while (n > 0)  
        {  
            // use above map to store actual character  
            // in short url  
            int mod = (int)(n % 62);
            char c = map62[mod];
            System.out.println(comma + " " + mod + ":" + c);
//            comma = ",";
            shorturl.append(c); 
            n = n / 62;  
        }  
      
        // Reverse shortURL to complete base conversion  
        return shorturl.reverse().toString();  
    }  
      
    // Function to generate a short url from integer ID  
    private static String idToShortURLmap33(long n)  
    {  
        StringBuffer shorturl = new StringBuffer();  
      
        // Convert given integer id to a base 62 number  
        while (n > 0)  
        {  
            // use above map to store actual character  
            // in short url  
            int mod = (int)(n % charMapLength);
            char c = map33[mod];
//            if ( c == 'I' ) c = '1';
//            else if ( c == 'O' || c == 'Q' ) c = '0';
            shorturl.append(c); 
            n = n / charMapLength;  
        }  
      
        // Reverse shortURL to complete base conversion  
        return shorturl.reverse().toString();  
    }  
      
    // Function to get integer ID back from a short url  
    private static long shortURLtoIDmap62(String shortURL)  
    {  
        long id = 0; // initialize result  
      
        String comma = "";
        // A simple base conversion logic  
        for (int i = 0; i < shortURL.length(); i++)  
        {  
            char c = shortURL.charAt(i);
            int v = -999;
            if ('a' <= c &&  
                       c <= 'z')   {
                v = c - 'a';  
                id = id * 62 + v;
            }
            if ('A' <= c &&  
                       c <= 'Z')  {
                v = c - 'A' + 26;  
                id = id * 62 + v;
            }
            if ('0' <= c &&  
                       c <= '9') {
                v = c - '0' + 52;  
                id = id * 62 + v;
            }
            System.out.println(comma +  c + ":"+v);
//            comma = ", ";
        }  
        return id;  
    }  
      
    private static long shortURLtoIDmap33(String shortURL)  
    {  
        long id = 0; // initialize result  
      
        String comma = "";
        // A simple base conversion logic  
        for (int i = 0; i < shortURL.length(); i++)  
        {  
            char c = shortURL.charAt(i);
            int v = -999;
//            if ('a' <= shortURL.charAt(i) &&  
//                       shortURL.charAt(i) <= 'z')  
//                id = id * 62 + shortURL.charAt(i) - 'a';  
            if ('A' <= c &&  
                       c <= 'Z')  {
                v = c - fix33[c-'A'];  
                id = id * 33 + v;
            }
            if ('0' <= c &&  
                       c <= '9')  {
                v = c - '0' + 23;  
                id = id * 33 + v;
            }
            System.out.println(comma +  c + ":"+v);
//            comma = ", ";
        }  
        return id;  
    }  
    private static long shortURLtoIDmap36(String shortURL)  
    {  
        long id = 0; // initialize result  
      
        // A simple base conversion logic  
        for (int i = 0; i < shortURL.length(); i++)  
        {  
//            if ('a' <= shortURL.charAt(i) &&  
//                       shortURL.charAt(i) <= 'z')  
//                id = id * 62 + shortURL.charAt(i) - 'a';  
            if ('A' <= shortURL.charAt(i) &&  
                       shortURL.charAt(i) <= 'Z')  
                id = id * 36 + shortURL.charAt(i) - 'A';  
            if ('0' <= shortURL.charAt(i) &&  
                       shortURL.charAt(i) <= '9')  
                id = id * 36 + shortURL.charAt(i) - '0' + 26;  
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
//            String fasterId = Generators.timeBasedGenerator().generate().toString();
//            System.out.println("faster:"+fasterId);
//            String ts = idToShortURLmap33(new Date().getTime());
//            System.out.println(ts);
////            SecureRandom sr = new SecureRandom();
////            byte[] bytes = new byte[15];
////            sr.nextBytes(bytes);
////            UUID tooShort = UUID.nameUUIDFromBytes(bytes);
////            System.out.println(tooShort);
//            MessageDigest salt = MessageDigest.getInstance("SHA-256");
//            salt.update(UUID.randomUUID().toString().getBytes("UTF-8"));
//            String digest = bytesToHex(salt.digest());
//            System.out.println(digest + "[" + digest.length() + "]");
//
            UUID uuid = UUID.randomUUID();
            System.out.println("UUID: " + uuid.toString());
            long n = Math.abs(uuid.getLeastSignificantBits());
            System.out.println("LSB: " + n);
            String shorturl = idToShortURL(n, map62);
            System.out.println("Generated 62 short url is " + shorturl);
            System.out.println("Id 62 from url is " + shortURLtoIDmap62(shorturl));
            shorturl = idToShortURL(n, map33);
            System.out.println("Generated 36 short url is " + shorturl);
            System.out.println("Id 36 from url is " + shortURLtoIDmap33(shorturl));
        } catch (Exception ex ) {
            ex.printStackTrace();
        }
    }
}
