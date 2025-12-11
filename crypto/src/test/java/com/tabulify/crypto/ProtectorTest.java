package com.tabulify.crypto;

import org.junit.Assert;
import org.junit.Test;

public class ProtectorTest {


    @Test
    public void protectorDefaultTest() {
        final String passphrase = "master";
        final String plaintext = "password";
        String cipherText = Protector
                .create(passphrase)
                .encrypt(CryptoSymmetricCipher.PBEWithMD5AndDES,plaintext);
        System.out.println("Encrypted: "+cipherText);

        // Get a new instance
        String plaintextDecoded  =
                Protector.create(passphrase)
                .decrypt(cipherText);
        System.out.println("Decrypted: "+plaintextDecoded);
        Assert.assertEquals("plaintext are equals",plaintext,plaintextDecoded);

    }

    @Test
    public void protectorAesTest() {
        final String passphrase = "master";
        final String plaintext = "password";
        String cipherText = Protector
                .create(passphrase)
                .encrypt(CryptoSymmetricCipher.AES_CBC_PKCS5PADDING,plaintext);
        System.out.println("Encrypted: "+cipherText);

        // Get a new instance
        String plaintextDecoded  =
                Protector.create(passphrase)
                        .decrypt(cipherText);
        System.out.println("Decrypted: "+plaintextDecoded);
        Assert.assertEquals("plaintext are equals",plaintext,plaintextDecoded);

    }

    @Test
    public void protectorPbeTest() {
        final String passphrase = "master";
        final String plaintext = "password";
        String cipherText = Protector
                .create(passphrase)
                .encrypt(CryptoSymmetricCipher.PBEWithMD5AndDES,plaintext);
        System.out.println("Encrypted: "+cipherText);

        // Get a new instance
        String plaintextDecoded  =
                Protector.create(passphrase)
                        .decrypt(cipherText);
        System.out.println("Decrypted: "+plaintextDecoded);
        Assert.assertEquals("plaintext are equals",plaintext,plaintextDecoded);

    }


}
