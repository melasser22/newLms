package com.ejada.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility helpers for hashing opaque marketplace tokens prior to persistence.
 */
public final class TokenHashUtils {

    private TokenHashUtils() {
    }

    /**
     * Returns the SHA-256 hex digest for the supplied value or {@code null} when the input is {@code null}.
     */
    public static String sha256(final String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
