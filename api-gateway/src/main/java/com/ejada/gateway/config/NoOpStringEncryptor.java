package com.ejada.gateway.config;

import org.jasypt.encryption.StringEncryptor;

/**
 * Minimal {@link StringEncryptor} implementation that simply echoes values back to callers.
 *
 * <p>This is intended for development scenarios where encrypted secrets are not required. The
 * encryptor should never be used in production because it does not provide any confidentiality for
 * stored values.</p>
 */
final class NoOpStringEncryptor implements StringEncryptor {

  @Override
  public String encrypt(String message) {
    return message;
  }

  @Override
  public String decrypt(String encryptedMessage) {
    return encryptedMessage;
  }
}

