package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.LookupResponse;
import com.ejada.setup.service.LookupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import static org.mockito.Mockito.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LookupController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LookupControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired LookupService lookupService;

  @TestConfiguration
  static class MockConfig {
    @Bean LookupService lookupService() { return mock(LookupService.class); }
  }

  @Test
  void getAllLookups_ok() throws Exception {
    BaseResponse<List<LookupResponse>> resp = BaseResponse.success("ok", List.of());
    when(lookupService.getAllLookups()).thenReturn(resp);

    mockMvc.perform(get("/setup/lookups").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("ok"));
  }
}
