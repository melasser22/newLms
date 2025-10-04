package com.ejada.starter_core.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.starter_core.config.CoreAutoConfiguration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class DefaultTenantResolverTest {

    private CoreAutoConfiguration.CoreProps.Tenant tenantProps;
    private InMemoryTenantDirectory directory;
    private DefaultTenantResolver resolver;

    @BeforeEach
    void setUp() {
        CoreAutoConfiguration.CoreProps props = new CoreAutoConfiguration.CoreProps();
        tenantProps = props.getTenant();
        tenantProps.setFailIfTenantMissing(false);
        tenantProps.setAllowHeaderResolution(true);
        tenantProps.setAllowSubdomainResolution(true);
        tenantProps.setAllowPathResolution(true);
        directory = new InMemoryTenantDirectory();
        resolver = new DefaultTenantResolver(tenantProps, directory);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesTenantFromJwtClaim() {
        UUID tenantId = UUID.randomUUID();
        directory.add(tenantId, "acme", true);
        setJwtAuthentication(Map.of("tenantId", tenantId.toString()));

        TenantResolution resolution = resolver.resolve(new MockHttpServletRequest());

        assertThat(resolution.hasTenant()).isTrue();
        assertThat(resolution.tenantId()).isEqualTo(tenantId.toString());
        assertThat(resolution.source()).isEqualTo(TenantSource.JWT);
    }

    @Test
    void rejectsHeaderResolutionWithoutApiKey() {
        UUID tenantId = UUID.randomUUID();
        directory.add(tenantId, "acme", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(tenantProps.getHeaderName(), tenantId.toString());

        TenantResolution resolution = resolver.resolve(request);

        assertThat(resolution.hasError()).isTrue();
        assertThat(resolution.error().httpStatus()).isEqualTo(401);
    }

    @Test
    void resolvesHeaderWhenApiKeyPresent() {
        UUID tenantId = UUID.randomUUID();
        directory.add(tenantId, "acme", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(tenantProps.getHeaderName(), tenantId.toString());
        request.addHeader("X-API-Key", "test");

        TenantResolution resolution = resolver.resolve(request);

        assertThat(resolution.hasTenant()).isTrue();
        assertThat(resolution.tenantId()).isEqualTo(tenantId.toString());
        assertThat(resolution.source()).isEqualTo(TenantSource.HEADER);
    }

    @Test
    void inactiveTenantIsForbidden() {
        UUID tenantId = UUID.randomUUID();
        directory.add(tenantId, "acme", false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(tenantProps.getHeaderName(), tenantId.toString());
        request.addHeader("X-API-Key", "test");

        TenantResolution resolution = resolver.resolve(request);

        assertThat(resolution.hasError()).isTrue();
        assertThat(resolution.error().httpStatus()).isEqualTo(403);
    }

    @Test
    void resolvesTenantFromSubdomain() {
        UUID tenantId = UUID.randomUUID();
        directory.add(tenantId, "acme", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("acme.example.com");

        TenantResolution resolution = resolver.resolve(request);

        assertThat(resolution.hasTenant()).isTrue();
        assertThat(resolution.tenantId()).isEqualTo(tenantId.toString());
        assertThat(resolution.source()).isEqualTo(TenantSource.SUBDOMAIN);
    }

    private void setJwtAuthentication(Map<String, Object> claims) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"), claims);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static final class InMemoryTenantDirectory implements TenantDirectory {

        private final Map<UUID, TenantRecord> byId = new HashMap<>();
        private final Map<String, TenantRecord> bySlug = new HashMap<>();

        void add(UUID id, String slug, boolean active) {
            TenantRecord record = new TenantRecord(id, slug != null ? slug.toLowerCase(Locale.ROOT) : null, active);
            byId.put(id, record);
            if (record.slug() != null) {
                bySlug.put(record.slug(), record);
            }
        }

        @Override
        public Optional<TenantRecord> findById(UUID tenantId) {
            return Optional.ofNullable(byId.get(tenantId));
        }

        @Override
        public Optional<TenantRecord> findBySlug(String slug) {
            if (slug == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(bySlug.get(slug.toLowerCase(Locale.ROOT)));
        }

        @Override
        public Optional<TenantRecord> findBySubdomain(String subdomain) {
            return findBySlug(subdomain);
        }
    }
}

