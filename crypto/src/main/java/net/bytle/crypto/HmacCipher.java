package net.bytle.crypto;

import sun.misc.BASE64Encoder;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class HmacCipher {

    // From https://stackoverflow.com/questions/3208160/how-to-generate-an-hmac-in-java-equivalent-to-a-python-example
    // md5 is the python hmac implementation
    public static String encode(String key, String message) throws Exception {

        // Generate a key for the HMAC-MD5 keyed-hashing algorithm; see RFC 2104
        // In practice, you would save this key.
        SecretKey keySpec;
        final String hmacCipher = "HmacMD5"; // could be also HmacSHA1
        if (key == null) {
            KeyGenerator keyGen = KeyGenerator.getInstance(hmacCipher);
            keySpec = keyGen.generateKey();
        } else {
            keySpec = new SecretKeySpec(
                    key.getBytes(),
                    hmacCipher);
        }


        Mac mac = Mac.getInstance(hmacCipher);
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(message.getBytes());

        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(rawHmac);

    }


}
