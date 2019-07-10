package net.bytle.crypto;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;


public class PasswordBasedEncryptionTransformation implements TransformationTwoWay {


    private final static String ALGORITHM = "PBEWithMD5AndDES";
    public static final int CODE = 2;


    /**
     * Should be 8
     */
    private static final int iv_length_bytes = 8;

    /**
     * Just to return the key if it's generated
     */
    private byte[] key;


    public static PasswordBasedEncryptionTransformation get() {
        return new PasswordBasedEncryptionTransformation();
    }





    @Override
    public byte[] encrypt(byte[] plaintext, String passphrase, byte[] salt) {

        try {
            byte[] key = passphraseToKey(passphrase);
            return encrypt(plaintext,key,salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

    }

    private byte[] passphraseToKey(String passphrase) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
        SecretKey secretKey = secretKeyFactory.generateSecret(new PBEKeySpec(passphrase.toCharArray()));
        byte[] key = secretKey.getEncoded();
        setKey(key);
        return key;
    }

    private void setKey(byte[] key) {
        if (key !=null){
            this.key = key;
        } else {
            throw new RuntimeException("The key was already set");
        }
    }

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] salt) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            Key secretKey = new SecretKeySpec(key, ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, getSaltSpec(salt));
            return cipher.doFinal(plaintext);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    private AlgorithmParameterSpec getSaltSpec(byte[] salt) {
        final int iteration = 1000;
        return new PBEParameterSpec(salt, iteration);
    }

    /**
     * Used to produce another passphrase for the digest (hmac)
     *
     * @return
     */
    @Override
    public byte[] getKey() {
        return key;
    }

    @Override
    public Integer getCode() {
        return CODE;
    }


    @Override
    public byte[] decrypt(byte[] ciphertext, String passphrase, byte[] salt) {
        return decrypt(ciphertext, Cryptos.toBytes(passphrase),salt);
    }

    public byte[] decrypt(byte[] cipherText, byte[] key, byte[] salt) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            Key secretKey = new SecretKeySpec(key, ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, getSaltSpec(salt));
            return cipher.doFinal(cipherText);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getSaltLength() {
        return iv_length_bytes;
    }

    @Override
    public byte[] encrypt(String plaintext, String passphrase, byte[] salt) {
        return encrypt(Cryptos.toBytes(plaintext),passphrase,salt);
    }

    @Override
    public byte[] encrypt(String plaintext, byte[] key, byte[] salt) {
        return encrypt(Cryptos.toBytes(plaintext),key,salt);
    }


}
