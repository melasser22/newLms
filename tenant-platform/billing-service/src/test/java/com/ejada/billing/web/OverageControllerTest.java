package com.ejada.billing.web;

import com.ejada.billing.controller.OverageController;
import com.ejada.billing.dto.OverageResponse;
import com.ejada.billing.dto.RecordOverageRequest;
import com.ejada.billing.service.BillingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web MVC slice test for {@link OverageController}. The underlying service is
 * mocked so that the controller can be tested without a running database or
 * other infrastructure.
 */
@WebMvcTest(controllers = OverageController.class)
class OverageControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    BillingService service;

    @Test
    void postOverageDelegatesToService() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        String idem = UUID.randomUUID().toString();

        RecordOverageRequest req = new RecordOverageRequest(
                "feature", 5L, 10L, "USD", Instant.now(),
                Instant.now().minusSeconds(60), Instant.now(), idem, Map.of("k", "v"));

        OverageResponse resp = new OverageResponse(
                UUID.randomUUID(), tenantId, "feature", 5L, 10L, "USD",
                Instant.now(), Instant.now(), Instant.now(), "RECORDED");
        when(service.record(eq(tenantId), eq(subscriptionId), any())).thenReturn(resp);

        String json = mapper.writeValueAsString(req);
        String url = "/tenants/" + tenantId + "/billing/overages";

        var first = mvc.perform(post(url)
                        .param("subscriptionId", subscriptionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var second = mvc.perform(post(url)
                        .param("subscriptionId", subscriptionId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(first).isEqualTo(second);
        verify(service, times(2)).record(eq(tenantId), eq(subscriptionId), any());
    }
}
