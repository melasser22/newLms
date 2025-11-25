package com.ejada.crypto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import javax.crypto.SecretKey;

@AutoConfiguration
@EnableConfigurationProperties(JwtTokenProperties.class)
public class JwtTokenAutoConfiguration {

    @Bean
    public SecretKey jwtSigningKey(JwtTokenProperties props) {
        return JwtTokenService.createKey(props.getSecret());
    }

    @Bean
    public JwtTokenService jwtTokenService(SecretKey jwtSigningKey, JwtTokenProperties props) {
        return new JwtTokenService(jwtSigningKey, props.getTokenPeriod());
    }
}
