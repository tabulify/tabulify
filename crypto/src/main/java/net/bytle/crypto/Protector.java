package net.bytle.crypto;

import org.apache.commons.codec.binary.Base64;

public class Protector {


    private final Base64 base64 = new Base64();



    public final static int PBE = 2;
    public final static int AES = 1;

    private final CipherI cipher;

    // A code saved alongside the encrypted
    private final Integer cipherCode;
    // A version
    private final Integer cipherVersion;

    /**
     * @param cipherCode
     */
    private Protector(Integer cipherCode) {
        this.cipherCode = cipherCode;
        switch (cipherCode) {
            case PBE:
                this.cipher = PasswordBasedEncryptionCipher
                        .get();
                break;
            case AES:
                this.cipher = AdvancedEncryptionStandardCipher
                        .get();
                break;
            default:
                throw new RuntimeException("Cipher (" + cipherCode + ") is not known");

        }
        this.cipherVersion = cipher.getVersion();
    }

    Protector setPassphrase(String passphrase) {
        cipher.setPassphrase(passphrase);
        return this;
    }

    public static Protector get(Integer cipher) {
        return new Protector(cipher);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        } else {
            final byte[] cipherText = cipher.encrypt(plaintext);
            final byte[] cipherSalt = cipher.getSalt();
            final String cipherRelease = cipherCode + "." + cipherVersion;
            final String cipherReleaseHex = base64.encodeAsString(cipherRelease.getBytes());
            final String cipherTotal = "";
            return base64.encodeAsString(cipherText);
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