package com.ejada.setup.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.LookupResponse;
import com.ejada.setup.model.Lookup;
import com.ejada.setup.repository.LookupRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LookupServiceImplTest.TestConfig.class)
class LookupServiceImplTest {

  @Autowired private LookupServiceImpl service;
  @Autowired private LookupRepository lookupRepository;
  @Autowired private CacheManager cacheManager;

  @BeforeEach
  void clearCaches() {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
  }

  @AfterEach
  void resetMocks() {
    reset(lookupRepository);
  }

  @Test
  void getAllCachesRepositoryResults() {
    Lookup lookup = new Lookup();
    lookup.setLookupItemId(1);
    lookup.setLookupItemCd("CODE");
    lookup.setLookupGroupCode("GROUP");
    when(lookupRepository.findAll()).thenReturn(List.of(lookup));

    LookupResponse expected = new LookupResponse(1, "CODE", null, null, "GROUP", null, null, null, null);

    assertThat(service.getAll().getData()).containsExactly(expected);
    assertThat(service.getAll().getData()).containsExactly(expected);

    verify(lookupRepository, times(1)).findAll();
  }

  @Test
  void getAllReturnsErrorWhenRepositoryFails() {
    when(lookupRepository.findAll()).thenThrow(new IllegalStateException("boom"));

    BaseResponse<List<LookupResponse>> response = service.getAll();

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.getCode()).isEqualTo("ERR_LOOKUP_ALL");
  }

  @Test
  void getByGroupUsesCacheAndHandlesRepositoryExceptions() {
    Lookup lookup = new Lookup();
    lookup.setLookupItemId(1);
    lookup.setLookupItemCd("CODE");
    lookup.setLookupGroupCode("GROUP");
    when(lookupRepository.findByLookupGroupCodeAndIsActiveTrueOrderByLookupItemEnNmAsc("GROUP"))
        .thenReturn(List.of(lookup));

    LookupResponse expected = new LookupResponse(1, "CODE", null, null, "GROUP", null, null, null, null);

    BaseResponse<List<LookupResponse>> first = service.getByGroup("GROUP");
    BaseResponse<List<LookupResponse>> second = service.getByGroup("GROUP");
    assertThat(first.getData()).containsExactly(expected);
    assertThat(second.getData()).containsExactly(expected);
    verify(lookupRepository, times(1))
        .findByLookupGroupCodeAndIsActiveTrueOrderByLookupItemEnNmAsc("GROUP");

    doThrow(new RuntimeException("failure"))
        .when(lookupRepository)
        .findByLookupGroupCodeAndIsActiveTrueOrderByLookupItemEnNmAsc("ERR");

    BaseResponse<List<LookupResponse>> error = service.getByGroup("ERR");
    assertThat(error.isSuccess()).isFalse();
    assertThat(error.getCode()).isEqualTo("ERR_LOOKUP_GROUP");
  }

  @Configuration
  @EnableCaching(proxyTargetClass = true)
  static class TestConfig {
    @Bean
    LookupRepository lookupRepository() {
      return Mockito.mock(LookupRepository.class);
    }

    @Bean
    LookupServiceImpl lookupServiceImpl(LookupRepository repository, CacheManager cacheManager) {
      return new LookupServiceImpl(repository, cacheManager);
    }

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("lookups:all", "lookups:byGroup");
    }
  }
}
