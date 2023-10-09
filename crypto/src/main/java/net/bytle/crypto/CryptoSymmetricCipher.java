package net.bytle.crypto;

import net.bytle.type.Base64Utility;
import net.bytle.type.Bytes;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of Symmetric Cipher
 */
public enum CryptoSymmetricCipher {

  AES_CBC_PKCS5PADDING(
    1,
    "AES/CBC/PKCS5PADDING",
    "AES",
    "iv",
    "PBKDF2WithHmacSHA256",
    65536,
    256, // bit (not byte)
    128 / 8 // Block size / 8
  ),
  /**
   * Don't use too weak
   */
  PBEWithMD5AndDES(
    2,
    "PBEWithMD5AndDES",
    "PBEWithMD5AndDES",
    "pbe",
    "PBEWithMD5AndDES",
    1000,
    null,
    8 // Fixed (bytes)
  );


  public static final String BASE64_SEP = ">";

  /**
   * A unique int value used in the stored text
   * to be able to decrypt
   */
  private final int transformationId;

  /**
   * A transformation is a string that describes the operation (or set of operations) to be performed on the given input, to produce some output.
   * <p>
   * A transformation always includes:
   * * the name of a cryptographic algorithm (e.g., AES)
   * * and may be followed by:
   * * a feedback mode
   * * and padding scheme.
   * <p>
   * A transformation is of the form:
   * * "algorithm/mode/padding" or
   * * "algorithm"
   * <p>
   * https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
   *
   * The string of {@link Cipher#getInstance(String)}
   */
  private final String transformationString;

  /**
   * The string of the {@link SecretKeySpec} constructo
   */
  private final String transformationAlgorithm;
  /**
   * The algo used in {@link javax.crypto.SecretKeyFactory#getInstance(String)}
   */
  private final String keyAlgo;

  /**
   * Number of iterations when creating the key from the passphrase
   */
  private final Integer keyIterations;

  /**
   * The size of the key
   */
  private final Integer keySize;

  /**
   * The size of the key
   */
  private final Integer saltLength;

  /**
   * The type of parameter to pass to the {@link Cipher#init(int, Key, AlgorithmParameterSpec)}
   */
  private final String parameterSpec;


  CryptoSymmetricCipher(
    int transformationId,
    String transformationString,
    String transformationAlgorithm,
    String parameterSpec,
    String keyAlgorithm,
    Integer keyIterations,
    Integer keySize,
    Integer saltLength
  ) {
    this.transformationId = transformationId;
    this.transformationString = transformationString;
    this.transformationAlgorithm = transformationAlgorithm;
    this.parameterSpec = parameterSpec;
    this.keyAlgo = keyAlgorithm;
    this.keyIterations = keyIterations;
    this.keySize = keySize;
    this.saltLength = saltLength;
  }

  public String getTransformationString() {
    return this.transformationString;
  }

  public Integer getTransformationId() {
    return this.transformationId;
  }

  public String getKeyAlgorithm() {
    return this.keyAlgo;
  }

  // 10000 for Ansible
  public Integer getKeyIterations() {
    return this.keyIterations;
  }

  public Integer getKeySize() {
    return this.keySize;
  }



  public Integer getSaltLength() {
    return this.saltLength;
  }

  public String getTransformationAlgorithm() {
    return this.transformationAlgorithm;
  }

  public String getParameterSpec() {
    return this.parameterSpec;
  }

  public String encrypt(String passphrase, String plaintext) {

    //
    //
    // AES supports the following key lengths: 128, 192 and 256 bits
    // but
    // in the below doc (https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html)
    // for the transformation (AES/CBC/PKCS5PADDING) it should be 128 ?
    //
    final List<Integer> keyLengthsSupported = Arrays.asList(128, 192, 256);
    if (getTransformationString().startsWith("AES") && !keyLengthsSupported.contains(getKeySize())) {
      throw new IllegalStateException("The key size is not supported");
    }

    Integer saltLength = getSaltLength();
    byte[] cipherSalt = Bytes.getRandomBytes(saltLength);

    // The secret key for the hmac digest is the same as for AES
    byte[] key = Passphrase
      .create(this, passphrase)
      .toKey(cipherSalt);

    byte[] cipherText;
    try {

      SecretKey secretKey = new SecretKeySpec(key, getTransformationAlgorithm());
      AlgorithmParameterSpec algorithmParameterSpec = getAlgorithmParameterSpec(cipherSalt);

      Cipher cipher = Cipher.getInstance(getTransformationString());
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, algorithmParameterSpec);

      cipherText = cipher.doFinal(plaintext.getBytes());

    } catch (Exception e) {

      throw new RuntimeException(e);

    }

    // Hmac to verify the integrity - It's hash that outputs always 32 byte
    byte[] hmacDigest = CryptoHmac.create(key).encrypt(plaintext);


    return Base64Utility.bytesToBase64String(cipherSalt) + BASE64_SEP +
      Base64Utility.bytesToBase64String(hmacDigest) + BASE64_SEP +
      Base64Utility.bytesToBase64String(cipherText);


  }


  public String decrypt(String passphrase, String storedCipherText) {

    String[] blocks = storedCipherText.split(BASE64_SEP);
    byte[] saltBytes = Base64Utility.base64StringToByte(blocks[0]);
    byte[] hmacBytes = Base64Utility.base64StringToByte(blocks[1]);
    byte[] cipherText = Base64Utility.base64StringToByte(blocks[2]);

    byte[] keyBytes = Passphrase.create(this,passphrase)
      .toKey(saltBytes);

    final SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, getTransformationAlgorithm());
    AlgorithmParameterSpec algorithmParameterSpec = getAlgorithmParameterSpec(saltBytes);

    String plaintext;
    try {
      Cipher cipher = Cipher.getInstance(getTransformationString());
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, algorithmParameterSpec);
      plaintext = Cryptos.toString(cipher.doFinal(cipherText));
    } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }

    /**
     * Integrity check
     */
    byte[] hmacDigest = CryptoHmac.create(keyBytes).encrypt(plaintext);
    if (!(Bytes.equals(hmacDigest, hmacBytes))) {
      throw new RuntimeException("Data Integrity error");
    }

    return plaintext;
  }

  private AlgorithmParameterSpec getAlgorithmParameterSpec(byte[] saltBytes) {
    AlgorithmParameterSpec algorithmParameterSpec;
    if (getParameterSpec().equals("iv")) {
      algorithmParameterSpec = new IvParameterSpec(saltBytes);
    } else if (getParameterSpec().equals("pbe"))  {
      algorithmParameterSpec = new PBEParameterSpec(saltBytes, getKeyIterations());
    } else {
      throw new RuntimeException("Parameter spec is unknown");
    }
    return algorithmParameterSpec;
  }

  static CryptoSymmetricCipher getFrom(int transformationId) {
    CryptoSymmetricCipher[] values = CryptoSymmetricCipher.values();
    for (CryptoSymmetricCipher type : values) {
      if (type.getTransformationId() == transformationId) {
        return type;
      }
    }
    throw new RuntimeException("Bad transformation id");
  }

}
