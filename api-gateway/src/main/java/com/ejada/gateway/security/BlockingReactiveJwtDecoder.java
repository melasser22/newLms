package com.ejada.gateway.security;

import java.util.Objects;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Adapts a blocking {@link JwtDecoder} so it can be used safely in a reactive pipeline.
 */
public class BlockingReactiveJwtDecoder implements ReactiveJwtDecoder {

  private final JwtDecoder delegate;
  private final Scheduler scheduler;

  public BlockingReactiveJwtDecoder(JwtDecoder delegate) {
    this(delegate, Schedulers.boundedElastic());
  }

  public BlockingReactiveJwtDecoder(JwtDecoder delegate, Scheduler scheduler) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
  }

  @Override
  public Mono<Jwt> decode(String token) {
    return Mono.defer(() -> Mono.fromCallable(() -> delegate.decode(token)))
        .subscribeOn(scheduler);
  }
}

