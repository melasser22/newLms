package com.ejada.sec.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.domain.PasswordResetToken;
import com.ejada.sec.domain.User;
import com.ejada.sec.dto.ForgotPasswordRequest;
import com.ejada.sec.dto.ResetPasswordRequest;
import com.ejada.sec.repository.PasswordResetTokenRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.PasswordResetNotifier;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordResetTokenRepository tokenRepository;
  @Mock private PasswordResetNotifier passwordResetNotifier;

  private PasswordResetServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new PasswordResetServiceImpl(userRepository, tokenRepository, passwordResetNotifier);
    ReflectionTestUtils.setField(service, "resetTtl", 300L);
  }

  @Test
  void createTokenReplacesExistingTokensAndPersistsNewToken() {
    UUID tenantId = UUID.randomUUID();
    User user = User.builder().id(7L).tenantId(tenantId).username("user").email("user@example.com").build();
    when(userRepository.findByTenantIdAndUsername(tenantId, "user"))
        .thenReturn(Optional.of(user));
    when(tokenRepository.invalidateActiveTokens(eq(7L), any(Instant.class))).thenReturn(0);
    when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ForgotPasswordRequest request = ForgotPasswordRequest.builder()
        .tenantId(tenantId)
        .identifier("user")
        .build();

    try (var mockedUuid = mockStatic(UUID.class)) {
      mockedUuid.when(UUID::randomUUID).thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));

      BaseResponse<Void> response = service.createToken(request);

      assertThat(response.isSuccess()).isTrue();
      assertThat(response.getData()).isNull();
    }

    verify(tokenRepository).invalidateActiveTokens(eq(7L), any(Instant.class));
    verify(tokenRepository).deleteByUserId(7L);
    ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(tokenRepository).save(captor.capture());
    PasswordResetToken saved = captor.getValue();
    assertThat(saved.getUser()).isEqualTo(user);
    assertThat(saved.getToken()).isEqualTo("123e4567-e89b-12d3-a456-426614174001");
    assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    verify(passwordResetNotifier).notify(user, "123e4567-e89b-12d3-a456-426614174001");
  }

  @Test
  void resetRejectsExpiredTokens() {
    when(tokenRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(any(), any()))
        .thenReturn(Optional.empty());

    ResetPasswordRequest request = ResetPasswordRequest.builder()
        .resetToken("expired")
        .newPassword("new-pass")
        .build();

    assertThatThrownBy(() -> service.reset(request))
        .isInstanceOf(java.util.NoSuchElementException.class)
        .hasMessageContaining("Invalid or expired reset token");
  }

  @Test
  void resetUpdatesPasswordAndMarksTokenAsUsed() {
    User user = User.builder().id(9L).passwordHash("old").build();
    PasswordResetToken token = PasswordResetToken.builder()
        .token("valid")
        .user(user)
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
    when(tokenRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(any(), any()))
        .thenReturn(Optional.of(token));

    ResetPasswordRequest request = ResetPasswordRequest.builder()
        .resetToken("valid")
        .newPassword("new-pass")
        .build();

    BaseResponse<Void> response = service.reset(request);

    assertThat(response.isSuccess()).isTrue();
    assertThat(user.getPasswordHash()).isNotEqualTo("old");
    assertThat(token.getUsedAt()).isNotNull();
  }
}
