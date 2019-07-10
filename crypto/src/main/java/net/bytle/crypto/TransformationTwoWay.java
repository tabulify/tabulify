package net.bytle.crypto;

/**
 *
 * Not all Cipher are working with a salt
 *
 **/
public interface TransformationTwoWay extends TransformationAll {

    byte[] decrypt(byte[] ciphertext, String passphrase, byte[] salt);
    byte[] decrypt(byte[] ciphertext, byte[] key, byte[] salt);

    int getSaltLength();

    // The child function
    byte[] encrypt(String plaintext, String passphrase, byte[] salt);
    byte[] encrypt(String plaintext, byte[] key, byte[] salt);

    // The base function
    byte[] encrypt(byte[] plaintext, String passphrase, byte[] salt);
    byte[] encrypt(byte[] plaintext, byte[] key, byte[] salt);

}
