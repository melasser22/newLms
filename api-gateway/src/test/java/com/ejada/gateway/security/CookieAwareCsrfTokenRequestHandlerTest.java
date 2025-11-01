package com.ejada.gateway.security;

import com.ejada.common.constants.HeaderNames;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import reactor.test.StepVerifier;

class CookieAwareCsrfTokenRequestHandlerTest {

  private final CookieAwareCsrfTokenRequestHandler handler = new CookieAwareCsrfTokenRequestHandler();
  private final CsrfToken token = new DefaultCsrfToken(HeaderNames.CSRF_TOKEN, "_csrf", "token-123");

  @Test
  void resolvesValueFromHeaderWhenPresent() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
        .header(token.getHeaderName(), token.getToken())
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(handler.resolveCsrfTokenValue(exchange, token))
        .expectNext(token.getToken())
        .verifyComplete();
  }

  @Test
  void resolvesValueFromXsrfCookieWhenHeaderMissing() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
        .cookie(new HttpCookie("XSRF-TOKEN", token.getToken()))
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(handler.resolveCsrfTokenValue(exchange, token))
        .expectNext(token.getToken())
        .verifyComplete();
  }

  @Test
  void resolvesValueFromHeaderNamedCookieWhenXsrfCookieMissing() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
        .cookie(new HttpCookie(HeaderNames.CSRF_TOKEN, token.getToken()))
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(handler.resolveCsrfTokenValue(exchange, token))
        .expectNext(token.getToken())
        .verifyComplete();
  }

  @Test
  void returnsEmptyWhenNoHeaderOrCookiePresent() {
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
        .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    StepVerifier.create(handler.resolveCsrfTokenValue(exchange, token))
        .verifyComplete();
  }
}
