package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.TestBase;
import com.ejada.setup.model.Country;
import com.ejada.setup.dto.CountryDto;
import com.ejada.setup.service.CountryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CountryController.class)
@WithMockUser(roles = {"ADMIN", "USER"})
class CountryControllerTest extends TestBase {

    @MockBean
    private CountryService countryService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void add_shouldCreateCountry_whenValidRequest() throws Exception {
        // Given
        CountryDto request = createTestCountryDto();
        Country country = createTestCountry();
        BaseResponse<Country> response = BaseResponse.success("Country created", country);
        when(countryService.add(any(CountryDto.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(postRequest("/countries", request))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Country created"))
                .andExpect(jsonPath("$.data.countryCd").value("US"));

        verify(countryService).add(any(CountryDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldUpdateCountry_whenValidRequest() throws Exception {
        // Given
        CountryDto request = createTestCountryDto();
        Country country = createTestCountry();
        country.setCountryId(1);
        BaseResponse<Country> response = BaseResponse.success("Country updated", country);
        when(countryService.update(eq(1), any(CountryDto.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(putRequest("/countries/1", request))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Country updated"));

        verify(countryService).update(eq(1), any(CountryDto.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void get_shouldReturnCountry_whenValidId() throws Exception {
        // Given
        Country country = createTestCountry();
        country.setCountryId(1);
        BaseResponse<Country> response = BaseResponse.success("Country", country);
        when(countryService.get(1)).thenReturn(response);

        // When & Then
        mockMvc.perform(getRequest("/countries/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.countryId").value(1));

        verify(countryService).get(1);
    }

    @Test
    @WithMockUser(roles = "USER")
    void list_shouldReturnPaginatedCountries_whenValidRequest() throws Exception {
        // Given
        List<Country> countries = Arrays.asList(createTestCountry());
        Page<Country> page = new PageImpl<>(countries, PageRequest.of(0, 20), 1);
        BaseResponse<?> response = BaseResponse.success("Countries page", page);  
      doReturn(response).when(countryService).list(any(Pageable.class), anyString(), anyBoolean());


        // When & Then
        mockMvc.perform(getRequest("/countries?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(countryService).list(any(Pageable.class), anyString(), anyBoolean());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listActive_shouldReturnActiveCountries() throws Exception {
        // Given
        List<Country> countries = Arrays.asList(createTestCountry());
        BaseResponse<List<Country>> response = BaseResponse.success("Active countries", countries);
        when(countryService.listActive()).thenReturn(response);

        // When & Then
        mockMvc.perform(getRequest("/countries/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray());

        verify(countryService).listActive();
    }

    @Test
    @WithMockUser(roles = "USER")
    void get_shouldReturn404_whenCountryNotFound() throws Exception {
        // Given
        BaseResponse<Country> response = BaseResponse.error("ERR_COUNTRY_NOT_FOUND", "Country not found");
        when(countryService.get(999)).thenReturn(response);

        // When & Then
        mockMvc.perform(getRequest("/countries/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("ERR_COUNTRY_NOT_FOUND"));

        verify(countryService).get(999);
    }

    @Test
    @WithMockUser(roles = "USER")
    void add_shouldReturn403_whenUserNotAdmin() throws Exception {
        // When & Then
        mockMvc.perform(postRequest("/countries", createTestCountryDto()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_shouldReturn403_whenUserNotAdmin() throws Exception {
        // When & Then
        mockMvc.perform(putRequest("/countries/1", createTestCountryDto()))
                .andExpect(status().isForbidden());
    }
}
