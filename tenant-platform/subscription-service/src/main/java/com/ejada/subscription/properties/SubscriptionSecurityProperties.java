package com.ejada.subscription.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "subscription.security")
@Validated
public class SubscriptionSecurityProperties {

  private final Jwt jwt = new Jwt();
  @Valid private List<User> users = new ArrayList<>();

  public Jwt getJwt() {
    return jwt;
  }

  public List<User> getUsers() {
    return users;
  }

  public void setUsers(final List<User> users) {
    this.users = users == null ? new ArrayList<>() : new ArrayList<>(users);
  }

  public static class Jwt {
    @NotBlank
    @Size(min = 32, message = "subscription.security.jwt.secret must be at least 32 characters")
    private String secret;

    private Duration expiration = Duration.ofMinutes(30);

    public String getSecret() {
      return secret;
    }

    public void setSecret(final String secret) {
      this.secret = secret;
    }

    public Duration getExpiration() {
      return expiration;
    }

    public void setExpiration(final Duration expiration) {
      if (expiration != null) {
        this.expiration = expiration;
      }
    }
  }

  public static class User {
    @NotBlank private String loginName;

    @NotBlank
    @Pattern(regexp = "^[A-Fa-f0-9]{64}$", message = "password must be SHA-256 hex")
    private String password;

    public String getLoginName() {
      return loginName;
    }

    public void setLoginName(final String loginName) {
      this.loginName = loginName;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(final String password) {
      this.password = password;
    }
  }
}
