package net.bytle.crypto;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.GeneralSecurityException;

public class Protector {

    private final static byte[] SALT = {
            (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
            (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };
    private final static String ALGORITHM = "PBEWithMD5AndDES";

    private final Base64 base64 = new Base64();
    private Cipher encryptCipher;
    private Cipher decryptCipher;

    /**
     * Passphrase is mandatory (otherwise it is a nonsense)
     * @param passphrase
     * @throws GeneralSecurityException
     */
    private Protector(String passphrase) throws GeneralSecurityException {

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
        SecretKey secretKey = secretKeyFactory.generateSecret(new PBEKeySpec(passphrase.toCharArray()));
        encryptCipher = Cipher.getInstance(ALGORITHM);
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, new PBEParameterSpec(SALT, 20));
        decryptCipher = Cipher.getInstance(ALGORITHM);
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, new PBEParameterSpec(SALT, 20));
    }

    public static Protector get(String master){
        try {
            return new Protector(master);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public String encrypt(String unencrypted)  {
        if (unencrypted==null){
            return null;
        }
        try {
            return base64.encodeAsString(encryptCipher.doFinal(unencrypted.getBytes()));
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encrypted)  {
        if (encrypted==null){
            return null;
        }
        try {
            return new String(decryptCipher.doFinal(base64.decode(encrypted)));
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}