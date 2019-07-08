package net.bytle.crypto;

import org.apache.commons.codec.binary.Base64;

public class Protector {


    private final Base64 base64 = new Base64();

    // Other type getInstance
    // bouncycastle library
    // https://bouncycastle.org/
    // Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5Padding", "BC");

    // Note
    // KerbTicket Encryption Type: AES-256-CTS-HMAC-SHA1-96 - AES in CTR (Counter) mode, and append an HMAC


    public final static String PBE = "PBEWithMD5AndDES";
    public final static String AES = "AES/CBC/PKCS5PADDING";

    private final CipherI cipher;

    /**
     * @param cipher
     */
    private Protector(String cipher) {

        switch (cipher) {
            case PBE:
                this.cipher = PasswordBasedEncryptionCipher
                        .get();
                break;
            case AES:
                this.cipher = AdvancedEncryptionStandardCipher
                        .get();
                break;
            default:
                throw new RuntimeException("Cipher (" + cipher + ") is not known");

        }
    }

    Protector setPassphrase(String passphrase) {
        cipher.setPassphrase(passphrase);
        return this;
    }

    public static Protector get(String cipher) {
        return new Protector(cipher);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        } else {
            return base64.encodeAsString(cipher.encrypt(plaintext));
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        } else {
            return cipher.decrypt(base64.decode(ciphertext));
        }
    }
}