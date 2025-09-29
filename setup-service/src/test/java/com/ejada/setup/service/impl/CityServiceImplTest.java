package com.ejada.setup.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.CityDto;
import com.ejada.setup.mapper.CityMapper;
import com.ejada.setup.model.City;
import com.ejada.setup.model.Country;
import com.ejada.setup.repository.CityRepository;
import com.ejada.setup.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityServiceImplTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private CacheManager cacheManager;

    private CityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CityServiceImpl(cityRepository, countryRepository, new TestCityMapper(), cacheManager);
    }

    @Test
    void listShouldIncludeInactiveCitiesWhenNoFilterProvided() {
        City active = city(1, "C1", true);
        City inactive = city(2, "C2", false);
        Page<City> page = new PageImpl<>(List.of(active, inactive), PageRequest.of(0, 20), 2);

        when(cityRepository.findAll(any(Pageable.class))).thenReturn(page);

        BaseResponse<Page<CityDto>> response = service.list(PageRequest.of(0, 20), null, false);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getTotalElements()).isEqualTo(2);
        assertThat(response.getData().getContent())
                .extracting(CityDto::getIsActive)
                .containsExactlyInAnyOrder(Boolean.TRUE, Boolean.FALSE);

        verify(cityRepository).findAll(any(Pageable.class));
        verify(cityRepository, never()).findAll(ArgumentMatchers.<Specification<City>>any(), any(Pageable.class));
    }

    private static City city(int id, String code, boolean active) {
        Country country = new Country();
        country.setCountryId(99);

        City city = new City();
        city.setCityId(id);
        city.setCityCd(code);
        city.setCityEnNm("City " + code);
        city.setCityArNm("مدينة " + code);
        city.setCountry(country);
        city.setIsActive(active);
        return city;
    }

    private static final class TestCityMapper implements CityMapper {

        @Override
        public CityDto toDto(City entity) {
            CityDto dto = new CityDto();
            dto.setId(entity.getCityId());
            dto.setCityCd(entity.getCityCd());
            dto.setCityEnNm(entity.getCityEnNm());
            dto.setCityArNm(entity.getCityArNm());
            dto.setIsActive(entity.getIsActive());
            dto.setCountryId(entity.getCountry() != null ? entity.getCountry().getCountryId() : null);
            return dto;
        }

        @Override
        public City toEntity(CityDto dto) {
            City entity = new City();
            entity.setCityId(dto.getId());
            entity.setCityCd(dto.getCityCd());
            entity.setCityEnNm(dto.getCityEnNm());
            entity.setCityArNm(dto.getCityArNm());
            entity.setIsActive(dto.getIsActive());
            if (dto.getCountryId() != null) {
                Country country = new Country();
                country.setCountryId(dto.getCountryId());
                entity.setCountry(country);
            }
            return entity;
        }

        @Override
        public List<CityDto> toDtoList(List<City> entities) {
            return entities.stream().map(this::toDto).toList();
        }
    }
}

