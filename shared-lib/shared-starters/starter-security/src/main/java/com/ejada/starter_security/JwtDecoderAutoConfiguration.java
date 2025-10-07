package com.ejada.starter_security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@AutoConfiguration
@EnableConfigurationProperties(SharedSecurityProps.class)
public class JwtDecoderAutoConfiguration {

  @Bean
  public JwtDecoder jwtDecoder(SharedSecurityProps props,
                               TenantAwareJwtValidator tenantAwareJwtValidator) {
    if ("jwks".equalsIgnoreCase(props.getMode())) {
      String uri = props.getJwks().getUri();
      Assert.hasText(uri, "shared.security.jwks.uri must not be null or empty when mode=jwks");
      NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(uri).build();
      decoder.setJwtValidator(buildValidators(props, tenantAwareJwtValidator));
      return decoder;
    }

    String secret = props.getHs256().getSecret();
    Assert.hasText(secret, "shared.security.hs256.secret must not be null or empty when mode=hs256");
    SecretKey key = new SecretKeySpec(resolveHs256Secret(secret), "HmacSHA256");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
    decoder.setJwtValidator(buildValidators(props, tenantAwareJwtValidator));
    return decoder;
  }

  private DelegatingOAuth2TokenValidator<Jwt> buildValidators(SharedSecurityProps props,
                                                              TenantAwareJwtValidator tenantAwareJwtValidator) {
    List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
    validators.add(JwtValidators.createDefault());
    if (StringUtils.hasText(props.getIssuer())) {
      validators.add(JwtValidators.createDefaultWithIssuer(props.getIssuer()));
    }
    if (StringUtils.hasText(props.getAudience())) {
      validators.add(new JwtClaimValidator<List<String>>(OAuth2ParameterNames.AUDIENCE,
          aud -> aud != null && aud.contains(props.getAudience())));
    }
    validators.add(tenantAwareJwtValidator);
    return new DelegatingOAuth2TokenValidator<>(validators);
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
