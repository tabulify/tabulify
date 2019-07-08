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
 * https://en.wikipedia.org/wiki/Advanced_Encryption_Standard
 */
public class AdvancedEncryptionStandardCipher implements CipherI {


    final int bit_block_size = 128;
    // key lengths: 128, 192 and 256 bits.
    final int key_length = 256;

    public static final String ALGORITHM = "AES/CBC/PKCS5PADDING";
    private final byte[] salt;

    private String passphrase;

    public AdvancedEncryptionStandardCipher() {

        // Init a salt
        // The salt (init vector) has the same length than the cipher block size
        // TODO: the salt should be stored closed to the password
        int iv_length_bytes = bit_block_size / 8;
        salt =  Bytes.getRandomBytes(iv_length_bytes);

    }

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
            IvParameterSpec salt = new IvParameterSpec(this.salt);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, salt);

            return cipher.doFinal(plaintext.getBytes());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    Cipher getDecryptCipher() {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            IvParameterSpec saltAsInitVector = new IvParameterSpec(this.salt);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), saltAsInitVector);
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


    /**
     * @return a key specification derived from the passphrase, the salt, the number of iterations and the length
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    SecretKeySpec getSecretKey() {
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            final int numberOfIterations = 10000; // same as Ansible
            KeySpec pbeKeySpec = new PBEKeySpec(passphrase.toCharArray(), this.salt, numberOfIterations, key_length);
            SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
            return new SecretKeySpec(secretKey.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
