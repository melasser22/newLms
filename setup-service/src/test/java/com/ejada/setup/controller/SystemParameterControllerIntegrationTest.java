package com.ejada.setup.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.SystemParameterRequest;
import com.ejada.setup.dto.SystemParameterResponse;
import com.ejada.setup.service.SystemParameterService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = SystemParameterControllerIntegrationTest.TestApp.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemParameterControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestSystemParameterService testSystemParameterService;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        testSystemParameterService.reset();
    }

    @Test
    void getByKeysCachesResponses() throws Exception {
        mockMvc.perform(post("/setup/systemParameters/by-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"alpha\"]"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/setup/systemParameters/by-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"alpha\"]"))
            .andExpect(status().isOk());

        assertThat(testSystemParameterService.getByKeysInvocationCount()).isEqualTo(1);
        Cache cache = cacheManager.getCache("sysparams:byKeys");
        assertThat(cache).isNotNull();
        assertThat(cache.get(List.of("alpha"))).isNotNull();
    }

    @Test
    void getByKeysMapsErrorStatusFromResponse() throws Exception {
        mockMvc.perform(post("/setup/systemParameters/by-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"missing\"]"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(ErrorCodes.DATA_NOT_FOUND));
    }

    @EnableCaching(proxyTargetClass = true)
    // Exclude security auto-configuration to avoid loading the full resource server stack
    // when only verifying controller/caching behaviour.
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        com.ejada.starter_security.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    })
    @Import(SystemParameterController.class)
    static class TestApp {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("sysparams:byKeys");
        }

        @Bean
        TestSystemParameterService systemParameterService() {
            return new TestSystemParameterService();
        }
    }

    static class TestSystemParameterService implements SystemParameterService {

        private final AtomicInteger byKeysInvocationCount = new AtomicInteger();

        @Override
        public BaseResponse<SystemParameterResponse> add(SystemParameterRequest request) {
            throw new UnsupportedOperationException("Not needed for test");
        }

        @Override
        public BaseResponse<SystemParameterResponse> update(Integer paramId, SystemParameterRequest request) {
            throw new UnsupportedOperationException("Not needed for test");
        }

        @Override
        public BaseResponse<SystemParameterResponse> get(Integer paramId) {
            throw new UnsupportedOperationException("Not needed for test");
        }

        @Override
        public BaseResponse<org.springframework.data.domain.Page<SystemParameterResponse>> list(
            org.springframework.data.domain.Pageable pageable, String group, Boolean onlyActive) {
            throw new UnsupportedOperationException("Not needed for test");
        }

        @Override
        @Cacheable(cacheNames = "sysparams:byKeys", key = "#keys")
        public BaseResponse<List<SystemParameterResponse>> getByKeys(List<String> keys) {
            byKeysInvocationCount.incrementAndGet();
            if (keys.contains("missing")) {
                return BaseResponse.error(ErrorCodes.DATA_NOT_FOUND, "Parameter not found");
            }
            SystemParameterResponse response = new SystemParameterResponse();
            response.setParamKey("alpha");
            response.setParamValue("42");
            response.setIsActive(Boolean.TRUE);
            return BaseResponse.success("Parameters", List.of(response));
        }

        @Override
        public BaseResponse<SystemParameterResponse> getByKey(String paramKey) {
            throw new UnsupportedOperationException("Not needed for test");
        }

        int getByKeysInvocationCount() {
            return byKeysInvocationCount.get();
        }

        void reset() {
            byKeysInvocationCount.set(0);
        }
    }
}
