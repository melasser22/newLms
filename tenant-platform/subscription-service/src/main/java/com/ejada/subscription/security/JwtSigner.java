package com.ejada.subscription.security;

public interface JwtSigner {
  String generateToken(String subject);
}
