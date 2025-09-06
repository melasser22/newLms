package com.ejada.catalog;

import com.ejada.common.dto.BaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ejada.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = {"ADMIN", "USER"})
public abstract class TestBase extends IntegrationTestSupport {
//
//    @Autowired
//    protected MockMvc mockMvc;
//
//    @Autowired
//    protected ObjectMapper objectMapper;
//
//    protected static final String BASE_URL = "/core/catalog";
//
//    @BeforeEach
//    void setUp() {
//        // Common catalog for all tests
//        // IntegrationTestSupport automatically provides Postgres and Redis containers
//    }
//
//    // Helper methods for creating test data
//    protected Country createTestCountry() {
//        Country country = new Country();
//        country.setCountryCd("US");
//        country.setCountryEnNm("United States");
//        country.setCountryArNm("الولايات المتحدة");
//        country.setDialingCode("+1");
//        country.setNationalityEn("American");
//        country.setNationalityAr("أمريكي");
//        country.setIsActive(true);
//        country.setEnDescription("United States of America");
//        country.setArDescription("الولايات المتحدة الأمريكية");
//        return country;
//    }
//
//    protected CountryDto createTestCountryDto() {
//        CountryDto dto = new CountryDto();
//        dto.setCountryCd("US");
//        dto.setCountryEnNm("United States");
//        dto.setCountryArNm("الولايات المتحدة");
//        dto.setDialingCode("+1");
//        dto.setNationalityEn("American");
//        dto.setNationalityAr("أمريكي");
//        dto.setIsActive(true);
//        dto.setEnDescription("United States of America");
//        dto.setArDescription("الولايات المتحدة الأمريكية");
//        return dto;
//    }
//
//    protected City createTestCity() {
//        City city = new City();
//        city.setCityCd("NYC");
//        city.setCityEnNm("New York");
//        city.setCityArNm("نيويورك");
//        city.setIsActive(true);
//        // Note: City doesn't have description fields, so we skip them
//        return city;
//    }
//
//    protected Lookup createTestLookup() {
//        Lookup lookup = new Lookup();
//        lookup.setLookupItemCd("ACTIVE");
//        lookup.setLookupItemEnNm("Active");
//        lookup.setLookupItemArNm("نشط");
//        lookup.setLookupGroupCode("STATUS");
//        lookup.setIsActive(true);
//        lookup.setItemEnDescription("Active status");
//        lookup.setItemArDescription("الحالة النشطة");
//        return lookup;
//    }
//
//    protected Resource createTestResource() {
//        Resource resource = new Resource();
//        resource.setResourceCd("USER_MGMT");
//        resource.setResourceEnNm("User Management");
//        resource.setResourceArNm("إدارة المستخدمين");
//        resource.setPath("/users");
//        resource.setHttpMethod("GET");
//        resource.setIsActive(true);
//        resource.setEnDescription("User management functionality");
//        resource.setArDescription("وظائف إدارة المستخدمين");
//        return resource;
//    }
//
//    protected SystemParameter createTestSystemParameter() {
//        SystemParameter param = new SystemParameter();
//        param.setParamKey("APP_VERSION");
//        param.setParamValue("1.0.0");
//        param.setParamGroup("SYSTEM");
//        param.setIsActive(true);
//        param.setDescription("Application version");
//        return param;
//    }
//
//    // Helper methods for building requests
//    protected MockHttpServletRequestBuilder getRequest(String url) {
//        return get(BASE_URL + url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON);
//    }
//
//    protected MockHttpServletRequestBuilder postRequest(String url, Object body) throws Exception {
//        return post(BASE_URL + url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(body));
//    }
//
//    protected MockHttpServletRequestBuilder putRequest(String url, Object body) throws Exception {
//        return put(BASE_URL + url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(body));
//    }
//
//    protected MockHttpServletRequestBuilder deleteRequest(String url) {
//        return delete(BASE_URL + url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON);
//    }
//
//    // Helper methods for assertions
//    protected <T> T parseResponse(String responseBody, Class<T> clazz) throws Exception {
//        return objectMapper.readValue(responseBody, clazz);
//    }
//
//    protected BaseResponse<?> parseBaseResponse(String responseBody) throws Exception {
//        return objectMapper.readValue(responseBody, BaseResponse.class);
//    }
//
//    // Test data builders
//    protected Map<String, Object> buildTestData() {
//        Map<String, Object> testData = new HashMap<>();
//        testData.put("timestamp", LocalDateTime.now().toString());
//        testData.put("testId", System.currentTimeMillis());
//        return testData;
//    }
}
