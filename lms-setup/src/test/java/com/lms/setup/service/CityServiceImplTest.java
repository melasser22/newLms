package com.lms.setup.service;

import com.common.dto.BaseResponse;
import com.lms.setup.dto.CityDto;
import com.lms.setup.mapper.CityMapper;
import com.lms.setup.model.City;
import com.lms.setup.repository.CityRepository;
import com.lms.setup.repository.CountryRepository;
import com.lms.setup.service.impl.CityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CityServiceImplTest {

    @Mock private CityRepository cityRepository;
    @Mock private CountryRepository countryRepository;
    @Mock private CityMapper mapper;

    @InjectMocks
    private CityServiceImpl service;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void list_ok() {
        var cityPage = new org.springframework.data.domain.PageImpl<>(Collections.singletonList(new City()));
        when(cityRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(cityPage);
        when(mapper.toDtoPage(cityPage)).thenReturn(org.springframework.data.domain.Page.<CityDto>empty());

        BaseResponse<Page<CityDto>> resp = service.list(org.springframework.data.domain.Pageable.unpaged(), null, false);
        assertNotNull(resp);
    }
}
