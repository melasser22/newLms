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
@Import(JdbcTenantSettingsAdapter.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driverClassName=org.h2.Driver"
})
class JdbcTenantSettingsAdapterTest {

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Autowired
    JdbcTenantSettingsAdapter adapter;

    @BeforeEach
    void setup() {
        jdbc.getJdbcOperations().execute("drop table if exists tenant");
        jdbc.getJdbcOperations().execute("create table tenant (tenant_id uuid primary key, overage_enabled boolean not null)");
    }

    @Test
    void togglesFlag() {
        UUID id = UUID.randomUUID();
        adapter.setOverageEnabled(id, true);
        assertThat(adapter.isOverageEnabled(id)).isTrue();
        adapter.setOverageEnabled(id, false);
        assertThat(adapter.isOverageEnabled(id)).isFalse();
    }

    @Test
    void insertsWhenMissing() {
        UUID id = UUID.randomUUID();
        adapter.setOverageEnabled(id, true);
        Integer count = jdbc.queryForObject("select count(*) from tenant where tenant_id=:id", Map.of("id", id), Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
