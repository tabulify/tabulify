package net.bytle.crypto;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

public class PasswordBasedEncryptionCipher implements CipherSalt {



    private final static String ALGORITHM = "PBEWithMD5AndDES";
    private String passphrase;
    private byte[] key;

    public static PasswordBasedEncryptionCipher get() {
        return new PasswordBasedEncryptionCipher();
    }

    public PasswordBasedEncryptionCipher setPassphrase(String passphrase) {
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

    /**
     * Used to produce another passphrase for the digest (hmac)
     *
     * @return
     */
    @Override
    public byte[] getKey() {
        return this.key;
    }


    public SecretKey getSecretKey() {

        try {
            if (key==null) {
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
                SecretKey secretKey = secretKeyFactory.generateSecret(new PBEKeySpec(this.passphrase.toCharArray()));
                key = secretKey.getEncoded();
            }
            return new SecretKeySpec(key, ALGORITHM);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    Cipher getEncryptCipher() {

        Cipher cipher;
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), getSaltSpec());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        return cipher;

    }

    public byte[] encrypt(String plaintext) {
        try {
            return getEncryptCipher().doFinal(plaintext.getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        return new byte[0];
    }

    private AlgorithmParameterSpec getSaltSpec() {
        final int iteration = 1000;
        return new PBEParameterSpec(getSalt(), iteration);
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    Cipher getDecryptCipher() {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), getSaltSpec());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        return cipher;
    }

    public String decrypt(byte[] cipher) {
        try {
            return new String(getDecryptCipher().doFinal(cipher));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] getSalt() {
        //TODO: the salt should be random and stored with the password
        byte[] SALT = {
                (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
                (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
        };
        return SALT;
    }


}
