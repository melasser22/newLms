package com.ejada.sec.service;

import com.ejada.sec.domain.User;

/**
 * Abstraction for delivering password reset tokens to end users via
 * email, SMS or any other out-of-band channel.
 */
public interface PasswordResetNotifier {
  void notify(User user, String token);
}
