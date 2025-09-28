package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.password.PasswordHasher;
import com.ejada.sec.domain.PasswordResetToken;
import com.ejada.sec.domain.User;
import com.ejada.sec.dto.ForgotPasswordRequest;
import com.ejada.sec.dto.ResetPasswordRequest;
import com.ejada.sec.repository.PasswordResetTokenRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.PasswordResetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository tokenRepository;

  @Value("${security.reset.ttl-seconds:900}") // 15 minutes
  private long resetTtl;

  @Transactional
  @Override
  public BaseResponse<String> createToken(ForgotPasswordRequest req) {
    // locate user by username OR email within tenant
    UUID tenantId = req.getTenantId();
    User user = userRepository.findByTenantIdAndUsername(tenantId, req.getIdentifier())
        .or(() -> userRepository.findByTenantIdAndEmail(tenantId, req.getIdentifier()))
        .orElseThrow(() -> new NoSuchElementException("Account not found"));

    String token = UUID.randomUUID().toString();
    tokenRepository.deleteByUserId(user.getId());

    var prt = PasswordResetToken.builder()
        .user(user)
        .token(token)
        .expiresAt(Instant.now().plusSeconds(resetTtl))
        .build();
    tokenRepository.save(prt);
    return BaseResponse.success("Reset token generated", token); // deliver via mail/SMS service externally
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
