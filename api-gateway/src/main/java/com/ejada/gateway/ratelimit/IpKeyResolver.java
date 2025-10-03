package com.ejada.gateway.ratelimit;

import com.ejada.common.constants.HeaderNames;
import java.net.InetSocketAddress;
import java.util.List;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Key resolver that uses the originating client IP address. The resolver
 * honours common proxy headers (e.g. {@code X-Forwarded-For}) before falling
 * back to the TCP remote address exposed by Reactor Netty.
 */
@Component("ipKeyResolver")
public class IpKeyResolver implements KeyResolver {

  private static final List<String> FORWARDED_HEADERS = List.of(
      HeaderNames.CLIENT_IP,
      "X-Forwarded-For",
      "X-Real-IP"
  );

  @Override
  public Mono<String> resolve(ServerWebExchange exchange) {
    for (String header : FORWARDED_HEADERS) {
      String value = exchange.getRequest().getHeaders().getFirst(header);
      if (StringUtils.hasText(value)) {
        String candidate = extractFirst(value);
        if (StringUtils.hasText(candidate)) {
          return Mono.just(candidate);
        }
      }
    }

    InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
    String fallback = (remote != null) ? remote.getAddress().getHostAddress() : "unknown";
    return Mono.just(fallback);
  }

  private String extractFirst(String headerValue) {
    String[] parts = headerValue.split(",");
    if (parts.length == 0) {
      return null;
    }
    String first = parts[0].trim();
    return first.isEmpty() ? null : first;
  }
}

