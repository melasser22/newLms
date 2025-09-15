package com.ejada.crypto;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * HMAC-SHA256 implementation of Signer.  Delegates to existing HmacSigner.
 */
public class HmacSha256Signer implements Signer {

    private final HmacSigner signer = new HmacSigner(); // existing helper from your codebase
    private final Supplier<SecretKey> keySupplier;

    public HmacSha256Signer(Supplier<SecretKey> keySupplier) {
        this.keySupplier = Objects.requireNonNull(keySupplier, "keySupplier");
    }

    @Override
    public byte[] sign(byte[] data) throws GeneralSecurityException {
        Objects.requireNonNull(data, "data");
        return signer.sign(data, keySupplier.get());
    }

    @Override
    public boolean verify(byte[] data, byte[] expectedMac) throws GeneralSecurityException {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(expectedMac, "expectedMac");
        return signer.verify(data, expectedMac, keySupplier.get());
    }

    @Override
    public String signToBase64(String messageUtf8) throws GeneralSecurityException {
        Objects.requireNonNull(messageUtf8, "messageUtf8");
        return signer.signToBase64(messageUtf8, keySupplier.get());
    }

    @Override
    public boolean verifyBase64(String messageUtf8, String expectedMacBase64) throws GeneralSecurityException {
        Objects.requireNonNull(messageUtf8, "messageUtf8");
        Objects.requireNonNull(expectedMacBase64, "expectedMacBase64");
        return signer.verifyBase64(messageUtf8, expectedMacBase64, keySupplier.get());
    }
}
