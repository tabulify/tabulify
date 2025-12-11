package com.tabulify.crypto;

import org.junit.Assert;
import org.junit.Test;

public class PassphraseTest {


  @Test
  public void passphraseAes() {

    CryptoSymmetricCipher aesCbcPkcs5padding = CryptoSymmetricCipher.AES_CBC_PKCS5PADDING;

    byte[] key = Passphrase.create(aesCbcPkcs5padding, "bla").toKey("salt".getBytes());
    Assert.assertEquals("Size should be equals", (long) aesCbcPkcs5padding.getKeySize(), key.length*8);

  }

}
