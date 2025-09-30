package com.ejada.sec.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.JwtTokenService;
import com.ejada.crypto.password.PasswordHasher;
import com.ejada.sec.domain.Superadmin;
import com.ejada.sec.domain.SuperadminPasswordHistory;
import com.ejada.sec.dto.admin.FirstLoginRequest;
import com.ejada.sec.exception.PasswordHistoryUnavailableException;
import com.ejada.sec.mapper.SuperadminMapper;
import com.ejada.sec.repository.SuperadminPasswordHistoryRepository;
import com.ejada.sec.repository.SuperadminRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SuperadminServiceImplTest {

    @Mock private SuperadminRepository superadminRepository;
    @Mock private SuperadminMapper superadminMapper;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private SuperadminPasswordHistoryRepository passwordHistoryRepository;
    @Mock private SuperadminAuditService superadminAuditService;

    private SuperadminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SuperadminServiceImpl(
            superadminRepository,
            superadminMapper,
            jwtTokenService,
            passwordHistoryRepository,
            superadminAuditService);

        ReflectionTestUtils.setField(service, "passwordExpiryDays", 90);
        ReflectionTestUtils.setField(service, "minActiveSuperadmins", 1);
        ReflectionTestUtils.setField(service, "superadminTokenTtl", Duration.ofHours(24));
        ReflectionTestUtils.setField(service, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(service, "lockoutDurationMinutes", 30);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completeFirstLoginUsesSubjectFallbackWhenUidMissing() {
        Superadmin superadmin = Superadmin.builder()
            .id(5L)
            .username("superadmin")
            .email("admin@ejada.com")
            .firstLoginCompleted(false)
            .passwordHash(PasswordHasher.bcrypt("Admin@123!"))
            .build();

        when(superadminRepository.findByIdentifier("superadmin"))
            .thenReturn(Optional.of(superadmin));
        when(superadminRepository.findById(5L)).thenReturn(Optional.of(superadmin));
        when(superadminRepository.save(superadmin)).thenReturn(superadmin);
        when(passwordHistoryRepository.findTop5BySuperadminIdOrderByCreatedAtDesc(5L))
            .thenReturn(List.of());

        Instant issuedAt = Instant.now().minusSeconds(60);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("superadmin")
            .claim("roles", List.of("EJADA_OFFICER"))
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
            jwt,
            List.of(new SimpleGrantedAuthority("ROLE_EJADA_OFFICER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        FirstLoginRequest request = FirstLoginRequest.builder()
            .currentPassword("Admin@123!")
            .newPassword("StrongerPass123!")
            .confirmPassword("StrongerPass123!")
            .build();

        LocalDateTime beforeCall = LocalDateTime.now();
        BaseResponse<Void> response = service.completeFirstLogin(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(superadmin.isFirstLoginCompleted()).isTrue();
        assertThat(superadmin.getPasswordChangedAt()).isNotNull();
        assertThat(superadmin.getPasswordExpiresAt()).isAfter(beforeCall);

        verify(superadminRepository).findByIdentifier("superadmin");
        verify(superadminRepository).save(superadmin);
        verify(passwordHistoryRepository).save(any());
    }

    @Test
    void completeFirstLoginSkipsBlankPasswordHistoryEntries() {
        Superadmin superadmin = Superadmin.builder()
            .id(7L)
            .username("superadmin")
            .email("admin@ejada.com")
            .firstLoginCompleted(false)
            .passwordHash(PasswordHasher.bcrypt("Admin@123!"))
            .build();

        when(superadminRepository.findById(7L)).thenReturn(Optional.of(superadmin));
        when(superadminRepository.save(superadmin)).thenReturn(superadmin);
        when(passwordHistoryRepository.findTop5BySuperadminIdOrderByCreatedAtDesc(7L))
            .thenReturn(List.of(
                SuperadminPasswordHistory.builder().id(1L).superadminId(7L).passwordHash(null).build(),
                SuperadminPasswordHistory.builder().id(2L).superadminId(7L).passwordHash("   ").build()
            ));

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .claim("uid", 7)
            .claim("roles", List.of("EJADA_OFFICER"))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
            jwt,
            List.of(new SimpleGrantedAuthority("ROLE_EJADA_OFFICER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        FirstLoginRequest request = FirstLoginRequest.builder()
            .currentPassword("Admin@123!")
            .newPassword("AnotherPass123!")
            .confirmPassword("AnotherPass123!")
            .build();

        BaseResponse<Void> response = service.completeFirstLogin(request);

        assertThat(response.isSuccess()).isTrue();
        verify(superadminRepository).save(superadmin);
        verify(passwordHistoryRepository).save(any());
    }

    @Test
    void completeFirstLoginThrowsWhenHistoryHashInvalid() {
        Superadmin superadmin = Superadmin.builder()
            .id(9L)
            .username("superadmin")
            .email("admin@ejada.com")
            .firstLoginCompleted(false)
            .passwordHash(PasswordHasher.bcrypt("Admin@123!"))
            .build();

        when(superadminRepository.findById(9L)).thenReturn(Optional.of(superadmin));
        when(passwordHistoryRepository.findTop5BySuperadminIdOrderByCreatedAtDesc(9L))
            .thenReturn(List.of(
                SuperadminPasswordHistory.builder().id(3L).superadminId(9L).passwordHash("not-a-bcrypt-hash").build()
            ));

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .claim("uid", 9)
            .claim("roles", List.of("EJADA_OFFICER"))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
            jwt,
            List.of(new SimpleGrantedAuthority("ROLE_EJADA_OFFICER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        FirstLoginRequest request = FirstLoginRequest.builder()
            .currentPassword("Admin@123!")
            .newPassword("NextPassword123!")
            .confirmPassword("NextPassword123!")
            .build();

        assertThatThrownBy(() -> service.completeFirstLogin(request))
            .isInstanceOf(PasswordHistoryUnavailableException.class);
        verify(superadminRepository, never()).save(any(Superadmin.class));
        verify(passwordHistoryRepository, never()).save(any());
    }

    @Test
    void completeFirstLoginRejectsRecentlyUsedPassword() {
        Superadmin superadmin = Superadmin.builder()
            .id(11L)
            .username("superadmin")
            .email("admin@ejada.com")
            .firstLoginCompleted(false)
            .passwordHash(PasswordHasher.bcrypt("Admin@123!"))
            .build();

        when(superadminRepository.findById(11L)).thenReturn(Optional.of(superadmin));
        when(passwordHistoryRepository.findTop5BySuperadminIdOrderByCreatedAtDesc(11L))
            .thenReturn(List.of(
                SuperadminPasswordHistory.builder()
                    .id(4L)
                    .superadminId(11L)
                    .passwordHash(PasswordHasher.bcrypt("ReusedPass123!"))
                    .build()
            ));

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .claim("uid", 11)
            .claim("roles", List.of("EJADA_OFFICER"))
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
            jwt,
            List.of(new SimpleGrantedAuthority("ROLE_EJADA_OFFICER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        FirstLoginRequest request = FirstLoginRequest.builder()
            .currentPassword("Admin@123!")
            .newPassword("ReusedPass123!")
            .confirmPassword("ReusedPass123!")
            .build();

        assertThatThrownBy(() -> service.completeFirstLogin(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("New password cannot match any of your last 5 passwords");

        verify(superadminRepository, never()).save(any(Superadmin.class));
        verify(passwordHistoryRepository, never()).save(any());
    }

    @Test
    void ensureTokenFreshnessRejectsTokenIssuedBeforePasswordChange() {
        Superadmin superadmin = Superadmin.builder()
            .id(13L)
            .username("admin")
            .passwordChangedAt(LocalDateTime.of(2025, 9, 29, 16, 6, 0))
            .build();

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .claim("uid", 13)
            .issuedAt(Instant.parse("2025-09-29T13:05:00Z"))
            .expiresAt(Instant.parse("2025-09-29T14:05:00Z"))
            .build();

        assertThatThrownBy(() ->
            ReflectionTestUtils.invokeMethod(service, "ensureTokenFreshness", jwt, superadmin))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessageContaining("Authentication token is no longer valid because the password was changed");
    }

    @Test
    void ensureTokenFreshnessAllowsNewTokenAfterPasswordChangeWithTimezoneOffset() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        String originalTimeZoneId = System.getProperty("user.timezone");

        try {
            TimeZone testZone = TimeZone.getTimeZone("Asia/Riyadh");
            TimeZone.setDefault(testZone);
            System.setProperty("user.timezone", testZone.getID());

            Superadmin superadmin = Superadmin.builder()
                .id(15L)
                .username("admin")
                .passwordChangedAt(LocalDateTime.of(2025, 9, 29, 16, 6, 0))
                .build();

            Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("uid", 15)
                .issuedAt(Instant.parse("2025-09-29T13:07:00Z"))
                .expiresAt(Instant.parse("2025-09-29T14:07:00Z"))
                .build();

            assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(service, "ensureTokenFreshness", jwt, superadmin));
        } finally {
            TimeZone.setDefault(originalTimeZone);
            if (originalTimeZoneId != null) {
                System.setProperty("user.timezone", originalTimeZoneId);
            } else {
                System.clearProperty("user.timezone");
            }
        }
    }

    @Test
    void ensureTokenFreshnessAllowsTokenIssuedInSameMillisecond() {
        Instant issuedAt = Instant.parse("2025-09-29T13:06:56.216Z");
        LocalDateTime passwordChangedAt = LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault());

        Superadmin superadmin = Superadmin.builder()
            .id(17L)
            .username("admin")
            .passwordChangedAt(passwordChangedAt)
            .build();

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .claim("uid", 17)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(3600))
            .build();

        assertDoesNotThrow(() ->
            ReflectionTestUtils.invokeMethod(service, "ensureTokenFreshness", jwt, superadmin));
    }
}
