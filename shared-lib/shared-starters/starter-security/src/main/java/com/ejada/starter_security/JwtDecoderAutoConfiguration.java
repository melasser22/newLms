package com.ejada.starter_security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;

@AutoConfiguration
@EnableConfigurationProperties(SharedSecurityProps.class)
public class JwtDecoderAutoConfiguration {

  @Bean
  public JwtDecoder jwtDecoder(SharedSecurityProps props) {
    if ("jwks".equalsIgnoreCase(props.getMode())) {
      String uri = props.getJwks().getUri();
      Assert.hasText(uri, "shared.security.jwks.uri must not be null or empty when mode=jwks");
      return NimbusJwtDecoder.withJwkSetUri(uri).build();
    }

    String secret = props.getHs256().getSecret();
    Assert.hasText(secret, "shared.security.hs256.secret must not be null or empty when mode=hs256");
    SecretKey key = new SecretKeySpec(resolveHs256Secret(secret), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }

  private static byte[] resolveHs256Secret(String secret) {
    byte[] decoded = tryDecodeBase64(secret);
    if (decoded != null && decoded.length >= 32) {
      return decoded;
    }
    return secret.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] tryDecodeBase64(String value) {
    try {
      byte[] decoded = Base64.getDecoder().decode(value);
      return decoded.length == 0 ? null : decoded;
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
