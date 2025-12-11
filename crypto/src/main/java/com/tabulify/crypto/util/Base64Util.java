package com.tabulify.crypto.util;


import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A series of Base64 utility that wraps around {@link java.util.Base64}
 */
public class Base64Util {


    public static String toBase64(byte[] binaryData) {
        return java.util.Base64.getEncoder().encodeToString(binaryData);
    }

    @SuppressWarnings("unused")
    public static String toBase64Url(byte[] binaryData) {
        return java.util.Base64.getUrlEncoder().encodeToString(binaryData);
    }

    /**
     * Encode a string in base64
     *
     * @param string - the string
     * @return the string in base 64
     */
    @SuppressWarnings("unused")
    public static String toBase64Url(String string) {
        return java.util.Base64.getUrlEncoder().encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }


    @SuppressWarnings("unused")
    public static String toBase64UrlWithoutPadding(byte[] bytes) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public static byte[] toBytesFromBase64(String base64String) {
        return java.util.Base64.getDecoder().decode(base64String);
    }

    @SuppressWarnings("unused")
    public static byte[] toBytesFromBase64Url(String base64String) {
        try {
            return java.util.Base64.getUrlDecoder().decode(base64String);
        } catch (Exception e) {
            throw new RuntimeException("The string is not a base 64 string", e);
        }
    }

}
