package net.bytle.crypto;

import sun.misc.BASE64Encoder;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * See example:
 * https://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#HmacEx
 *
 * Stackoverflow:
 * From https://stackoverflow.com/questions/3208160/how-to-generate-an-hmac-in-java-equivalent-to-a-python-example
 */
public class HmacCipher implements CipherAll {

    private String passphrase;
    final static private String TRANSFORMATION = "HmacSHA256"; // could be also "HmacMD5"  HmacSHA1
    private SecretKey secretKey;
    private byte[] key;

    public static HmacCipher get() {
        return new HmacCipher();
    }


    @Override
    public HmacCipher setPassphrase(String passphrase) {
        this.passphrase = passphrase;
        return this;
    }

    /**
     * To set a key (You would set a key or a passphrase but not both, if a key is given, a key is not generated from the passphrase)
     *
     * @param key
     * @return
     */
    @Override
    public CipherAll setKey(byte[] key) {
        this.key = key;
        return this;
    }


    public SecretKey getSecretKey() {

        // Generate a key for the HMAC-MD5 keyed-hashing algorithm; see RFC 2104
        // In practice, you would save this key.
        if (secretKey==null) {
            if (getKey() == null) {
                try {
                    KeyGenerator keyGen = KeyGenerator.getInstance(TRANSFORMATION);
                    secretKey = keyGen.generateKey();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

            } else {
                secretKey = new SecretKeySpec(
                        getKey(),
                        TRANSFORMATION);
            }
        }
        return secretKey;
    }

    public byte[] getKey() {
        if (key !=null){
            return key;
        } else if (passphrase!=null) {
            return passphrase.getBytes();
        } else {
            return null;
        }
    }

    @Override
    public byte[] encrypt(String plaintext) {
        return encrypt(Cryptos.toBytes(plaintext));
    }


    @Override
    public String decrypt(byte[] ciphertext) {
        return null;
    }


    @Override
    public Integer getVersion() {
        return null;
    }

    @Override
    public byte[] encrypt(byte[] cipherText) {
        try {
            Mac mac = Mac.getInstance(TRANSFORMATION);
            mac.init(getSecretKey());
            return  mac.doFinal(cipherText);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
