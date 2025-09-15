package com.ejada.crypto;

import java.security.GeneralSecurityException;

public class CryptoFacade {

    private final Encryptor encryptor;
    private final Signer signer;

    public CryptoFacade(Encryptor encryptor, Signer signer) {
        this.encryptor = encryptor;
        this.signer = signer;
    }

    // Encryption
    public byte[] encrypt(byte[] plaintext, byte[] aad) throws GeneralSecurityException {
        return encryptor.encrypt(plaintext, aad);
    }

    public byte[] decrypt(byte[] ciphertext, byte[] aad) throws GeneralSecurityException {
        return encryptor.decrypt(ciphertext, aad);
    }

    public String encryptToBase64(String plaintextUtf8, byte[] aad) throws GeneralSecurityException {
        return encryptor.encryptToBase64(plaintextUtf8, aad);
    }

    public String decryptFromBase64(String ciphertextBase64, byte[] aad) throws GeneralSecurityException {
        return encryptor.decryptFromBase64(ciphertextBase64, aad);
    }

    // Signing
    public byte[] sign(byte[] data) throws GeneralSecurityException {
        return signer.sign(data);
    }

    public boolean verify(byte[] data, byte[] expectedMac) throws GeneralSecurityException {
        return signer.verify(data, expectedMac);
    }

    public String signToBase64(String messageUtf8) throws GeneralSecurityException {
        return signer.signToBase64(messageUtf8);
    }

    public boolean verifyBase64(String messageUtf8, String expectedMacBase64) throws GeneralSecurityException {
        return signer.verifyBase64(messageUtf8, expectedMacBase64);
    }
}
