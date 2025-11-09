package com.ejada.subscription.service.impl;

import com.ejada.subscription.exception.InvalidCredentialsException;
import com.ejada.subscription.repository.SubscriptionUserRepository;
import com.ejada.subscription.security.JwtSigner;
import com.ejada.subscription.service.SubscriptionAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionAuthServiceImpl implements SubscriptionAuthService {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionAuthServiceImpl.class);
  private final SubscriptionUserRepository userRepository;
  private final JwtSigner jwtSigner;

  @Override
  public String authenticate(final String loginName, final String sha256Password) {
    return userRepository
        .findByLoginName(loginName)
        .filter(user -> passwordMatches(user.password(), sha256Password))
        .map(user -> {
          log.debug("Authentication succeeded for loginName={}", loginName);
          return jwtSigner.generateToken(loginName);
        })
        .orElseThrow(InvalidCredentialsException::new);
  }

  private boolean passwordMatches(final String storedPassword, final String providedPassword) {
    return storedPassword != null
        && providedPassword != null
        && storedPassword.equalsIgnoreCase(providedPassword);
  }
}
