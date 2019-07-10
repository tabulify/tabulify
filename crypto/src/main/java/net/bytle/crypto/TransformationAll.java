package net.bytle.crypto;

/**
 *
 * https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
 *
 * // Other type getInstance
 *     // bouncycastle library
 *     // https://bouncycastle.org/
 *     // Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5Padding", "BC");
 *
 *
 * KerbTicket Encryption Type: AES-256-CTS-HMAC-SHA1-96 ie:
 *   * Algo: AES-256: AES using 256-bit key
 *   * Mode: CTS (ciphertext stealing)
 *   * HMAC-SHA1-96 = HMAC using SHA-1 hash function with mac truncated to 96 bits
 */
public interface TransformationAll {


    /**
     * Used to produce another passphrase for the digest (hmac)
     * @return
     */
    byte[] getKey();



    /**
     * A unique id for the transformation
     */
    Integer getCode();

}
