package net.bytle.crypto;

import net.bytle.type.Arrayss;
import net.bytle.type.Bytes;
import org.apache.commons.codec.binary.Base64;

import java.util.Arrays;
import java.util.List;

public class Protector {


    public static final int saltLength = 16;
    public static final int codeLength = 1;
    public static final int digestLength = 32;
    private final Base64 base64 = new Base64();



    public final static int PBE = 2;
    public final static int AES = 1;

    private final CipherTwoWay cipherTwoWay;

    // A code saved alongside the encrypted
    private final Integer cipherCode;
    // A version
    private final Integer cipherVersion;
    private String passphrase;
    private int versionLength = 1;

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

            // One bytes
            final byte[] cipherCodeArray = new byte[] { cipherCode.byteValue() };
            final byte[] cipherVersionArray  = new byte[] { cipherVersion.byteValue()};

            return base64.encodeAsString(Arrayss.concatAll(cipherSalt,cipherCodeArray, hmacDigest, cipherVersionArray, cipherText));
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        } else {
            byte[] cipherTextBytes = base64.decode(ciphertext);
            final int boundary0 = Protector.saltLength;
            byte[] saltBytes = Arrays.copyOfRange(cipherTextBytes,0, boundary0);
            final int boundary1 = boundary0 + codeLength;
            byte[] codeBytes = Arrays.copyOfRange(cipherTextBytes, boundary0, boundary1);
            final int boundary2 = boundary1+digestLength;
            byte[] hmacBytes = Arrays.copyOfRange(cipherTextBytes, boundary1, boundary2);
            final int boundary3 = boundary2+versionLength;
            byte[] versionBytes = Arrays.copyOfRange(cipherTextBytes,boundary2,boundary3);
            final int boundary4 = cipherTextBytes.length;
            byte[] cipherBytes = Arrays.copyOfRange(cipherTextBytes, boundary3,boundary4);

            final byte codeByte = codeBytes[0];
            if (cipherCode.byteValue()!=codeByte){
                throw new RuntimeException("Bad code");
            }
            if (versionBytes[0]!=cipherVersion.byteValue()){
                throw new RuntimeException("Bad version");
            }
            // Data Verification
            final String plaintext = cipherTwoWay
                    .setPassphrase(passphrase)
                    .setSalt(saltBytes)
                    .decrypt(cipherBytes);
            byte[] keyBytes = cipherTwoWay.getKey();
            byte[] hmacDigest = HmacCipher.get().setKey(keyBytes).encrypt(plaintext);
            if (!(Bytes.equals(hmacDigest,hmacBytes))){
                throw new RuntimeException("Integrity error");
            }
            return plaintext;
        }
    }
}