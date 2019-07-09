package net.bytle.crypto;

import org.apache.commons.codec.binary.Base64;

public class Protector {


    private final Base64 base64 = new Base64();



    public final static int PBE = 2;
    public final static int AES = 1;

    private final CipherTwoWay saltedCipher;

    // A code saved alongside the encrypted
    private final Integer cipherCode;
    // A version
    private final Integer cipherVersion;
    private String passphrase;

    /**
     * @param cipherCode
     */
    private Protector(Integer cipherCode) {
        this.cipherCode = cipherCode;
        switch (cipherCode) {
            case PBE:
                this.saltedCipher = PasswordBasedEncryptionCipher
                        .get();
                break;
            case AES:
                this.saltedCipher = AdvancedEncryptionStandardCipher
                        .get();
                break;
            default:
                throw new RuntimeException("Cipher (" + cipherCode + ") is not known");

        }
        this.cipherVersion = saltedCipher.getVersion();
    }

    Protector setPassphrase(String passphrase) {
        saltedCipher.setPassphrase(passphrase);
        this.passphrase = passphrase;
        return this;
    }

    public static Protector get(Integer cipher) {
        return new Protector(cipher);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        } else {
            final byte[] cipherText = saltedCipher.encrypt(plaintext);
            final byte[] cipherSalt = saltedCipher.getSalt();
            final String cipherRelease = cipherCode + "." + cipherVersion;
            byte[] secretKey = saltedCipher.getKey();
            HmacCipher.get()
                    .setKey(secretKey)
                    .encrypt(cipherText);

            return base64.encodeAsString(cipherText);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        } else {
            return saltedCipher.decrypt(base64.decode(ciphertext));
        }
    }
}