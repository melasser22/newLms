package com.ejada.sec.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.domain.User;
import com.ejada.sec.dto.AuthRequest;
import com.ejada.sec.dto.AuthResponse;
import com.ejada.sec.dto.RegisterRequest;
import com.ejada.sec.dto.UserDto;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.RefreshTokenService;
import com.ejada.sec.service.TokenIssuer;
import com.ejada.sec.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private UserService userService;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private TokenIssuer tokenIssuer;

  private AuthServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AuthServiceImpl(userRepository, userService, refreshTokenService, tokenIssuer);
  }

  @Test
  void registerReturnsTokensWhenUserCreationSucceeds() {
    UUID tenantId = UUID.randomUUID();
    RegisterRequest request = RegisterRequest.builder()
        .tenantId(tenantId)
        .username("jane")
        .email("jane@example.com")
        .password("s3cr3t")
        .build();

    UserDto dto = UserDto.builder()
        .id(42L)
        .tenantId(tenantId)
        .username("jane")
        .email("jane@example.com")
        .enabled(true)
        .locked(false)
        .build();

    when(userService.create(any())).thenReturn(BaseResponse.success("created", dto));
    when(refreshTokenService.issue(42L)).thenReturn("refresh-token");
    when(tokenIssuer.issueAccessToken(tenantId, 42L, "jane")).thenReturn("access-token");
    when(tokenIssuer.getAccessTokenTtlSeconds()).thenReturn(3600L);

    BaseResponse<AuthResponse> response = service.register(request);

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().getAccessToken()).isEqualTo("access-token");
    assertThat(response.getData().getRefreshToken()).isEqualTo("refresh-token");
  }

  @Test
  void registerPropagatesValidationErrors() {
    UUID tenantId = UUID.randomUUID();
    RegisterRequest request = RegisterRequest.builder()
        .tenantId(tenantId)
        .username("taken-user")
        .email("taken@example.com")
        .password("pw")
        .build();

    BaseResponse<UserDto> failure = BaseResponse.error("ERR_DUP", "username taken");
    when(userService.create(any())).thenReturn(failure);

    BaseResponse<AuthResponse> response = service.register(request);

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.getCode()).isEqualTo("ERR_DUP");
    assertThat(response.getData()).isNull();
  }

  @Test
  void loginRejectsLockedUsers() {
    UUID tenantId = UUID.randomUUID();
    AuthRequest request = AuthRequest.builder()
        .tenantId(tenantId)
        .identifier("locked-user")
        .password("pw")
        .build();

    User lockedUser = User.builder()
        .id(10L)
        .tenantId(tenantId)
        .username("locked-user")
        .passwordHash("hash")
        .enabled(true)
        .locked(true)
        .build();

    when(userRepository.findByTenantIdAndUsername(tenantId, "locked-user"))
        .thenReturn(Optional.of(lockedUser));

    assertThatThrownBy(() -> service.login(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Account disabled or locked");
  }
}
