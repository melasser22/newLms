package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.model.SystemParameter;
import com.ejada.setup.service.SystemParameterService;
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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SystemParameterController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SystemParameterControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired SystemParameterService systemParameterService;

    @TestConfiguration
    static class MockConfig {
        @Bean SystemParameterService systemParameterService() { return mock(SystemParameterService.class); }
    }

    @Test
    void list_ok() throws Exception {
        BaseResponse<Page<SystemParameter>> resp = BaseResponse.success("ok", Page.<SystemParameter>empty());

        doReturn(resp)
                .when(systemParameterService)
                .list(any(Pageable.class), nullable(String.class), nullable(Boolean.class));

        mockMvc.perform(get("/setup/systemParameters").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("ok"));
    }

    @Test
    void get_notFound_translatesHttpStatus() throws Exception {
        BaseResponse<SystemParameter> resp = BaseResponse.error("ERR_PARAM_NOT_FOUND", "System parameter not found");

        doReturn(resp)
                .when(systemParameterService)
                .get(999);

        mockMvc.perform(get("/setup/systemParameters/{paramId}", 999).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR_PARAM_NOT_FOUND"));
    }
}
