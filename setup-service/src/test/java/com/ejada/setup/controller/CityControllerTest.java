package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.CityDto;
import com.ejada.setup.service.CityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CityController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CityControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired CityService cityService;

  @TestConfiguration
  static class MockConfig {
    @Bean CityService cityService() { return Mockito.mock(CityService.class); }
  }

  @Test
  void list_ok() throws Exception {
    BaseResponse<Page<CityDto>> resp = BaseResponse.success("ok", Page.<CityDto>empty());

    doReturn(resp)
        .when(cityService)
        .list(any(Pageable.class), nullable(String.class), anyBoolean());

    mockMvc.perform(get("/setup/cities").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("ok"));
  }

  @Test
  void getCities_ok() throws Exception {
    BaseResponse<Page<CityDto>> resp = BaseResponse.success("ok", Page.<CityDto>empty());

    // Same broad stub covers q = null and any pageable/flag
    doReturn(resp)
        .when(cityService)
        .list(any(Pageable.class), nullable(String.class), anyBoolean());

    mockMvc.perform(get("/setup/cities").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("ok"));
  }
}
