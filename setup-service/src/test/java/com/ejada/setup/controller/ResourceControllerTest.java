package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.ResourceDto;
import com.ejada.setup.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ResourceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ResourceService resourceService;

    @TestConfiguration
    static class MockConfig {
        @Bean ResourceService resourceService() { return mock(ResourceService.class); }
    }

    @Test
    void list_ok() throws Exception {
        BaseResponse<Page<ResourceDto>> resp = BaseResponse.success("ok", Page.<ResourceDto>empty());

        doReturn(resp)
                .when(resourceService)
                .list(any(Pageable.class), nullable(String.class), anyBoolean());

        mockMvc.perform(get("/setup/resources").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("ok"));
    }
}
