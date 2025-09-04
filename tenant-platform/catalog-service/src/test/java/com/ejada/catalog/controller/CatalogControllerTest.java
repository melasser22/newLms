package com.ejada.catalog.controller;

import com.ejada.catalog.service.CatalogService;
import com.ejada.catalog.service.FeaturePolicyPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CatalogController.class, properties = "shared.security.resource-server.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
class CatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    CatalogService service;

    @Test
    void returnsEffectiveFeature() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(service.effective("tier", tenantId, "feat"))
                .thenReturn(new FeaturePolicyPort.EffectiveFeature(true, 100L, false, null, null));

        mvc.perform(get("/catalog/effective")
                .param("tierId", "tier")
                .param("tenantId", tenantId.toString())
                .param("featureKey", "feat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.limit").value(100));
    }
}
