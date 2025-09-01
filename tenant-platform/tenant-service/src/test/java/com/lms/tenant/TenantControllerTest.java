package com.lms.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.flyway.enabled=false"
})
class TenantControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Autowired
    ObjectMapper mapper;

    @BeforeEach
    void setup() {
        jdbc.getJdbcOperations().execute("drop table if exists tenant");
        jdbc.getJdbcOperations().execute("create table tenant (tenant_id uuid primary key, tenant_slug text, name text, status text, overage_enabled boolean not null)");
    }

    @Test
    void togglesOverageAndReadsBack() throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into tenant (tenant_id, tenant_slug, name, status, overage_enabled) values (:id,:slug,:name,'ACTIVE',false)",
                Map.of("id", id, "slug", "acme", "name", "Acme"));

        mockMvc.perform(put("/tenants/" + id + "/overage-enabled")
                        .header("X-Tenant-ID", "acme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("enabled", true))))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/tenants/" + id).header("X-Tenant-ID", "acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overageEnabled").value(true));
    }
}
