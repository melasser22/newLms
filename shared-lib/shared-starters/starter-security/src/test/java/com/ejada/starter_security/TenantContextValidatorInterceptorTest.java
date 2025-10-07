package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantContextValidatorInterceptorTest {

  @AfterEach
  void cleanup() {
    ContextManager.Tenant.clear();
  }

  @Test
  void allowsMatchingHeaderAndContext() throws Exception {
    TenantContextValidatorInterceptor interceptor = newInterceptor(true, true, true);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HeaderNames.X_TENANT_ID, "acme");
    ContextManager.Tenant.set("acme");

    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(interceptor.preHandle(request, response, new Object()));
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }

  @Test
  void rejectsMissingHeaderWhenRequired() throws Exception {
    TenantContextValidatorInterceptor interceptor = newInterceptor(false, true, true);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertFalse(interceptor.preHandle(request, response, new Object()));
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  void rejectsMismatchedContextInStrictMode() throws Exception {
    TenantContextValidatorInterceptor interceptor = newInterceptor(true, true, false);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HeaderNames.X_TENANT_ID, "acme");
    ContextManager.Tenant.set("other");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertFalse(interceptor.preHandle(request, response, new Object()));
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
  }

  @Test
  void allowsMismatchWhenNotStrict() throws Exception {
    TenantContextValidatorInterceptor interceptor = newInterceptor(true, false, false);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HeaderNames.X_TENANT_ID, "acme");
    ContextManager.Tenant.set("other");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(interceptor.preHandle(request, response, new Object()));
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }

  private TenantContextValidatorInterceptor newInterceptor(boolean verify,
                                                            boolean strict,
                                                            boolean requireHeader) {
    SharedSecurityProps props = new SharedSecurityProps();
    props.getResourceServer().setVerifyTenantClaim(verify);
    props.getTenantVerification().setStrictMode(strict);
    props.getTenantVerification().setRequireTenantHeader(requireHeader);
    return new TenantContextValidatorInterceptor(props);
  }
}
