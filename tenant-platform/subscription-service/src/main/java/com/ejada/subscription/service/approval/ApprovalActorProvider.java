package com.ejada.subscription.service.approval;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves information about the authenticated administrator performing an approval action.
 */
@Component
public class ApprovalActorProvider {

    private static final String DEFAULT_USERNAME = "SYSTEM";

    public ApprovalActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String username = firstNonBlank(
                    claim(jwt, "preferred_username"),
                    claim(jwt, "upn"),
                    claim(jwt, "uid"),
                    jwt.getSubject(),
                    authentication.getName());
            String email = firstNonBlank(claim(jwt, "email"), claim(jwt, "mail"));
            String displayName = firstNonBlank(claim(jwt, "name"), username);
            return new ApprovalActor(defaultString(username), defaultString(displayName), email);
        }

        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            String username = authentication.getName();
            return new ApprovalActor(username, username, null);
        }

        return new ApprovalActor(DEFAULT_USERNAME, DEFAULT_USERNAME, null);
    }

    private String claim(final Jwt jwt, final String name) {
        if (jwt == null || !StringUtils.hasText(name)) {
            return null;
        }
        Object value = jwt.getClaim(name);
        if (value instanceof String str) {
            return str;
        }
        if (value instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(this::asString)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
        }
        return asString(value);
    }

    private String asString(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        return value.toString();
    }

    private String firstNonBlank(final String... values) {
        if (values == null) {
            return null;
        }
        return Stream.of(values)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private String defaultString(final String value) {
        return StringUtils.hasText(value) ? value : DEFAULT_USERNAME;
    }

    public record ApprovalActor(String username, String displayName, String email) {}
}
