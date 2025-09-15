package com.ejada.crypto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties("shared.security.jwt")
public class JwtTokenProperties {

    @NotBlank
    private String secret;

    @NotNull
    private Duration tokenPeriod;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getTokenPeriod() {
        return tokenPeriod;
    }

    public void setTokenPeriod(Duration tokenPeriod) {
        this.tokenPeriod = tokenPeriod;
    }
}
