package com.ejada.setup.service;

import static com.ejada.testsupport.assertions.ResponseAssertions.assertThatBaseResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.SetupApplication;
import com.ejada.setup.dto.LookupResponse;
import com.ejada.setup.model.Lookup;
import com.ejada.setup.repository.LookupRepository;
import com.ejada.setup.service.impl.LookupServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = SetupApplication.class)
@Testcontainers
@ActiveProfiles("test")
class LookupServiceCachingIT {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "redis");
    }

    @MockBean
    private LookupRepository lookupRepository;

    @Autowired
    private LookupServiceImpl lookupService;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void flushRedis() {
        redisConnectionFactory.getConnection().serverCommands().flushAll();
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

        BaseResponse<List<LookupResponse>> first = lookupService.getAll();
        BaseResponse<List<LookupResponse>> second = lookupService.getAll();

        verify(lookupRepository, times(1)).findAll();
        assertThatBaseResponse(first)
                .isSuccess()
                .hasDataSatisfying(list -> assertThat(list).hasSize(1));
        assertThatBaseResponse(second).isSuccess();

        Object cached = redisTemplate.opsForValue().get("lookups:all::all");
        assertThat(cached).isInstanceOf(List.class);
    }
}
