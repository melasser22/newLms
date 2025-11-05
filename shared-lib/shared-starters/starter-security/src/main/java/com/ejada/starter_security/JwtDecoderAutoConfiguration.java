package com.ejada.starter_security;

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
    SecretKey key = buildHs256Key(secret);
    return NimbusJwtDecoder.withSecretKey(key).build();
  }

  private SecretKey buildHs256Key(String secret) {
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(secret);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "shared.security.hs256.secret must be a Base64-encoded string", ex);
    }
    if (decoded.length < 32) {
      throw new IllegalArgumentException(
          "shared.security.hs256.secret must decode to at least 32 bytes");
    }
    return new SecretKeySpec(decoded, "HmacSHA256");
  }
}
