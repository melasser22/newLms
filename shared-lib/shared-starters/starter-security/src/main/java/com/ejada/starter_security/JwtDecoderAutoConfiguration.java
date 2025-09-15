package com.ejada.starter_security;

import java.nio.charset.StandardCharsets;
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
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }
}
