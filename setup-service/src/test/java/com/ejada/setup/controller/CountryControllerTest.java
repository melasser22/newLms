package com.ejada.setup.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.CountryDto;
import com.ejada.setup.service.CountryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.ejada.starter_security.RoleChecker;
import com.ejada.starter_security.SharedSecurityProps;
import com.ejada.starter_core.web.GlobalExceptionHandler;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CountryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(roles = {"ADMIN", "USER"})
@Import({CountryControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
@ImportAutoConfiguration(AopAutoConfiguration.class)
class CountryControllerTest {

    private static final String BASE_URL = "/setup/countries";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired CountryService countryService;

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {
        @Bean SharedSecurityProps sharedSecurityProps() { return new SharedSecurityProps(); }
        @Bean RoleChecker roleChecker(SharedSecurityProps props) { return new RoleChecker(props); }
        @Bean CountryService countryService() { return mock(CountryService.class); }
    }

    private CountryDto createTestCountryDto() {
        CountryDto dto = new CountryDto();
        dto.setCountryCd("US");
        dto.setCountryEnNm("United States");
        dto.setCountryArNm("الولايات المتحدة");
        dto.setDialingCode("+1");
        dto.setNationalityEn("American");
        dto.setNationalityAr("أمريكي");
        dto.setIsActive(true);
        dto.setEnDescription("United States of America");
        dto.setArDescription("الولايات المتحدة الأمريكية");
        return dto;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void add_shouldCreateCountry_whenValidRequest() throws Exception {
        CountryDto request = createTestCountryDto();
        CountryDto responseBody = createTestCountryDto();
        responseBody.setCountryId(1);
        BaseResponse<CountryDto> response = BaseResponse.success("Country created", responseBody);
        when(countryService.add(any(CountryDto.class))).thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
        CountryDto request = createTestCountryDto();
        CountryDto responseBody = createTestCountryDto();
        responseBody.setCountryId(1);
        BaseResponse<CountryDto> response = BaseResponse.success("Country updated", responseBody);
        when(countryService.update(eq(1), any(CountryDto.class))).thenReturn(response);

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Country updated"));

        verify(countryService).update(eq(1), any(CountryDto.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void get_shouldReturnCountry_whenValidId() throws Exception {
        CountryDto country = createTestCountryDto();
        country.setCountryId(1);
        BaseResponse<CountryDto> response = BaseResponse.success("Country", country);
        when(countryService.get(1)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.countryId").value(1));

        verify(countryService).get(1);
    }

    @Test
    @WithMockUser(roles = "USER")
    void list_shouldReturnPaginatedCountries_whenValidRequest() throws Exception {
        List<CountryDto> countries = Arrays.asList(createTestCountryDto());
        Page<CountryDto> page = new PageImpl<>(countries, PageRequest.of(0, 20), 1);
        BaseResponse<?> response = BaseResponse.success("Countries page", page);
        doReturn(response).when(countryService).list(any(Pageable.class), nullable(String.class), anyBoolean());

        mockMvc.perform(get(BASE_URL + "?page=0&size=20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(countryService).list(any(Pageable.class), nullable(String.class), anyBoolean());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listActive_shouldReturnActiveCountries() throws Exception {
        List<CountryDto> countries = Arrays.asList(createTestCountryDto());
        BaseResponse<List<CountryDto>> response = BaseResponse.success("Active countries", countries);
        when(countryService.listActive()).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/active")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray());

        verify(countryService).listActive();
    }

    @Test
    @WithMockUser(roles = "USER")
    void get_shouldReturn404_whenCountryNotFound() throws Exception {
        BaseResponse<CountryDto> response = BaseResponse.error("ERR_COUNTRY_NOT_FOUND", "Country not found");
        when(countryService.get(999)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("ERR_COUNTRY_NOT_FOUND"));

        verify(countryService).get(999);
    }

    @Test
    @WithMockUser(roles = "USER")
    void add_shouldReturn403_whenUserNotAdmin() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTestCountryDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_shouldReturn403_whenUserNotAdmin() throws Exception {
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTestCountryDto())))
                .andExpect(status().isForbidden());
    }
}
