// File: AesGcmCrypto.java
package com.ejada.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES/GCM/NoPadding with layout: IV(12 bytes) || CIPHERTEXT+TAG
 *
 * Notes:
 * - Uses 96-bit IVs (12 bytes) as recommended for GCM.
 * - Auth tag is 128 bits (16 bytes) and is appended by JCE to the ciphertext.
 * - The payload returned by encrypt() is IV || (ciphertext||tag).
 * - AAD (if provided) is authenticated but not encrypted.
 *
 * Key sizes: 128/192/256-bit (subject to your JCE/Runtime policy).
 */
public final class AesGcmCrypto implements CryptoAlgorithm {

    public static final String TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int IV_LENGTH_BYTES = 12; // 96-bit nonce
    public static final int TAG_LENGTH_BITS = 128; // 16 bytes auth tag

    private final SecureRandom rng;

    public AesGcmCrypto() {
        this(new SecureRandom());
    }

    public AesGcmCrypto(SecureRandom rng) {
        this.rng = rng;
    }

    @Override
    public byte[] encrypt(byte[] plaintext, SecretKey key, byte[] aad) throws GeneralSecurityException {
        if (plaintext == null || key == null)
            throw new IllegalArgumentException("plaintext/key must not be null");

        byte[] iv = new byte[IV_LENGTH_BYTES];
        rng.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        if (aad != null && aad.length > 0)
            cipher.updateAAD(aad);

        byte[] ciphertext = cipher.doFinal(plaintext);

        // Layout: IV || ciphertext+tag
        byte[] out = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
        return out;
    }

    @Override
    public byte[] decrypt(byte[] payload, SecretKey key, byte[] aad) throws GeneralSecurityException {
        if (payload == null || key == null)
            throw new IllegalArgumentException("payload/key must not be null");
        if (payload.length < IV_LENGTH_BYTES + 16) { // must at least hold IV + tag
            throw new GeneralSecurityException("Invalid payload length");
        }

        byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        if (aad != null && aad.length > 0)
            cipher.updateAAD(aad);

        return cipher.doFinal(ciphertext);
    }
}
