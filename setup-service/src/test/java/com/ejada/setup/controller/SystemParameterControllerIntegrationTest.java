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
import com.ejada.starter_security.Role;
import com.ejada.starter_security.RoleChecker;
import com.ejada.starter_security.SharedSecurityProps;
import com.ejada.starter_security.authorization.AuthorizationExpressions;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringJUnitConfig(SystemParameterControllerIntegrationTest.TestConfig.class)
class SystemParameterControllerIntegrationTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    TestSystemParameterService testSystemParameterService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    MappingJackson2HttpMessageConverter jacksonMessageConverter;

    MockMvc mockMvc;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        testSystemParameterService.reset();

        mockMvc = MockMvcBuilders.standaloneSetup(
                applicationContext.getBean(SystemParameterController.class))
            .setMessageConverters(jacksonMessageConverter)
            .build();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "test-user",
                "N/A",
                List.of(new SimpleGrantedAuthority(Role.EJADA_OFFICER.getAuthority()))
            )
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
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

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import(SystemParameterController.class)
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("sysparams:byKeys");
        }

        @Bean
        TestSystemParameterService systemParameterService() {
            return new TestSystemParameterService();
        }

        @Bean
        SharedSecurityProps sharedSecurityProps() {
            SharedSecurityProps props = new SharedSecurityProps();
            props.setEnableRoleCheck(false);
            props.getHs256().setSecret("test-secret");
            return props;
        }

        @Bean
        MethodSecurityExpressionHandler methodSecurityExpressionHandler(ApplicationContext applicationContext) {
            DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
            handler.setApplicationContext(applicationContext);
            return handler;
        }

        @Bean
        RoleChecker roleChecker(SharedSecurityProps sharedSecurityProps) {
            return new RoleChecker(sharedSecurityProps);
        }

        @Bean
        AuthorizationExpressions authorizationExpressions(RoleChecker roleChecker) {
            return new AuthorizationExpressions(roleChecker);
        }

        @Bean
        ObjectMapper objectMapper() {
            return Jackson2ObjectMapperBuilder.json().build();
        }

        @Bean
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
            return new MappingJackson2HttpMessageConverter(objectMapper);
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
