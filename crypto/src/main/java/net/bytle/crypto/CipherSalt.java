package net.bytle.crypto;

/**
 *
 * Not all Cipher are working with a salt
 *
 **/
public interface CipherSalt extends CipherAll {

    byte[] getSalt();

}
