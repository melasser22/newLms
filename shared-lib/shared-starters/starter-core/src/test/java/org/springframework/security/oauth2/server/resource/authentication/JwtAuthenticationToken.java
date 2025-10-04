package org.springframework.security.oauth2.server.resource.authentication;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Minimal stub of Spring Security's {@code JwtAuthenticationToken} used for unit testing.
 */
public class JwtAuthenticationToken implements Authentication {

    private final Jwt token;
    private final Collection<? extends GrantedAuthority> authorities;
    private boolean authenticated = true;

    public JwtAuthenticationToken(Jwt token) {
        this(token, List.of());
    }

    public JwtAuthenticationToken(Jwt token, Collection<? extends GrantedAuthority> authorities) {
        this.token = Objects.requireNonNull(token, "token");
        this.authorities = authorities == null ? List.of() : List.copyOf(authorities);
    }

    public Jwt getToken() {
        return token;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getDetails() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return token.getTokenValue();
    }
}
