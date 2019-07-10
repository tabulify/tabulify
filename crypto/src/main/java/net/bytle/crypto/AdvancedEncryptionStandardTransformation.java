package net.bytle.crypto;

import net.bytle.type.Bytes;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * This is a block Cipher
 * <p>
 * If you modify the encryption way please use the version to keep backward compatibility
 * <p>
 * https://en.wikipedia.org/wiki/Advanced_Encryption_Standard
 * <p>
 * Inspiration by:
 * ansible/parsing/vault/__init__.py
 * <p>
 * See: _encrypt_cryptography (L1172) where you can find:
 * * AES256,
 * * modes CTR,
 * * padding.PKCS7
 * * Hmac SHA256 to combine salt, digest (key2) and data
 * <p>
 * See: _create_key_cryptography for the key generation
 * kdf = PBKDF2HMAC(
 * algorithm=hashes.SHA256(),
 * length=2 * key_length + iv_length,
 * salt=b_salt,
 * iterations=10000,
 * backend=CRYPTOGRAPHY_BACKEND)
 * <p>
 * List of Cipher
 * https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
 */
public class AdvancedEncryptionStandardTransformation implements TransformationTwoWay {


    public static final String ALGORITHM = "AES";
    public static final int CODE = 1;

    final static int bit_block_size = 128;
    private final static int salt_length_bytes = bit_block_size / 8;

    //
    //
    // AES supports the following key lengths: 128, 192 and 256 bits
    // but
    // in the below doc (https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html)
    // for the transformation (AES/CBC/PKCS5PADDING) it should be 128 ?
    //
    private final static int key_size = 256;

    /**
     * A transformation is a string that describes the operation (or set of operations) to be performed on the given input, to produce some output.
     * <p>
     * A transformation always includes:
     * * the name of a cryptographic algorithm (e.g., AES)
     * * and may be followed by:
     * * a feedback mode
     * * and padding scheme.
     * <p>
     * A transformation is of the form:
     * * "algorithm/mode/padding" or
     * * "algorithm"
     */
    public static final String TRANSFORMATION = ALGORITHM + "/CBC/PKCS5PADDING";


    private byte[] key;

    public static AdvancedEncryptionStandardTransformation get() {
        return new AdvancedEncryptionStandardTransformation();
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

    @Override
    public byte[] encrypt(String plaintext, String passphrase, byte[] salt) {
        return encrypt(Cryptos.toBytes(plaintext), passphrase, salt);
    }

    @Override
    public byte[] encrypt(byte[] plaintext, String passphrase, byte[] salt) {

        byte[] key = passphraseToKey(passphrase, salt);
        return encrypt(plaintext, key, salt);

    }

    private byte[] passphraseToKey(String passphrase, byte[] salt) {
        try {
            if (salt == null) {
                throw new RuntimeException("The salt cannot be null");
            }
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            final int numberOfIterations = 65536; // 10000 for Ansible
            KeySpec pbeKeySpec = new PBEKeySpec(passphrase.toCharArray(), salt, numberOfIterations, key_size);
            SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
            byte[] key = secretKey.getEncoded();
            setKey(key);
            return key;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] encrypt(String plaintext, byte[] key, byte[] salt) {

        return encrypt(Cryptos.toBytes(plaintext), key, salt);

    }

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] salt) {
        try {

            SecretKey secretKey = new SecretKeySpec(key,ALGORITHM);
            IvParameterSpec saltSpec = new IvParameterSpec(salt);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, saltSpec);

            return cipher.doFinal(plaintext);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A function to be sure that we are not setting several different key
     *
     * @param key
     */
    private void setKey(byte[] key) {
        if (this.key != null) {
            if (!(Bytes.equals(key, this.key))) {
                throw new RuntimeException("Error: A different key value was already set");
            }
        }
        this.key = key;
    }


    @Override
    public byte[] decrypt(byte[] cipherText, String passphrase, byte[] salt) {
        final byte[] key = passphraseToKey(passphrase, salt);
        return decrypt(cipherText, key, salt);

    }

    @Override
    public byte[] decrypt(byte[] cipherText, byte[] key, byte[] salt) {
        try {

            final SecretKeySpec secretKeySpec = new SecretKeySpec(key,ALGORITHM);
            IvParameterSpec saltSpec = new IvParameterSpec(salt);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, saltSpec);

            return cipher.doFinal(cipherText);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getSaltLength() {
        return salt_length_bytes;
    }


    @Override
    public Integer getCode() {
        return CODE;
    }


}
