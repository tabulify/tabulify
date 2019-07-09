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

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * If you modify the encryption way please use the version to keep backward compatibility
 *
 * https://en.wikipedia.org/wiki/Advanced_Encryption_Standard
 *
 * Inspiration by:
 * ansible/parsing/vault/__init__.py
 *
 * See: _encrypt_cryptography (L1172) where you can find:
 *   * AES256,
 *   * modes CTR,
 *   * padding.PKCS7
 *   * Hmac SHA256 to combine salt, digest (key2) and data
 *
 * See: _create_key_cryptography for the key generation
 * kdf = PBKDF2HMAC(
 *  algorithm=hashes.SHA256(),
 *  length=2 * key_length + iv_length,
 *  salt=b_salt,
 *  iterations=10000,
 *  backend=CRYPTOGRAPHY_BACKEND)
 *
 * List of Cipher
 * https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
 */
public class AdvancedEncryptionStandardCipher implements CipherI {


    public static final String ALGORITHM = "AES";
    final int bit_block_size = 128;

    //
    //
    // AES supports the following key lengths: 128, 192 and 256 bits
    // but
    // in the below doc (https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html)
    // for the transformation (AES/CBC/PKCS5PADDING) it should be 128 ?
    //
    private final int key_size = 256;

    /**
     * A transformation is a string that describes the operation (or set of operations) to be performed on the given input, to produce some output.
     *
     * A transformation always includes:
     *   * the name of a cryptographic algorithm (e.g., AES)
     *   * and may be followed by:
     *     * a feedback mode
     *     * and padding scheme.
     *
     * A transformation is of the form:
     *    * "algorithm/mode/padding" or
     *    * "algorithm"
     */
    public static final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";

    // Salt is not  final because we need to set him for decryption
    private byte[] salt;

    private String passphrase;


    public static CipherI get() {
        return new AdvancedEncryptionStandardCipher();
    }

    @Override
    public CipherI setPassphrase(String passphrase) {
        this.passphrase = passphrase;
        return this;
    }

    @Override
    public byte[] encrypt(String plaintext) {

        try {

            SecretKeySpec key = getSecretKey();
            IvParameterSpec salt = new IvParameterSpec(this.getSalt());

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, salt);

            return cipher.doFinal(plaintext.getBytes(UTF_8));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    Cipher getDecryptCipher() {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(TRANSFORMATION);
            IvParameterSpec saltAsInitVector = new IvParameterSpec(this.getSalt());
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), saltAsInitVector);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        return cipher;
    }

    public String decrypt(byte[] cipher) {
        try {
            return new String(getDecryptCipher().doFinal(cipher),UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] getSalt() {
        // Init a salt
        // The salt (init vector) has the same length than the cipher block size
        // The salt should be stored closed to the password
        if (this.salt == null) {
            int iv_length_bytes = bit_block_size / 8;
            this.salt = Bytes.getRandomBytes(iv_length_bytes);
        }
        return this.salt;
    }

    @Override
    public Integer getVersion() {
        return 1;
    }


    /**
     * @return a key specification derived from the passphrase, the salt, the number of iterations and the length
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    SecretKeySpec getSecretKey() {
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            final int numberOfIterations = 65536; // 10000 for Ansible
            KeySpec pbeKeySpec = new PBEKeySpec(passphrase.toCharArray(), this.getSalt(), numberOfIterations, key_size);
            SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
            final SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), ALGORITHM);
            return secretKeySpec;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
