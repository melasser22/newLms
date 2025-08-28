// File: CryptoAlgorithm.java
package com.shared.crypto;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

/**
 * Pluggable crypto primitive for symmetric encryption.
 *
 * Implementations must be deterministic about their output format.
 * Return value for encrypt() is an opaque byte[] the caller can
 * persist/transmit
 * and pass back to decrypt() unchanged.
 */
public interface CryptoAlgorithm {

    /**
     * Encrypts plaintext and returns an opaque payload.
     *
     * @param plaintext bytes to encrypt (not null)
     * @param key       symmetric key (algorithm-specific; not null)
     * @param aad       optional Additional Authenticated Data (AAD); may be null
     * @return opaque payload bytes suitable for storage/transmission
     */
    byte[] encrypt(byte[] plaintext, SecretKey key, byte[] aad) throws GeneralSecurityException;

    /**
     * Decrypts a payload previously produced by encrypt().
     *
     * @param payload opaque payload returned from encrypt()
     * @param key     symmetric key used to encrypt
     * @param aad     the same AAD used during encryption; may be null if none was
     *                used
     * @return plaintext bytes
     */
    byte[] decrypt(byte[] payload, SecretKey key, byte[] aad) throws GeneralSecurityException;

    /* ---------- Convenience UTF-8 + Base64 helpers ---------- */

    default String encryptToBase64(String plaintextUtf8, SecretKey key, byte[] aad)
            throws GeneralSecurityException {
        byte[] ct = encrypt(plaintextUtf8.getBytes(java.nio.charset.StandardCharsets.UTF_8), key, aad);
        return java.util.Base64.getEncoder().encodeToString(ct);
    }

    default String decryptFromBase64(String payloadBase64, SecretKey key, byte[] aad)
            throws GeneralSecurityException {
        byte[] payload = java.util.Base64.getDecoder().decode(payloadBase64);
        byte[] pt = decrypt(payload, key, aad);
        return new String(pt, java.nio.charset.StandardCharsets.UTF_8);
    }
}
