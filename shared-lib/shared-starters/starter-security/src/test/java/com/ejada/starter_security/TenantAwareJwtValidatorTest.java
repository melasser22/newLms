package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantAwareJwtValidatorTest {

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
    ContextManager.Tenant.clear();
  }

  @Test
  void succeedsWhenHeaderMatchesClaim() {
    TenantAwareJwtValidator validator = newValidator(true, true, true);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HeaderNames.X_TENANT_ID, "acme");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    OAuth2TokenValidatorResult result = validator.validate(jwtWithTenant("acme"));

    assertFalse(result.hasErrors());
  }

  @Test
  void failsWhenHeaderMissingAndRequired() {
    TenantAwareJwtValidator validator = newValidator(true, true, true);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

    OAuth2TokenValidatorResult result = validator.validate(jwtWithTenant("acme"));

    assertTrue(result.hasErrors());
  }

  @Test
  void failsWhenHeaderDiffersFromClaimInStrictMode() {
    TenantAwareJwtValidator validator = newValidator(true, true, true);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HeaderNames.X_TENANT_ID, "acme");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    OAuth2TokenValidatorResult result = validator.validate(jwtWithTenant("other"));

    assertTrue(result.hasErrors());
  }

  @Test
  void failsWhenContextDiffersFromHeaderInStrictMode() {
    TenantAwareJwtValidator validator = newValidator(true, true, true);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HeaderNames.X_TENANT_ID, "acme");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    ContextManager.Tenant.set("other");

    OAuth2TokenValidatorResult result = validator.validate(jwtWithTenant("acme"));

    assertTrue(result.hasErrors());
  }

  @Test
  void logsButAllowsMismatchWhenVerificationDisabled() {
    TenantAwareJwtValidator validator = newValidator(false, false, false);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HeaderNames.X_TENANT_ID, "acme");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    OAuth2TokenValidatorResult result = validator.validate(jwtWithTenant("other"));

    assertFalse(result.hasErrors());
  }

  private TenantAwareJwtValidator newValidator(boolean verifyClaim,
                                               boolean strictMode,
                                               boolean requireHeader) {
    SharedSecurityProps props = new SharedSecurityProps();
    props.setTenantClaim("tenant");
    props.getResourceServer().setVerifyTenantClaim(verifyClaim);
    props.getTenantVerification().setStrictMode(strictMode);
    props.getTenantVerification().setRequireTenantHeader(requireHeader);
    return new TenantAwareJwtValidator(props, null);
  }

  private Jwt jwtWithTenant(String tenant) {
    return Jwt.withTokenValue("token")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .header("alg", "none")
        .claims(claims -> {
          if (tenant != null) {
            claims.put("tenant", tenant);
          }
        })
        .build();
  }
}
