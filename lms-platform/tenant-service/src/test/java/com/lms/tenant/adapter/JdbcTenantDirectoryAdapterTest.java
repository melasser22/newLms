package com.lms.tenant.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(JdbcTenantDirectoryAdapter.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driverClassName=org.h2.Driver"
})
class JdbcTenantDirectoryAdapterTest {

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Autowired
    JdbcTenantDirectoryAdapter adapter;

    @BeforeEach
    void setup() {
        jdbc.getJdbcOperations().execute("drop table if exists tenant");
        jdbc.getJdbcOperations().execute("create table tenant (tenant_id uuid primary key, tenant_slug text, domains text[])");
    }

    @Test
    void resolvesBySlugOrDomain() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into tenant(tenant_id, tenant_slug, domains) values(:id, :slug, ARRAY['acme.com'])",
                Map.of("id", id, "slug", "acme"));
        assertThat(adapter.resolveTenantIdBySlugOrDomain("acme")).isEqualTo(id);
        assertThat(adapter.resolveTenantIdBySlugOrDomain("acme.com")).isEqualTo(id);
    }

    @Test
    void returnsNullWhenNotFound() {
        assertThat(adapter.resolveTenantIdBySlugOrDomain("missing")).isNull();
    }
}
