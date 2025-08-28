package com.lms.setup.service;

import com.common.dto.BaseResponse;
import com.lms.setup.model.City;
import com.lms.setup.repository.CityRepository;
import com.lms.setup.service.impl.CityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class CityServiceImplTest {

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private CityServiceImpl service;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void list_ok() {
        when(cityRepository.findAll()).thenReturn(Collections.singletonList(new City()));
        BaseResponse<?> resp = service.list(org.springframework.data.domain.Pageable.unpaged(), null, false);
        assertNotNull(resp);
    }
}
