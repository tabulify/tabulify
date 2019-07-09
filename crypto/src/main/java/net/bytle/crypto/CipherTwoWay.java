package net.bytle.crypto;

/**
 *
 * Not all Cipher are working with a salt
 *
 **/
public interface CipherTwoWay extends CipherAll {

    byte[] getSalt();

    String decrypt(byte[] ciphertext);

    CipherTwoWay setPassphrase(String passphrase);
    CipherTwoWay setKey(byte[] key);
}
