package com.ejada.subscription.security;

@FunctionalInterface
public interface JwtValidator {
  boolean isValid(String token);
}
