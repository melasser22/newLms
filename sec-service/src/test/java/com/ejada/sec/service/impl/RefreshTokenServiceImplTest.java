package com.ejada.sec.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.sec.domain.RefreshToken;
import com.ejada.sec.domain.User;
import com.ejada.sec.repository.RefreshTokenRepository;
import com.ejada.sec.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
class RefreshTokenServiceImplTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private UserRepository userRepository;

  private RefreshTokenServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new RefreshTokenServiceImpl(refreshTokenRepository, userRepository);
    ReflectionTestUtils.setField(service, "ttlSeconds", 120L);
    ReflectionTestUtils.setField(service, "maxActivePerUser", 10);
    ReflectionTestUtils.setField(service, "maxActivePerTenant", 100);
  }

  @Test
  void issueGeneratesTokenWithConfiguredTtlAndDeletesExistingOnes() {
    UUID tenantId = UUID.randomUUID();
    User user = User.builder().id(5L).tenantId(tenantId).build();
    when(userRepository.findById(5L)).thenReturn(Optional.of(user));
    when(refreshTokenRepository.findActiveTokensByUserId(eq(5L), any())).thenReturn(java.util.Collections.emptyList());
    when(refreshTokenRepository.findActiveTokensByTenant(eq(tenantId), any())).thenReturn(java.util.Collections.emptyList());
    when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UUID generated = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    try (var mockedUuid = mockStatic(UUID.class)) {
      mockedUuid.when(UUID::randomUUID).thenReturn(generated);
      String issued = service.issue(5L, true, "prev-token");
      assertThat(issued).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
    }

    verify(refreshTokenRepository).deleteByUserId(5L);
    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    RefreshToken saved = captor.getValue();
    assertThat(saved.getToken()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");
    assertThat(saved.getExpiresAt()).isAfter(saved.getIssuedAt());
    assertThat(saved.getExpiresAt()).isEqualTo(saved.getIssuedAt().plusSeconds(120L));
    assertThat(saved.getRotatedFrom()).isEqualTo("prev-token");
  }

  @Test
  void issueEnforcesTenantLimitWithoutRevokingTooManyTokens() {
    ReflectionTestUtils.setField(service, "maxActivePerTenant", 2);

    UUID tenantId = UUID.randomUUID();
    User user = User.builder().id(7L).tenantId(tenantId).build();
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));

    var stored = new ArrayList<RefreshToken>();
    Instant base = Instant.now();
    RefreshToken oldest = RefreshToken.builder()
        .token("oldest")
        .user(user)
        .issuedAt(base.minusSeconds(300))
        .expiresAt(base.plusSeconds(600))
        .build();
    RefreshToken newer = RefreshToken.builder()
        .token("newer")
        .user(user)
        .issuedAt(base.minusSeconds(60))
        .expiresAt(base.plusSeconds(600))
        .build();
    stored.add(oldest);
    stored.add(newer);

    when(refreshTokenRepository.findActiveTokensByUserId(eq(7L), any()))
        .thenReturn(java.util.Collections.emptyList());
    when(refreshTokenRepository.findActiveTokensByTenant(eq(tenantId), any()))
        .thenAnswer(invocation -> List.copyOf(stored));
    when(refreshTokenRepository.save(any())).thenAnswer(invocation -> {
      RefreshToken token = invocation.getArgument(0);
      if (stored.stream().noneMatch(existing -> existing == token)) {
        stored.add(token);
      }
      return token;
    });

    String issued = service.issue(7L, false, null);
    assertThat(issued).isNotBlank();

    long revokedCount = stored.stream().filter(t -> t.getRevokedAt() != null).count();
    assertThat(revokedCount).isEqualTo(1);
    assertThat(oldest.getRevokedAt()).isNotNull();
    assertThat(newer.getRevokedAt()).isNull();
  }

  @Test
  void issueHonorsTenantLimitOfOne() {
    ReflectionTestUtils.setField(service, "maxActivePerTenant", 1);

    UUID tenantId = UUID.randomUUID();
    User user = User.builder().id(11L).tenantId(tenantId).build();
    when(userRepository.findById(11L)).thenReturn(Optional.of(user));

    var stored = new ArrayList<RefreshToken>();
    Instant base = Instant.now();
    RefreshToken existing = RefreshToken.builder()
        .token("existing")
        .user(user)
        .issuedAt(base.minusSeconds(120))
        .expiresAt(base.plusSeconds(600))
        .build();
    stored.add(existing);

    when(refreshTokenRepository.findActiveTokensByUserId(eq(11L), any()))
        .thenReturn(java.util.Collections.emptyList());
    when(refreshTokenRepository.findActiveTokensByTenant(eq(tenantId), any()))
        .thenAnswer(invocation -> List.copyOf(stored));
    when(refreshTokenRepository.save(any()))
        .thenAnswer(
            invocation -> {
              RefreshToken token = invocation.getArgument(0);
              if (stored.stream().noneMatch(existingToken -> existingToken == token)) {
                stored.add(token);
              }
              return token;
            });

    String issued = service.issue(11L, false, null);
    assertThat(issued).isNotBlank();

    long activeCount =
        stored.stream().filter(t -> t.getRevokedAt() == null && t.getExpiresAt().isAfter(base)).count();
    assertThat(activeCount).isEqualTo(1);
    assertThat(existing.getRevokedAt()).isNotNull();
  }

  @Test
  void validateAndGetUserRejectsExpiredOrRevokedTokens() {
    when(refreshTokenRepository.findByToken("bad"))
        .thenReturn(Optional.of(RefreshToken.builder()
            .token("bad")
            .issuedAt(Instant.now().minusSeconds(10))
            .expiresAt(Instant.now().minusSeconds(5))
            .build()));

    assertThatThrownBy(() -> service.validateAndGetUser("bad"))
        .isInstanceOf(java.util.NoSuchElementException.class)
        .hasMessageContaining("Invalid or expired refresh token");
  }

  @Test
  void validateAndGetUserReturnsLinkedUser() {
    User user = User.builder().id(9L).build();
    RefreshToken token = RefreshToken.builder()
        .token("good")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .user(user)
        .build();
    when(refreshTokenRepository.findByToken("good")).thenReturn(Optional.of(token));

    User resolved = service.validateAndGetUser("good");
    assertThat(resolved).isEqualTo(user);
  }

  @Test
  void revokeMarksTokenAsRevokedWhenFound() {
    RefreshToken token = RefreshToken.builder()
        .token("tok")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
    when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

    service.revoke("tok");

    verify(refreshTokenRepository).save(token);
    assertThat(token.getRevokedAt()).isNotNull();
  }

  @Test
  void revokeExpiredDelegatesToRepository() {
    when(refreshTokenRepository.deleteByExpiresAtBefore(any())).thenReturn(3);

    int deleted = service.revokeExpired();

    assertThat(deleted).isEqualTo(3);
    verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(any());
  }
}
