package com.ejada.sec.context;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Helper component that extracts auditing metadata (created/updated timestamps and actors)
 * from the authenticated JWT token.
 */
@Component
@Slf4j
public class RequestAuditContextProvider {

  private static final String[] CREATED_BY_CLAIMS = {
      "createdBy", "created_by", "email", "preferred_username", "username", "user_name", "sub"
  };
  private static final String[] UPDATED_BY_CLAIMS = {
      "updatedBy", "updated_by"
  };
  private static final String[] CREATED_AT_CLAIMS = {
      "createdAt", "created_at"
  };
  private static final String[] UPDATED_AT_CLAIMS = {
      "updatedAt", "updated_at"
  };

  /** Resolve the creator identifier from JWT claims. */
  public Optional<String> resolveCreatedBy() {
    return resolveStringClaim(CREATED_BY_CLAIMS);
  }

  /** Resolve the modifier identifier from JWT claims (defaults to creator when missing). */
  public Optional<String> resolveUpdatedBy() {
    return resolveStringClaim(UPDATED_BY_CLAIMS).or(this::resolveCreatedBy);
  }

  /** Resolve the creation timestamp from JWT claims or fall back to iat claim. */
  public Optional<Instant> resolveCreatedAt() {
    return resolveInstantClaim(CREATED_AT_CLAIMS).or(this::resolveIssuedAt);
  }

  /** Resolve the last updated timestamp from JWT claims (defaults to creation time). */
  public Optional<Instant> resolveUpdatedAt() {
    return resolveInstantClaim(UPDATED_AT_CLAIMS).or(this::resolveCreatedAt);
  }

  private Optional<Instant> resolveInstantClaim(String[] claimNames) {
    return currentJwt()
        .flatMap(jwt -> {
          for (String claim : claimNames) {
            Object value = jwt.getClaims().get(claim);
            if (value != null) {
              return Optional.ofNullable(parseInstant(value, claim));
            }
          }
          return Optional.empty();
        });
  }

  private Optional<String> resolveStringClaim(String[] claimNames) {
    return currentJwt()
        .flatMap(jwt -> {
          for (String claim : claimNames) {
            Object value = jwt.getClaims().get(claim);
            if (value instanceof String str && !str.isBlank()) {
              return Optional.of(str);
            }
          }
          return Optional.empty();
        });
  }

  private Optional<Instant> resolveIssuedAt() {
    return currentJwt().map(Jwt::getIssuedAt);
  }

  private Instant parseInstant(Object raw, String claimName) {
    if (raw instanceof Instant instant) {
      return instant;
    }
    if (raw instanceof Number number) {
      long epochSeconds = number.longValue();
      return Instant.ofEpochSecond(epochSeconds);
    }
    if (raw instanceof String text && !text.isBlank()) {
      try {
        return Instant.parse(text);
      } catch (DateTimeParseException ex) {
        log.debug("Unable to parse claim '{}' as Instant", claimName, ex);
      }
    }
    return null;
  }

  private Optional<Jwt> currentJwt() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return Optional.of(jwtAuth.getToken());
    }
    return Optional.empty();
  }
}
