package com.shared.crypto;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * AES-GCM implementation of Encryptor.  Delegates to CryptoAlgorithm and uses a key supplier.
 */
public class AesGcmEncryptor implements Encryptor {

    private final CryptoAlgorithm algorithm;
    private final Supplier<SecretKey> keySupplier;

    public AesGcmEncryptor(Supplier<SecretKey> keySupplier) {
        this(new AesGcmCrypto(), keySupplier);
    }

    public AesGcmEncryptor(CryptoAlgorithm algorithm, Supplier<SecretKey> keySupplier) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.keySupplier = Objects.requireNonNull(keySupplier, "keySupplier");
    }

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] aad) throws GeneralSecurityException {
        Objects.requireNonNull(plaintext, "plaintext");
        return algorithm.encrypt(plaintext, keySupplier.get(), aad);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] aad) throws GeneralSecurityException {
        Objects.requireNonNull(ciphertext, "ciphertext");
        return algorithm.decrypt(ciphertext, keySupplier.get(), aad);
    }

    @Override
    public String encryptToBase64(String plaintextUtf8, byte[] aad) throws GeneralSecurityException {
        Objects.requireNonNull(plaintextUtf8, "plaintextUtf8");
        return algorithm.encryptToBase64(plaintextUtf8, keySupplier.get(), aad);
    }

    @Override
    public String decryptFromBase64(String ciphertextBase64, byte[] aad) throws GeneralSecurityException {
        Objects.requireNonNull(ciphertextBase64, "ciphertextBase64");
        return algorithm.decryptFromBase64(ciphertextBase64, keySupplier.get(), aad);
    }
}
