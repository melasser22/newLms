package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.password.PasswordHasher;
import com.ejada.sec.domain.PasswordResetToken;
import com.ejada.sec.domain.User;
import com.ejada.sec.dto.ForgotPasswordRequest;
import com.ejada.sec.dto.ResetPasswordRequest;
import com.ejada.sec.repository.PasswordResetTokenRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.PasswordResetNotifier;
import com.ejada.sec.service.PasswordResetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository tokenRepository;
  private final PasswordResetNotifier passwordResetNotifier;

  @Value("${security.reset.ttl-seconds:900}") // 15 minutes
  private long resetTtl;

  @Transactional
  @Override
  public BaseResponse<Void> createToken(ForgotPasswordRequest req) {
    // locate user by username OR email within tenant
    UUID tenantId = req.getTenantId();
    User user = userRepository.findByTenantIdAndUsername(tenantId, req.getIdentifier())
        .or(() -> userRepository.findByTenantIdAndEmail(tenantId, req.getIdentifier()))
        .orElseThrow(() -> new NoSuchElementException("Account not found"));

    Instant now = Instant.now();
    int revoked = tokenRepository.invalidateActiveTokens(user.getId(), now);
    if (revoked > 0) {
      log.debug("Expired {} previous password reset tokens for user '{}'", revoked, user.getUsername());
    }

    String token = UUID.randomUUID().toString();
    var prt = PasswordResetToken.builder()
        .user(user)
        .token(token)
        .expiresAt(now.plusSeconds(resetTtl))
        .build();
    tokenRepository.save(prt);
    passwordResetNotifier.notify(user, token);
    return BaseResponse.success("Reset token generated", null);
  }

  @Transactional
  @Override
  public BaseResponse<Void> reset(ResetPasswordRequest req) {
    var prt = tokenRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(
        req.getResetToken(), Instant.now()
    ).orElseThrow(() -> new NoSuchElementException("Invalid or expired reset token"));

    var user = prt.getUser();
    user.setPasswordHash(PasswordHasher.bcrypt(req.getNewPassword()));
    prt.setUsedAt(Instant.now());
    // repositories will update through transaction
    return BaseResponse.success("Password reset", null);
  }
}
