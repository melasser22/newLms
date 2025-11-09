package com.ejada.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ejada.subscription.exception.InvalidCredentialsException;
import com.ejada.subscription.model.auth.SubscriptionUser;
import com.ejada.subscription.repository.SubscriptionUserRepository;
import com.ejada.subscription.security.JwtSigner;
import com.ejada.subscription.service.impl.SubscriptionAuthServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionAuthServiceImplTest {

  private static final String LOGIN = "xyz";
  private static final String PASSWORD =
      "b03ddf3ca2e714a6548e7495e2a03f5e824eaac9837cd7f159c67b90fb4b7342";

  @Mock private SubscriptionUserRepository repository;
  @Mock private JwtSigner jwtSigner;

  private SubscriptionAuthServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new SubscriptionAuthServiceImpl(repository, jwtSigner);
  }

  @Test
  void authenticateReturnsTokenForValidCredentials() {
    when(repository.findByLoginName(LOGIN)).thenReturn(Optional.of(new SubscriptionUser(LOGIN, PASSWORD)));
    when(jwtSigner.generateToken(LOGIN)).thenReturn("token");

    assertThat(service.authenticate(LOGIN, PASSWORD)).isEqualTo("token");
  }

  @Test
  void authenticateThrowsWhenPasswordDoesNotMatch() {
    when(repository.findByLoginName(LOGIN))
        .thenReturn(Optional.of(new SubscriptionUser(LOGIN, PASSWORD)));

    assertThatThrownBy(() -> service.authenticate(LOGIN, PASSWORD.replace('b', 'c')))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void authenticateThrowsWhenUserMissing() {
    when(repository.findByLoginName(LOGIN)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.authenticate(LOGIN, PASSWORD))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
