package com.lms.setup.service.impl;

import com.common.dto.BaseResponse;
import com.common.exception.ResourceNotFoundException;
import com.lms.setup.domain.CitySpecifications;
import com.lms.setup.dto.CityDto;
import com.lms.setup.mapper.CityMapper;
import com.lms.setup.model.City;
import com.lms.setup.model.Country;
import com.lms.setup.repository.CityRepository;
import com.lms.setup.repository.CountryRepository;
import com.lms.setup.service.CityService;
import com.shared.audit.starter.api.AuditAction;
import com.shared.audit.starter.api.DataClass;
import com.shared.audit.starter.api.annotations.Audited;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {
    private final CityRepository cityRepo;
    private final CountryRepository countryRepo;
    private final CityMapper mapper;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "cities", allEntries = true),
        @CacheEvict(cacheNames = "cities:byCountry", key = "#request.countryId")
    })
    public BaseResponse<CityDto> add(CityDto request) {
        Country country = countryRepo.findById(request.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + request.getCountryId()));

        City entity = mapper.toEntity(request);
        entity.setCountry(country);
        City saved = cityRepo.save(entity);
        return BaseResponse.success("City created", mapper.toDto(saved));
    }

    @Override
    @Transactional
    @Caching(
        put = { @CachePut(cacheNames = "cities", key = "#id") },
        evict = {
            // Evict old and new country caches if country was changed
            @CacheEvict(cacheNames = "cities:byCountry", key = "#result.body.countryId"),
            @CacheEvict(cacheNames = "cities:byCountry", key = "#request.countryId")
        }
    )
    public BaseResponse<CityDto> update(Integer id, CityDto request) {
        City city = cityRepo.findByIdWithCountry(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));

        // update mutable fields
        city.setCityCd(request.getCityCd());
        city.setCityEnNm(request.getCityEnNm());
        city.setCityArNm(request.getCityArNm());
        city.setIsActive(request.getIsActive());

        if (!city.getCountry().getCountryId().equals(request.getCountryId())) {
            Country country = countryRepo.findById(request.getCountryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Country not found with id: " + request.getCountryId()));
            city.setCountry(country);
        }

        City saved = cityRepo.save(city);
        return BaseResponse.success("City updated", mapper.toDto(saved));
    }

    @Override
    @Transactional
    public BaseResponse<Void> delete(Integer id) {
        City city = cityRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));

        // Evict caches before deleting
        evictCaches(city);

        cityRepo.deleteById(id);
        return BaseResponse.success("City deleted", null);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "cities", key = "#city.cityId"),
        @CacheEvict(cacheNames = "cities:byCountry", key = "#city.country.countryId")
    })
    public void evictCaches(City city) {
        // This method is a helper to allow @Caching annotations for the delete operation.
        // The actual logic is in the annotations.
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "cities", key = "#id")
    public BaseResponse<CityDto> get(Integer id) {
        City city = cityRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + id));
        return BaseResponse.success("OK", mapper.toDto(city));
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entity = "City", dataClass = DataClass.HEALTH, message = "List cities")
    public BaseResponse<?> list(Pageable pageable, String q) {
        final Pageable pg = (pageable == null ? Pageable.unpaged() : pageable);
        var spec = Specification.where(CitySpecifications.isActive()).and(CitySpecifications.nameContains(q));
        var page = cityRepo.findAll(spec, pg);
        return BaseResponse.success("Cities page", mapper.toDtoPage(page));
    }

    @Override
    @Transactional(readOnly = true)
    @Audited(action = AuditAction.READ, entity = "City", dataClass = DataClass.HEALTH, message = "List active cities by country")
    @Cacheable(cacheNames = "cities:byCountry", key = "#countryId")
    public BaseResponse<List<CityDto>> listActiveByCountry(Integer countryId) {
        if (!countryRepo.existsById(countryId)) {
            throw new ResourceNotFoundException("Country not found with id: " + countryId);
        }
        List<City> entities = cityRepo.findByCountry_CountryIdAndIsActiveTrueOrderByCityEnNmAsc(countryId);
        return BaseResponse.success("Active cities", mapper.toDtoList(entities));
    }
}
