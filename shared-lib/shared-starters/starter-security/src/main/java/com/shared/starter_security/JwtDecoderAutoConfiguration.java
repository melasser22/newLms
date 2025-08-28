package com.shared.starter_security;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.spec.SecretKeySpec; import javax.crypto.SecretKey;
@AutoConfiguration
@EnableConfigurationProperties(SharedSecurityProps.class)
public class JwtDecoderAutoConfiguration {
  @Bean
  public JwtDecoder jwtDecoder(SharedSecurityProps props){
    if ("jwks".equalsIgnoreCase(props.getMode()) && props.getJwks().getUri()!=null) {
      return NimbusJwtDecoder.withJwkSetUri(props.getJwks().getUri()).build();
    }
    SecretKey key = new SecretKeySpec(props.getHs256().getSecret().getBytes(), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }
}
