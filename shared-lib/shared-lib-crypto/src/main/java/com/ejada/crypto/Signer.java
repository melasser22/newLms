package com.ejada.crypto;

import java.security.GeneralSecurityException;

public interface Signer {

    byte[] sign(byte[] data) throws GeneralSecurityException;

    boolean verify(byte[] data, byte[] expectedMac) throws GeneralSecurityException;

    String signToBase64(String messageUtf8) throws GeneralSecurityException;

    boolean verifyBase64(String messageUtf8, String expectedMacBase64) throws GeneralSecurityException;
}
