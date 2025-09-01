package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.model.SystemParameter;
import com.lms.setup.service.SystemParameterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SystemParameterControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean SystemParameterService systemParameterService;

    @Test
    void list_ok() throws Exception {
        BaseResponse<Page<SystemParameter>> resp = BaseResponse.success("ok", Page.<SystemParameter>empty());

        doReturn(resp)
                .when(systemParameterService)
                .list(any(Pageable.class), nullable(String.class), nullable(Boolean.class));

        mockMvc.perform(get("/setup/system-parameters").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("ok"));
    }
}
