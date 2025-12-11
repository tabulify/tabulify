package com.tabulify.crypto;

import org.junit.Assert;
import org.junit.Test;

public class CryptoSymmetricCipherTest {

  @Test
  public void aesTest() {


    final String plaintext = "password";
    CryptoSymmetricCipher aes = CryptoSymmetricCipher.AES_CBC_PKCS5PADDING;
    String passphrase = "master";
    final String cipherText = aes.encrypt(passphrase, plaintext);

    System.out.println("Encrypted: " + cipherText);
    String actualPassword = aes.decrypt(passphrase, cipherText);
    Assert.assertEquals("password are equals", plaintext, actualPassword);

  }

  @Test
  public void aesLengthTest() {


    CryptoSymmetricCipher aes = CryptoSymmetricCipher.AES_CBC_PKCS5PADDING;
    String passphrase = "master";
    String cipherText = aes.encrypt(passphrase, "password");
    System.out.println("Encrypted: length: "+cipherText.length() + ", "+ cipherText);
    cipherText = aes.encrypt(passphrase, "62efcef091869782be46da9617345d3288da394ec4c31f7b9e075fef7822f850");
    System.out.println("Encrypted: length: "+cipherText.length() + ", "+ cipherText);

  }

  @Test
  public void pbeTest() {

    final String passphrase = "master";
    final String plaintext = "password";

    CryptoSymmetricCipher pbeWithMD5AndDES = CryptoSymmetricCipher.PBEWithMD5AndDES;


    final String cipherTextToStore = pbeWithMD5AndDES.encrypt(passphrase, plaintext);

    System.out.println("Encrypted: " + cipherTextToStore);
    String actualPassword = pbeWithMD5AndDES.decrypt(passphrase, cipherTextToStore);
    Assert.assertEquals("password are equals", plaintext, actualPassword);

  }


}
