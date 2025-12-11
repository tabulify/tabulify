package com.tabulify.crypto;


import com.tabulify.crypto.util.HexaUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class to create digest hash (MD5, SHA, ...)
 */
public class Digest {

    public enum Algorithm {
        MD5("MD5"),
        SHA384("SHA-384"),
        SHA256("SHA-256");

        private final String name;

        Algorithm(String name) {
            this.name = name;
        }

        public static Algorithm createFrom(String algorithm) {
            String normalizedAlgorithmName = algorithm.replace("-", "").toLowerCase();
            for (Algorithm algo : values()) {
                if (algo.toString().toLowerCase().equals(normalizedAlgorithmName)) {
                    return algo;
                }
            }
            throw new RuntimeException("The algorithm (" + algorithm + ") is unknown");
        }
    }

    private final byte[] bytes;

    public Digest(byte[] bytes) {
        this.bytes = bytes;
    }

    public static Digest createFromBytes(Algorithm algorithm, byte[] bytes) {

        try {
            byte[] hashBytes = MessageDigest.getInstance(algorithm.name).digest(bytes);
            return new Digest(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Digest createFromString(Algorithm algorithm, String... s) {

        return createFromBytes(algorithm, String.join("", s).getBytes());
    }

    public static Digest createFromPath(Algorithm algorithm, Path path) throws NoSuchFileException {
        try {

            MessageDigest digest = MessageDigest.getInstance(algorithm.name);

            byte[] buffer = new byte[512];
            InputStream in = Files.newInputStream(path);
            DigestInputStream din = new DigestInputStream(in, digest);
            int rc = din.read(buffer);
            while (rc != -1) {
                // rc contain the number of bytes read in this operation.
                // next read
                rc = din.read(buffer);
            }
            din.close();
            return new Digest(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            if (e instanceof NoSuchFileException) {
                throw (NoSuchFileException) e;
            }
            throw new RuntimeException(e);
        }

    }

    public String getHashHex() {
        return HexaUtil.printHexBinary(bytes).toLowerCase();
    }

    public byte[] getHashBytes() {
        return bytes;
    }

}
