package com.tabulify.crypto;


import com.tabulify.crypto.util.Base64Util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class implements the encryption and decryption of sensitive data
 * <p>
 * is public
 * <p>
 * <p>
 * Salt:
 * * is used for:
 * * the generation of the key
 * * for the initialization of the cipher
 * * length is cipher (transformation) dependent
 */
public class Protector {


  public static final String SEPARATOR = ">";
  public static final Charset UTF_8 = StandardCharsets.UTF_8;


  private final String passphrase;


  /**
   * @param passphrase the passphrase
   */
  private Protector(String passphrase) {

    assert passphrase != null;
    this.passphrase = passphrase;

  }


  public static Protector create(String passphrase) {

    return new Protector(passphrase);

  }


  /**
   * @param plaintext the plain text
   * @return a ciphertext with the following structure
   */
  public String encrypt(CryptoSymmetricCipher typeTransformationEnum, String plaintext) {

    if (plaintext == null) {
      return null;
    }

    String storedCryptoMaterial = typeTransformationEnum.encrypt(passphrase, plaintext);

    // Encoding the transformation implementation (One bytes = 255 possible implementation)
    // This byte gives the transformation version, there is for now only one
    // The transformation version byte should always be first
    byte[] transformationId = {typeTransformationEnum.getTransformationId().byteValue()};

    String base64Concat = Base64Util.toBase64(transformationId) + SEPARATOR + storedCryptoMaterial;
    /**
     * Two times to delete the SEPARATOR
     */
    return Base64Util.toBase64(base64Concat.getBytes(UTF_8));

  }

  public String decrypt(String storedEncryptedText) {

    return decrypt(storedEncryptedText, this.passphrase);

  }

  private String decrypt(String storedEncryptedText, String passphrase) {

    if (storedEncryptedText == null) {
      return null;
    }

    byte[] firstBytes = Base64Util.toBytesFromBase64(storedEncryptedText);
    String base64ConcatString = new String(firstBytes, 0, firstBytes.length, UTF_8);
    String[] components = base64ConcatString.split(SEPARATOR, 2);

    // First byte is the version of the crypto transformation
    final int protectorStorageVersion = Byte.toUnsignedInt(Base64Util.toBytesFromBase64(components[0])[0]);
    CryptoSymmetricCipher typeTransformationEnum = CryptoSymmetricCipher.getFrom(protectorStorageVersion);

    return typeTransformationEnum.decrypt(passphrase, components[1]);

  }


}
