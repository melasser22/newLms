package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Lookup;
import com.lms.setup.service.LookupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LookupController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LookupControllerTest {

  @Autowired MockMvc mockMvc;
  @MockBean LookupService lookupService;

  @Test
  void getAllLookups_ok() throws Exception {
    BaseResponse<List<Lookup>> resp = BaseResponse.success("ok", List.of());
    when(lookupService.getAllLookups()).thenReturn(resp);

    mockMvc.perform(get("/setup/lookups").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("ok"));
  }
}
