package com.ejada.setup.service;

import static com.ejada.testsupport.assertions.ResponseAssertions.assertThatBaseResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.LookupResponse;
import com.ejada.setup.model.Lookup;
import com.ejada.setup.repository.LookupRepository;
import com.ejada.setup.service.impl.LookupServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.cache.annotation.EnableCaching;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LookupServiceCachingIT.CacheTestConfiguration.class)
class LookupServiceCachingIT {

    @Autowired
    private LookupService lookupService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private LookupRepository lookupRepository;

    @BeforeEach
    void flushRedis() {
        reset(lookupRepository);
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    void getAllCachesResponsesInRedis() {
        Lookup entity = Lookup.builder()
                .lookupItemId(1)
                .lookupItemCd("CODE")
                .lookupGroupCode("GENERAL")
                .lookupItemEnNm("General")
                .lookupItemArNm("عام")
                .isActive(true)
                .build();
        when(lookupRepository.findAll()).thenReturn(List.of(entity));

        BaseResponse<List<LookupResponse>> firstResponse = lookupService.getAll();
        BaseResponse<List<LookupResponse>> secondResponse = lookupService.getAll();

        verify(lookupRepository, times(1)).findAll();

        assertThatBaseResponse(firstResponse)
                .isSuccess()
                .hasDataSatisfying(list -> assertThat(list).hasSize(1));

        assertThatBaseResponse(secondResponse)
                .isSuccess()
                .hasDataSatisfying(list -> assertThat(list).hasSize(1));

        Cache cache = cacheManager.getCache("lookups:all");
        Object cached = cache != null ? cache.get("all", Object.class) : null;
        assertThat(cached).isInstanceOf(List.class);
    }

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class CacheTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("lookups:all", "lookups:byGroup");
        }

        @Bean
        LookupRepository lookupRepository() {
            return mock(LookupRepository.class);
        }

        @Bean
        LookupServiceImpl lookupService(final LookupRepository lookupRepository, final CacheManager cacheManager) {
            return new LookupServiceImpl(lookupRepository, cacheManager);
        }
    }
}
