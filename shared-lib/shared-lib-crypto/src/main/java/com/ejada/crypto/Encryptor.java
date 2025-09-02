package com.ejada.crypto;

import java.security.GeneralSecurityException;

/**
 * Symmetric encryption interface.  Implementations define the algorithm and key management.
 */
public interface Encryptor {

    byte[] encrypt(byte[] plaintext, byte[] aad) throws GeneralSecurityException;

    byte[] decrypt(byte[] ciphertext, byte[] aad) throws GeneralSecurityException;

    String encryptToBase64(String plaintextUtf8, byte[] aad) throws GeneralSecurityException;

    String decryptFromBase64(String ciphertextBase64, byte[] aad) throws GeneralSecurityException;
}
