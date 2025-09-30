package com.ejada.setup.service.impl;

import com.ejada.common.cache.CacheEvictionUtils;
import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.domain.CitySpecifications;
import com.ejada.setup.dto.CityDto;
import com.ejada.setup.mapper.CityMapper;
import com.ejada.setup.model.City;
import com.ejada.setup.model.Country;
import com.ejada.setup.repository.CityRepository;
import com.ejada.setup.repository.CountryRepository;
import com.ejada.setup.service.CityService;
import com.ejada.common.sort.SortUtils;
import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;
import com.ejada.common.exception.NotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {
        private final CityRepository cityRepo;
        private final CountryRepository countryRepo;
        private final CityMapper mapper;
        private final CacheManager cacheManager;

    @Override
        @Transactional
        public BaseResponse<CityDto> add(final CityDto request) {
                Country country = countryRepo.findById(request.getCountryId())
                                .orElseThrow(() -> new NotFoundException(
                                                "Country not found", String.valueOf(request.getCountryId())));

                City entity = mapper.toEntity(request);
                entity.setCountry(country);
                City saved = cityRepo.save(entity);
                CacheEvictionUtils.evict(cacheManager, "cities:byCountry", request.getCountryId());
                return BaseResponse.success("City created", mapper.toDto(saved));
        }

    @Override
        @Transactional
        public BaseResponse<CityDto> update(final Integer id, final CityDto request) {
                City city = cityRepo.findById(id)
                                .orElseThrow(() -> new NotFoundException("City not found", String.valueOf(id)));
                Integer oldCountryId = city.getCountry().getCountryId();

                city.setCityCd(request.getCityCd());
                city.setCityEnNm(request.getCityEnNm());
                city.setCityArNm(request.getCityArNm());
                city.setIsActive(request.getIsActive());

                if (!oldCountryId.equals(request.getCountryId())) {
                        Country country = countryRepo.findById(request.getCountryId())
                                        .orElseThrow(() -> new NotFoundException(
                                                        "Country not found", String.valueOf(request.getCountryId())));
                        city.setCountry(country);
                }

                City saved = cityRepo.save(city);
                CacheEvictionUtils.evict(cacheManager, "cities", id);
                CacheEvictionUtils.evict(cacheManager, "cities:byCountry", oldCountryId);
                CacheEvictionUtils.evict(cacheManager, "cities:byCountry", request.getCountryId());
                return BaseResponse.success("City updated", mapper.toDto(saved));
        }

    @Override
        @Transactional
        public BaseResponse<Void> delete(final Integer id) {
                City city = cityRepo.findById(id)
                                .orElse(null);
                if (city == null) {
                        return BaseResponse.error("CITY_NOT_FOUND", "City not found");
                }
                Integer countryId = city.getCountry().getCountryId();
                cityRepo.delete(city);
                CacheEvictionUtils.evict(cacheManager, "cities", id);
                CacheEvictionUtils.evict(cacheManager, "cities:byCountry", countryId);
                return BaseResponse.success("City deleted", null);
        }

    @Override
        @Transactional(Transactional.TxType.SUPPORTS)
        @Cacheable(cacheNames = "cities", key = "#id")
        public BaseResponse<CityDto> get(final Integer id) {
                City city = cityRepo.findById(id)
                                .orElseThrow(() -> new NotFoundException("City not found", String.valueOf(id)));
                return BaseResponse.success("OK", mapper.toDto(city));
        }

    @Override
        @Transactional(Transactional.TxType.SUPPORTS)
        @Audited(action = AuditAction.READ, entity = "City", dataClass = DataClass.HEALTH, message = "List cities")
        public BaseResponse<Page<CityDto>> list(final Pageable pageable, final String q, final boolean unpaged) {
                Sort sort = SortUtils.sanitize(pageable != null ? pageable.getSort() : Sort.unsorted(),
                                "cityEnNm", "cityArNm", "cityCd");
                final Pageable pg = (pageable == null || !pageable.isPaged()
                                ? Pageable.unpaged()
                                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));

                var spec = CitySpecifications.nameContains(q);

                if (unpaged) {
                        var entities = spec == null
                                        ? cityRepo.findAll(sort)
                                        : cityRepo.findAll(spec, sort);
                        return BaseResponse.success("City list", new PageImpl<>(mapper.toDtoList(entities)));
                }

                var page = spec == null
                                ? cityRepo.findAll(pg)
                                : cityRepo.findAll(spec, pg);
                return BaseResponse.success("Cities page", mapper.toDtoPage(page));
        }

    @Override
        @Transactional(Transactional.TxType.SUPPORTS)
        @Audited(action = AuditAction.READ, entity = "City", dataClass = DataClass.HEALTH, message = "List active cities by country")
        @Cacheable(cacheNames = "cities:byCountry", key = "#countryId")
        public BaseResponse<List<CityDto>> listActiveByCountry(final Integer countryId) {
                if (!countryRepo.existsById(countryId)) {
                        return BaseResponse.error("ERR_CITY_COUNTRY_NOT_FOUND", "Country not found");
                }
                List<City> entities = cityRepo.findByCountryCountryIdAndIsActiveTrueOrderByCityEnNmAsc(countryId);

                return BaseResponse.success("Active cities", mapper.toDtoList(entities));
        }
}
