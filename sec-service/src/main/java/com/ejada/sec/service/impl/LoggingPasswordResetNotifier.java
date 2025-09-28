package com.ejada.sec.service.impl;

import com.ejada.sec.domain.User;
import com.ejada.sec.service.PasswordResetNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default notifier that simply logs token dispatching. Real deployments
 * should replace this bean with one that integrates with email/SMS
 * providers.
 */
@Component
@Slf4j
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

  @Override
  public void notify(User user, String token) {
    log.info("Password reset token issued for user '{}' in tenant {}", user.getUsername(), user.getTenantId());
  }
}
