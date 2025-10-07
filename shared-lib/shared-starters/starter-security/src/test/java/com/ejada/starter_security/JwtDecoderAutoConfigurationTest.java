package com.ejada.starter_security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

class JwtDecoderAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(JwtDecoderAutoConfiguration.class))
      .withUserConfiguration(TestTenantValidatorConfig.class);

  private static final String BASE64_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

  @Test
  void missingSecretThrowsMeaningfulException() {
    SharedSecurityProps props = new SharedSecurityProps();
    JwtDecoderAutoConfiguration configuration = new JwtDecoderAutoConfiguration();
    TenantAwareJwtValidator validator = new TenantAwareJwtValidator(props, null);

    assertThatThrownBy(() -> configuration.jwtDecoder(props, validator))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shared.security.hs256.secret");
  }

  @Test
  void missingJwksUriThrowsMeaningfulException() {
    SharedSecurityProps props = new SharedSecurityProps();
    props.setMode("jwks");
    JwtDecoderAutoConfiguration configuration = new JwtDecoderAutoConfiguration();
    TenantAwareJwtValidator validator = new TenantAwareJwtValidator(props, null);

    assertThatThrownBy(() -> configuration.jwtDecoder(props, validator))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shared.security.jwks.uri");
  }

  @Test
  void hs256SecretAcceptsBase64EncodedValues() {
    contextRunner.withPropertyValues("shared.security.hs256.secret=" + BASE64_SECRET)
        .run(context -> {
          assertThat(context).hasNotFailed();
          JwtDecoder decoder = context.getBean(JwtDecoder.class);
          try {
            String token = createHs256Token(BASE64_SECRET, "alice");
            Jwt jwt = decoder.decode(token);
            assertThat(jwt.getSubject()).isEqualTo("alice");
          } catch (JOSEException e) {
            fail("Failed to create test token", e);
          }
        });
  }

  @Test
  void legacyJwtSecretPropertyIsBackwardsCompatible() {
    contextRunner.withPropertyValues("shared.security.jwt.secret=" + BASE64_SECRET)
        .run(context -> {
          assertThat(context).hasNotFailed();
          JwtDecoder decoder = context.getBean(JwtDecoder.class);
          try {
            String token = createHs256Token(BASE64_SECRET, "legacy-user");
            Jwt jwt = decoder.decode(token);
            assertThat(jwt.getSubject()).isEqualTo("legacy-user");
          } catch (JOSEException e) {
            fail("Failed to create test token", e);
          }
        });
  }

  private static String createHs256Token(String base64Secret, String subject) throws JOSEException {
    byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
    JWSSigner signer = new MACSigner(keyBytes);
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(subject)
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plusSeconds(60)))
        .build();
    SignedJWT signed = new SignedJWT(new com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256), claims);
    signed.sign(signer);
    return signed.serialize();
  }

  @Configuration
  static class TestTenantValidatorConfig {
    @Bean
    TenantAwareJwtValidator tenantAwareJwtValidator(SharedSecurityProps props) {
      return new TenantAwareJwtValidator(props, null);
    }
  }
}
