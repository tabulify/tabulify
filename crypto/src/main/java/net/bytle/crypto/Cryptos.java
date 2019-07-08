package net.bytle.crypto;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class Cryptos {


    static SecretKeySpec generateKeyFromPassphrase(String passphrase, int keyLength, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Create a secret key from a passphrase
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        final int numberOfIterations = 10000; // same as Ansible
        KeySpec pbeKeySpec = new PBEKeySpec(passphrase.toCharArray(), salt, numberOfIterations, keyLength);
        SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }
}
