package net.bytle.crypto;

import net.bytle.type.Arrayss;
import net.bytle.type.Bytes;
import org.apache.commons.codec.binary.Base64;

import java.util.Arrays;

/**
 * This class implements the encryption and decryption of sensitive data
 * It shows case the use of two transformation but only the first one {@link AdvancedEncryptionStandardTransformation}
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

    // Storage Version / Protector version management
    // That define for how the data (salt, digest and ciphertext) are stored
    // There is for now only one storage option but for compatibility issue we encode it
    private final Integer ProtectorStorageVersion = 1;


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
                    .encrypt(plaintext, passphrase, cipherSalt);

            // The secret key for the hmac digest is the same than for AES
            byte[] secretKey = cipherTwoWay.getKey();

            // Always 32 byte
            byte[] hmacDigest = HmacTransformation.get(secretKey).encrypt(plaintext);

            // Encoding the transformation implementation (One bytes = 255 possible implementation)
            final byte[] cipherCodeArray = new byte[]{this.transformationCode.byteValue()};

            String storage = null;
            // This byte gives the storage implementation, there is for now only one
            // But we show here how it works
            // For instance, we could put the Transformation code at the end and switch the salt and the digest
            // The protector version byte should always be first
            if (ProtectorStorageVersion == 1) {
                final byte[] protectorStorageVersionArray = new byte[]{ProtectorStorageVersion.byteValue()};
                storage = base64.encodeAsString(Arrayss.concatAll(protectorStorageVersionArray, cipherCodeArray, cipherSalt, hmacDigest, cipherText));
            }
            return storage;
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        } else {
            // Debase64
            byte[] cipherTextBytes = base64.decode(ciphertext);

            // Parse the string
            final int boundary0 = Protector.protectorVersionLength;
            byte[] protectorVersion = Arrays.copyOfRange(cipherTextBytes, 0, boundary0);
            final int protectorStorageVersion = Byte.toUnsignedInt(protectorVersion[0]);
            if (protectorStorageVersion == this.ProtectorStorageVersion) {

                final int boundary1 = boundary0 + Protector.transformationCodeLength;
                byte[] codeByte = Arrays.copyOfRange(cipherTextBytes, boundary0, boundary1);
                TransformationTwoWay cipherTwoWay = getCipher(Byte.toUnsignedInt(codeByte[0]));

                final int boundary2 = boundary1 + cipherTwoWay.getSaltLength();
                byte[] saltBytes = Arrays.copyOfRange(cipherTextBytes, boundary1, boundary2);
                final int boundary3 = boundary2 + hmacDigestLength;
                byte[] hmacBytes = Arrays.copyOfRange(cipherTextBytes, boundary2, boundary3);
                final int boundary4 = cipherTextBytes.length;
                byte[] cipherBytes = Arrays.copyOfRange(cipherTextBytes, boundary3, boundary4);

                // Data Verification
                final byte[] plaintext = cipherTwoWay
                        .decrypt(cipherBytes, passphrase, saltBytes);
                byte[] keyBytes = cipherTwoWay.getKey();

                byte[] hmacDigest = HmacTransformation.get(keyBytes).encrypt(plaintext);
                if (!(Bytes.equals(hmacDigest, hmacBytes))) {
                    throw new RuntimeException("Integrity error");
                }
                return Cryptos.toString(plaintext);

            } else {
                throw new RuntimeException("Bad version");
            }
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