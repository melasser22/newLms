package com.ejada.sec.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.JwtTokenService;
import com.ejada.crypto.password.PasswordHasher;
import com.ejada.sec.domain.Superadmin;
import com.ejada.sec.dto.admin.FirstLoginRequest;
import com.ejada.sec.mapper.SuperadminMapper;
import com.ejada.sec.repository.SuperadminPasswordHistoryRepository;
import com.ejada.sec.repository.SuperadminRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
