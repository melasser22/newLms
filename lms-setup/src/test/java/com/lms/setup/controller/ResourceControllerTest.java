package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.dto.ResourceDto;
import com.lms.setup.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
    @MockBean ResourceService resourceService;

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
