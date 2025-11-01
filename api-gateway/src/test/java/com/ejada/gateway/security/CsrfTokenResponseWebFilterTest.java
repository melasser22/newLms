package com.ejada.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CsrfTokenResponseWebFilterTest {

  private final CsrfTokenResponseWebFilter filter = new CsrfTokenResponseWebFilter();

  @Test
  void addsHeaderWhenTokenPresent() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test"));
    CsrfToken token = new DefaultCsrfToken(HeaderNames.CSRF_TOKEN, "_csrf", "csrf-123");
    exchange.getAttributes().put(CsrfToken.class.getName(), Mono.just(token));
    TestWebFilterChain chain = new TestWebFilterChain();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getHeaders().getFirst(HeaderNames.CSRF_TOKEN))
        .isEqualTo("csrf-123");
    assertThat(chain.invoked).isTrue();
  }

  @Test
  void skipsHeaderWhenTokenMissing() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test"));
    TestWebFilterChain chain = new TestWebFilterChain();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getHeaders().containsKey(HeaderNames.CSRF_TOKEN)).isFalse();
    assertThat(chain.invoked).isTrue();
  }

  private static class TestWebFilterChain implements WebFilterChain {

    private boolean invoked;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange) {
      invoked = true;
      return Mono.empty();
    }
  }
}

