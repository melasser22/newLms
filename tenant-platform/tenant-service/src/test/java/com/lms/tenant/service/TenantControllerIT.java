package com.lms.tenant.service;

import com.lms.tenant.service.dto.CreateTenantRequest;
import com.lms.tenant.service.dto.ToggleOverageRequest;
import com.lms.tenant.service.dto.TenantResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TenantControllerIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void toggleOverageFlag() {
        CreateTenantRequest create = new CreateTenantRequest("acme", "Acme Corp");
        ResponseEntity<TenantResponse> createResp = restTemplate.postForEntity(url("/api/tenants"), create, TenantResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID tenantId = createResp.getBody().id();

        ToggleOverageRequest toggle = new ToggleOverageRequest(true);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ToggleOverageRequest> toggleEntity = new HttpEntity<>(toggle, headers);
        ResponseEntity<Void> toggleResp = restTemplate.exchange(url("/api/tenants/" + tenantId + "/overage"), HttpMethod.PATCH, toggleEntity, Void.class);
        assertThat(toggleResp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<TenantResponse> getResp = restTemplate.getForEntity(url("/api/tenants/" + tenantId), TenantResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody().overageEnabled()).isTrue();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
