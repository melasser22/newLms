// File: HmacSigner.java
package com.shared.crypto;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * HMAC signer/verifier (HmacSHA256 by default).
 *
 * Provides Base64 and hex helpers and constant-time verification.
 */
public final class HmacSigner {

    public static final String DEFAULT_ALG = "HmacSHA256";

    private final String algorithm;

    public HmacSigner() {
        this(DEFAULT_ALG);
    }

    public HmacSigner(String algorithm) {
        this.algorithm = algorithm;
    }

    /** Signs data and returns raw MAC bytes. */
    public byte[] sign(byte[] data, SecretKey key) throws GeneralSecurityException {
        if (data == null || key == null)
            throw new IllegalArgumentException("data/key must not be null");
        Mac mac = Mac.getInstance(algorithm);
        mac.init(key);
        return mac.doFinal(data);
    }

    /** Signs UTF-8 string and returns Base64 string. */
    public String signToBase64(String messageUtf8, SecretKey key) throws GeneralSecurityException {
        byte[] mac = sign(messageUtf8.getBytes(java.nio.charset.StandardCharsets.UTF_8), key);
        return Base64.getEncoder().encodeToString(mac);
    }

    /** Signs UTF-8 string and returns lowercase hex string. */
    public String signToHex(String messageUtf8, SecretKey key) throws GeneralSecurityException {
        byte[] mac = sign(messageUtf8.getBytes(java.nio.charset.StandardCharsets.UTF_8), key);
        return toHex(mac);
    }

    /** Constant-time verification of provided MAC bytes. */
    public boolean verify(byte[] data, byte[] expectedMac, SecretKey key) throws GeneralSecurityException {
        byte[] actual = sign(data, key);
        return CryptoUtils.constantTimeEquals(actual, expectedMac);
    }

    /** Constant-time verification for Base64 strings. */
    public boolean verifyBase64(String messageUtf8, String expectedMacBase64, SecretKey key)
            throws GeneralSecurityException {
        byte[] expected = Base64.getDecoder().decode(expectedMacBase64);
        return verify(messageUtf8.getBytes(java.nio.charset.StandardCharsets.UTF_8), expected, key);
    }

    /** Utility to create an HMAC key from raw bytes. */
    public static SecretKey hmacKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, DEFAULT_ALG);
    }

    /* ------------------ helpers ------------------ */

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte x : bytes) {
            sb.append(Character.forDigit((x >>> 4) & 0xF, 16));
            sb.append(Character.forDigit((x & 0xF), 16));
        }
        return sb.toString();
    }
}
