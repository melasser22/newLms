package com.ejada.testsupport.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Convenience utilities for creating signed JWT tokens in integration tests.
 *
 * <p>The tokens generated here use the HS256 algorithm and can be customised with
 * roles, scopes, tenant and arbitrary custom claims so that security filters in
 * resource-server tests can exercise the full validation path.</p>
 */
public final class JwtTestTokens {

    private JwtTestTokens() {
    }

    /**
     * Start building an HS256 signed JWT using the provided shared secret.
     *
     * @param secret shared secret (will be padded to 32 bytes if needed)
     * @return fluent builder for configuring claims
     */
    public static Hs256Builder hs256(String secret) {
        return new Hs256Builder(secret);
    }

    /**
     * Fluent builder for HS256 JWT tokens.
     */
    public static final class Hs256Builder {
        private final byte[] secret;
        private String subject = "test-user";
        private String issuer = "test-issuer";
        private Instant issuedAt = Instant.now();
        private Instant expiresAt = Instant.now().plusSeconds(3600);
        private Collection<String> roles = List.of();
        private String scope;
        private String tenant;
        private Collection<String> audience = List.of();
        private final Map<String, Object> additionalClaims = new LinkedHashMap<>();

        private Hs256Builder(String secret) {
            this.secret = normaliseSecret(secret);
        }

        public Hs256Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Hs256Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Hs256Builder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Hs256Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Hs256Builder roles(Collection<String> roles) {
            if (roles == null) {
                this.roles = List.of();
            } else {
                this.roles = roles.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(ArrayList::new));
            }
            return this;
        }

        public Hs256Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Hs256Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public Hs256Builder audience(Collection<String> audience) {
            if (audience == null) {
                this.audience = List.of();
            } else {
                this.audience = audience.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(ArrayList::new));
            }
            return this;
        }

        public Hs256Builder claim(String name, Object value) {
            if (name != null && !name.isBlank()) {
                additionalClaims.put(name, value);
            }
            return this;
        }

        /**
         * Serialize the configured token using HS256 signature.
         *
         * @return compact JWT representation (suitable for Bearer token header)
         */
        public String build() {
            try {
                JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                        .subject(subject)
                        .issuer(issuer)
                        .issueTime(Date.from(issuedAt))
                        .expirationTime(Date.from(expiresAt));

                if (!audience.isEmpty()) {
                    builder.audience(new ArrayList<>(audience));
                }
                if (tenant != null && !tenant.isBlank()) {
                    builder.claim("tenant", tenant);
                }
                if (roles != null && !roles.isEmpty()) {
                    builder.claim("roles", new ArrayList<>(roles));
                }
                if (scope != null && !scope.isBlank()) {
                    builder.claim("scope", scope);
                }
                additionalClaims.forEach(builder::claim);

                SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
                jwt.sign(new MACSigner(secret));
                return jwt.serialize();
            } catch (JOSEException ex) {
                throw new IllegalStateException("Failed to sign test JWT", ex);
            }
        }
    }

    private static byte[] normaliseSecret(String secret) {
        byte[] raw = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        byte[] padded = new byte[32];
        System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, 32));
        if (raw.length == 0) {
            // ensure deterministic secret when empty input provided
            for (int i = 0; i < padded.length; i++) {
                padded[i] = (byte) (i + 1);
            }
        }
        return padded;
    }
}
