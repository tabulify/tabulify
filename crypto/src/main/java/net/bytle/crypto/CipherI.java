package net.bytle.crypto;

/**
 * https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
 */
public interface CipherI {

    CipherI setPassphrase(String passphrase);

    byte[] encrypt(String plaintext);

    String decrypt(byte[] ciphertext);

}
