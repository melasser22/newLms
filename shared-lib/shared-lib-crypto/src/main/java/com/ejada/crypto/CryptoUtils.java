package com.ejada.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Arrays;
import java.util.Objects;

/**
 * Utility methods for cryptographic helpers.
 */
public final class CryptoUtils {

    private CryptoUtils() {
    }

    /**
     * Constant-time comparison of two byte arrays to prevent timing attacks.
     * Returns {@code false} if either array is {@code null}.
     *
     * @param a first byte array
     * @param b second byte array
     * @return {@code true} if arrays are equal, {@code false} otherwise
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }

    /**
     * Decode a Base64 string, throwing an {@link IllegalArgumentException} with a
     * descriptive message if decoding fails.
     *
     * @param base64 the Base64 string
     * @param what   description of the data for error messages
     * @return decoded bytes
     */
    public static byte[] safeBase64Decode(String base64, String what) {
        Objects.requireNonNull(base64, what + " must not be null");
        try {
            return Base64.getDecoder().decode(base64.getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid Base64 for " + what + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Validates that a key has one of the expected lengths.
     *
     * @param key          the key bytes
     * @param validLengths acceptable lengths in bytes
     */
    public static void validateKeyLength(byte[] key, int... validLengths) {
        Objects.requireNonNull(key, "key");
        int len = key.length;
        for (int valid : validLengths) {
            if (len == valid) {
                return;
            }
        }
        throw new IllegalArgumentException(
            "Invalid key length: " + len + " bytes. Expected one of " + Arrays.toString(validLengths)
        );
    }
}

