package net.bytle.crypto;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * {@link SecretKeyFactory} wrapper
 */
public class Passphrase {


  private final String passphrase;
  private final CryptoSymmetricCipher cryptoTransformation;


  public Passphrase(CryptoSymmetricCipher cryptoTransformation, String passphrase) {
    assert passphrase != null : "A passphrase should not be null";
    this.cryptoTransformation = cryptoTransformation;
    this.passphrase = passphrase;

  }

  public static Passphrase create(CryptoSymmetricCipher cryptoTransformation, String passphrase) {
    return new Passphrase(cryptoTransformation, passphrase);
  }


  public byte[] toKey(byte[] salt) {
    try {
      if (salt == null) {
        throw new RuntimeException("The salt cannot be null");
      }

      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(this.cryptoTransformation.getKeyAlgorithm());

      /**
       * PBE = Password based encryption
       */
      Integer keySize = this.cryptoTransformation.getKeySize();
      KeySpec pbeKeySpec;
      if (keySize != null) {
        pbeKeySpec = new PBEKeySpec(
          passphrase.toCharArray(),
          salt,
          this.cryptoTransformation.getKeyIterations(),
          keySize
        );
      } else {
        pbeKeySpec = new PBEKeySpec(
          passphrase.toCharArray(),
          salt,
          this.cryptoTransformation.getKeyIterations()
        );
      }
      SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
      return secretKey.getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

}
