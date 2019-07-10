package net.bytle.crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Hmac is a one way algorithm used to verify
 * * the data integrity
 * * the signature (for authentication)
 * <p>
 * We are using it to verify the data integrity. See test.
 * *
 * See doc:
 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#Mac
 * <p>
 * See example:
 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#HmacEx
 * <p>
 * Stackoverflow:
 * From https://stackoverflow.com/questions/3208160/how-to-generate-an-hmac-in-java-equivalent-to-a-python-example
 */
public class HmacTransformation implements TransformationOneWay {


    final static private String TRANSFORMATION = "HmacSHA256"; // could be also "HmacMD5"  HmacSHA1
    private byte[] key;


    private HmacTransformation(byte[] key) {
        this.key = key;
    }

    public static HmacTransformation get(String passphrase) {
        return new HmacTransformation(Cryptos.toBytes(passphrase));
    }

    public static HmacTransformation get(byte[] key) {
        return new HmacTransformation(key);
    }




    /**
     * @return the shared secret key
     * Generate a key for the HMAC-MD5 keyed-hashing algorithm; see RFC 2104
     * In practice, you would save this key.
     */
    public byte[] getKey() {
        if (key == null) {

            try {
                KeyGenerator keyGen = KeyGenerator.getInstance(TRANSFORMATION);
                SecretKey secretKey = keyGen.generateKey();
                key = secretKey.getEncoded();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

        }
        return key;
    }

    /**
     * @param plaintext
     * @return the signed digest hash
     */
    @Override
    public byte[] encrypt(String plaintext) {
        return encrypt(Cryptos.toBytes(plaintext));
    }


    @Override
    public Integer getCode() {
        return null;
    }

    /**
     * @param cipherText
     * @return the signed digest hash
     */
    @Override
    public byte[] encrypt(byte[] cipherText) {
        try {
            Mac mac = Mac.getInstance(TRANSFORMATION);
            SecretKeySpec secretKey = new SecretKeySpec(getKey(), TRANSFORMATION);
            mac.init(secretKey);
            return mac.doFinal(cipherText);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
