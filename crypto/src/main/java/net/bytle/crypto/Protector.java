package net.bytle.crypto;

import net.bytle.type.Arrayss;
import net.bytle.type.Bytes;
import org.apache.commons.codec.binary.Base64;

import java.util.Arrays;
import java.util.List;

public class Protector {


    private final Base64 base64 = new Base64();



    public final static int PBE = 2;
    public final static int AES = 1;

    private final CipherTwoWay cipherTwoWay;

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
                this.cipherTwoWay = PasswordBasedEncryptionCipher
                        .get();
                break;
            case AES:
                this.cipherTwoWay = AdvancedEncryptionStandardCipher
                        .get();
                break;
            default:
                throw new RuntimeException("Cipher (" + cipherCode + ") is not known");

        }
        this.cipherVersion = cipherTwoWay.getVersion();
    }

    Protector setPassphrase(String passphrase) {

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
            final byte[] cipherText = cipherTwoWay
                    .setPassphrase(passphrase)
                    .encrypt(plaintext);

            // Always 16 byte
            final byte[] cipherSalt = cipherTwoWay.getSalt();
            // The secret key for the hmac digest is the same than for AES
            byte[] secretKey = cipherTwoWay.getKey();

            // Always 32 byte
            byte[] hmacDigest = HmacCipher.get().setKey(secretKey).encrypt(plaintext);

            // Always two bytes
            final byte[] cipherRelease = new byte[] { cipherCode.byteValue(), cipherVersion.byteValue()};

            // To go to string, we could have used: base64.encodeAsString(cipherText);
            //List<String> listStoredText = Arrays.asList(Bytes.toHexaDecimal(cipherSalt), Bytes.toHexaDecimal(hmacDigest), Bytes.toHexaDecimal(cipherText));
            // String storedText = String.join("\n",listStoredText);
            String storedText = base64.encodeAsString(Arrayss.concatAll(cipherRelease,cipherSalt,hmacDigest,cipherText));


            return storedText;
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        } else {
            return cipherTwoWay.decrypt(base64.decode(ciphertext));
        }
    }
}