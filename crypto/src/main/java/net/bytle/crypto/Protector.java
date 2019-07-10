package net.bytle.crypto;

import net.bytle.type.Arrayss;
import net.bytle.type.Bytes;
import org.apache.commons.codec.binary.Base64;

import java.util.Arrays;

/**
 * This class implements the encryption and decryption of sensitive data
 * It shows case the use of two transformation but only the first one {@link AdvancedEncryptionStandardTransformation}
 * is public
 *
 *
 * Salt:
 *   * is used for:
 *     * the generation of the key
 *     * for the initialization of the cipher
 *   * length is cipher (transformation) dependent
 *
 */
public class Protector {


    /**
     * Length used in the parsing of the ciphertext
     */
    private static final int transformationCodeLength = 1;
    private static final int hmacDigestLength = 32;
    private static final int protectorVersionLength = 1;

    private final Base64 base64 = new Base64();


    /**
     * A unique code representing an transformation (ie cipher + mode + padding)
     * Only AES is for now public
     */
    public final static int AES_CODE = AdvancedEncryptionStandardTransformation.CODE;
    protected final static int PBE_CODE = PasswordBasedEncryptionTransformation.CODE;
    private Integer transformationCode = AES_CODE;

    // Version management
    private final Integer ProtectorVersion = 1;


    private String passphrase;



    /**
     * @param passphrase
     */
    private Protector(String passphrase) {
        this.passphrase = passphrase;

    }


    public static Protector get(String passphrase) {
        return new Protector(passphrase);
    }


    /**
     * @param plaintext
     * @return a ciphertext with the following structure
     * * first bit: the transformation code (default to 1 ie AES if public)
     * * second bit: the version (There is for now only one version but this if for backward compatibility)
     * * next 16 byte: the salt
     * * next 32 bit: the hmax digest
     * * the rest: the ciphertext
     * We can see that the first two bit stay the same. We didn't put them between the salt and the digest
     * to not show their length.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        } else {
            final TransformationTwoWay cipherTwoWay = getCipher(this.transformationCode);

            byte[] cipherSalt = Bytes.getRandomBytes(cipherTwoWay.getSaltLength());

            final byte[] cipherText = cipherTwoWay
                    .encrypt(plaintext,passphrase,cipherSalt);

            // The secret key for the hmac digest is the same than for AES
            byte[] secretKey = cipherTwoWay.getKey();

            // Always 32 byte
            byte[] hmacDigest = HmacTransformation.get(secretKey).encrypt(plaintext);

            // One bytes
            final byte[] cipherCodeArray = new byte[]{this.transformationCode.byteValue()};
            final byte[] cipherVersionArray = new byte[]{ProtectorVersion.byteValue()};

            return base64.encodeAsString(Arrayss.concatAll(cipherCodeArray, cipherVersionArray, cipherSalt, hmacDigest, cipherText));
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        } else {
            byte[] cipherTextBytes = base64.decode(ciphertext);
            final int boundary0 = Protector.transformationCodeLength;
            byte[] transformationCodeBytes = Arrays.copyOfRange(cipherTextBytes, 0, boundary0);
            final byte codeByte = transformationCodeBytes[0];
            TransformationTwoWay cipherTwoWay = getCipher(Byte.toUnsignedInt(codeByte));

            final int boundary1 = boundary0 + protectorVersionLength;
            byte[] protectorVersionBytes = Arrays.copyOfRange(cipherTextBytes, boundary0, boundary1);

            final int boundary2 = boundary1 + cipherTwoWay.getSaltLength();
            byte[] saltBytes = Arrays.copyOfRange(cipherTextBytes, boundary1, boundary2);
            final int boundary3 = boundary2 + hmacDigestLength;
            byte[] hmacBytes = Arrays.copyOfRange(cipherTextBytes, boundary2, boundary3);
            final int boundary4 = cipherTextBytes.length;
            byte[] cipherBytes = Arrays.copyOfRange(cipherTextBytes, boundary3, boundary4);



            if (protectorVersionBytes[0] != ProtectorVersion.byteValue()) {
                throw new RuntimeException("Bad version");
            }
            // Data Verification
            final byte[] plaintext = cipherTwoWay
                    .decrypt(cipherBytes,passphrase,saltBytes);
            byte[] keyBytes = cipherTwoWay.getKey();

            byte[] hmacDigest = HmacTransformation.get(keyBytes).encrypt(plaintext);
            if (!(Bytes.equals(hmacDigest, hmacBytes))) {
                throw new RuntimeException("Integrity error");
            }
            return Cryptos.toString(plaintext);
        }
    }

    private TransformationTwoWay getCipher(int cipherCode) {
        /**
         * TODO: The link is now done in the code, they could register with the function {@link TransformationAll#getCode()}
         */
        switch (cipherCode) {
            case AES_CODE:
                return AdvancedEncryptionStandardTransformation.get();
            case PBE_CODE:
                return PasswordBasedEncryptionTransformation.get();
            default:
                throw new RuntimeException("Unknown cipher");
        }

    }


    public Protector setTransformation(int code) {
        this.transformationCode = code;
        return this;
    }
}