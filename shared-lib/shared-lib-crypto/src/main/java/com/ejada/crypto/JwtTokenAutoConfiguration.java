package com.ejada.crypto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(JwtTokenProperties.class)
public class JwtTokenAutoConfiguration {

    @Bean
    public JwtTokenService jwtTokenService(JwtTokenProperties props) {
        return JwtTokenService.withSecret(props.getSecret(), props.getTokenPeriod());
    }
}
