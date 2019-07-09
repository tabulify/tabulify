package net.bytle.crypto;

import javax.crypto.SecretKey;

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
public interface CipherAll {

    /**
     * A passphrase is by definition a string (char array)
     * If you want to create one from a byte[], use {@link Cryptos#toString(byte[])}
     * @param passphrase
     * @return a cipher for construction chaining
     */
    CipherAll setPassphrase(String passphrase);

    /**
     * To set a key (You would set a key or a passphrase but not both, if a key is given, a key is not generated from the passphrase)
     * @param key
     * @return
     */
    CipherAll setKey(byte[] key);

    /**
     * Used to produce another passphrase for the digest (hmac)
     * @return
     */
    byte[] getKey();

    byte[] encrypt(String plaintext);
    byte[] encrypt(byte[] plaintext);

    String decrypt(byte[] ciphertext);

    Integer getVersion();

}
