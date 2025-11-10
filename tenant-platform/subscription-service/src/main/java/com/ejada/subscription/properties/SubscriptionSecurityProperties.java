package com.ejada.subscription.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "subscription.security")
@Validated
public record SubscriptionSecurityProperties(
    @Valid Jwt jwt,
    @Valid List<User> users) {

  public SubscriptionSecurityProperties {
    Objects.requireNonNull(jwt, "subscription.security.jwt must be configured");
    users = users == null ? List.of() : List.copyOf(users);
  }

  public record Jwt(
      @NotBlank
      @Size(min = 32, message = "subscription.security.jwt.secret must be at least 32 characters")
      String secret,
      Duration expiration) {

    public Jwt {
      expiration = expiration == null ? Duration.ofMinutes(30) : expiration;
    }
  }

  public record User(
      @NotBlank String loginName,
      @NotBlank
      @Pattern(regexp = "^[A-Fa-f0-9]{64}$", message = "password must be SHA-256 hex")
      String password) { }
}
