// File: CryptoService.java
package com.ejada.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * High-level crypto facade combining AES-GCM encryption and HMAC signing.
 *
 * @deprecated since 2.x; prefer using {@link Encryptor}, {@link Signer} and {@link CryptoFacade} directly.
 */
@Deprecated
public final class CryptoService {

    private final CryptoFacade facade;
    private final Supplier<SecretKey> encryptionKeySupplier;
    private final Supplier<SecretKey> macKeySupplier;

    private CryptoService(CryptoFacade facade,
                          Supplier<SecretKey> encryptionKeySupplier,
                          Supplier<SecretKey> macKeySupplier) {
        this.facade = Objects.requireNonNull(facade, "facade");
        this.encryptionKeySupplier = Objects.requireNonNull(encryptionKeySupplier, "encryptionKeySupplier");
        this.macKeySupplier = Objects.requireNonNull(macKeySupplier, "macKeySupplier");
    }

    /* ---------------- Encryption ---------------- */

    public byte[] encrypt(byte[] plaintext, byte[] aad) throws GeneralSecurityException {
        return facade.encrypt(plaintext, aad);
    }

    public byte[] decrypt(byte[] payload, byte[] aad) throws GeneralSecurityException {
        return facade.decrypt(payload, aad);
    }

    public String encryptToBase64(String plaintextUtf8, byte[] aad) throws GeneralSecurityException {
        return facade.encryptToBase64(plaintextUtf8, aad);
    }

    public String decryptFromBase64(String payloadBase64, byte[] aad) throws GeneralSecurityException {
        return facade.decryptFromBase64(payloadBase64, aad);
    }

    /** Convenience: encrypt raw bytes → Base64 string. */
    public String encryptBytesToBase64(byte[] plaintext, byte[] aad) throws GeneralSecurityException {
        return Base64.getEncoder().encodeToString(encrypt(plaintext, aad));
    }

    /** Convenience: decrypt Base64 string → raw bytes. */
    public byte[] decryptBytesFromBase64(String payloadBase64, byte[] aad) throws GeneralSecurityException {
        Objects.requireNonNull(payloadBase64, "payloadBase64");
        return decrypt(Base64.getDecoder().decode(payloadBase64), aad);
    }

    /* ---------------- Signing ---------------- */

    public byte[] sign(byte[] data) throws GeneralSecurityException {
        return facade.sign(data);
    }

    public boolean verify(byte[] data, byte[] expectedMac) throws GeneralSecurityException {
        return facade.verify(data, expectedMac);
    }

    public String signToBase64(String messageUtf8) throws GeneralSecurityException {
        return facade.signToBase64(messageUtf8);
    }

    public boolean verifyBase64(String messageUtf8, String expectedMacBase64) throws GeneralSecurityException {
        return facade.verifyBase64(messageUtf8, expectedMacBase64);
    }

    /** Convenience: sign raw bytes → Base64 string. */
    public String signBytesToBase64(byte[] data) throws GeneralSecurityException {
        Objects.requireNonNull(data, "data");
        return Base64.getEncoder().encodeToString(sign(data));
    }

    /** Convenience: verify raw bytes with Base64 MAC. */
    public boolean verifyBytesWithBase64Mac(byte[] data, String expectedMacBase64) throws GeneralSecurityException {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(expectedMacBase64, "expectedMacBase64");
        byte[] mac = Base64.getDecoder().decode(expectedMacBase64.getBytes(StandardCharsets.US_ASCII));
        return verify(data, mac);
    }

    /* ---------------- Builder ---------------- */

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private CryptoAlgorithm algorithm = new AesGcmCrypto();
        private Supplier<SecretKey> encKeySupplier;
        private Supplier<SecretKey> macKeySupplier;

        /** Set a custom crypto algorithm (default AES-GCM). */
        public Builder algorithm(CryptoAlgorithm algorithm) {
            this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
            return this;
        }

        /** Provide a static AES key from raw bytes (16/24/32 length). */
        public Builder encryptionKey(byte[] keyBytes) {
            Objects.requireNonNull(keyBytes, "keyBytes");
            byte[] cloned = keyBytes.clone(); // defensive copy
            CryptoUtils.validateKeyLength(cloned, 16, 24, 32);
            final SecretKey key = new SecretKeySpec(cloned, "AES");
            this.encKeySupplier = () -> key;
            return this;
        }

        /** Provide a static AES key via Base64. */
        public Builder encryptionKeyFromBase64(String base64) {
            byte[] decoded = CryptoUtils.safeBase64Decode(base64, "AES encryption key (Base64)");
            return encryptionKey(decoded);
        }

        /** Provide an AES key via Supplier (e.g., KMS-backed rotation). */
        public Builder encryptionKeySupplier(Supplier<SecretKey> supplier) {
            this.encKeySupplier = Objects.requireNonNull(supplier, "supplier");
            return this;
        }

        /** Provide a static HMAC key from raw bytes. */
        public Builder macKey(byte[] keyBytes) {
            Objects.requireNonNull(keyBytes, "keyBytes");
            byte[] cloned = keyBytes.clone();
            final SecretKey key = HmacSigner.hmacKey(cloned);
            this.macKeySupplier = () -> key;
            return this;
        }

        /** Provide a static HMAC key via Base64. */
        public Builder macKeyFromBase64(String base64) {
            byte[] decoded = CryptoUtils.safeBase64Decode(base64, "HMAC key (Base64)");
            return macKey(decoded);
        }

        /** Provide an HMAC key via Supplier. */
        public Builder macKeySupplier(Supplier<SecretKey> supplier) {
            this.macKeySupplier = Objects.requireNonNull(supplier, "supplier");
            return this;
        }

        public CryptoService build() {
            if (encKeySupplier == null) {
                throw new IllegalStateException("encryptionKey / encryptionKeySupplier is required");
            }
            if (macKeySupplier == null) {
                throw new IllegalStateException("macKey / macKeySupplier is required");
            }
            Encryptor encryptor = new AesGcmEncryptor(algorithm, encKeySupplier);
            Signer signer = new HmacSha256Signer(macKeySupplier);
            CryptoFacade facade = new CryptoFacade(encryptor, signer);
            return new CryptoService(facade, encKeySupplier, macKeySupplier);
        }

    }
}
