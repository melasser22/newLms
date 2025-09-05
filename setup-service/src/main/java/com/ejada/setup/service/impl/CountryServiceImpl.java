package com.ejada.setup.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.model.Country;
import com.ejada.setup.dto.CountryDto;
import com.ejada.setup.repository.CountryRepository;
import com.ejada.setup.service.CountryService;
import com.ejada.common.sort.SortUtils;
import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
@Transactional
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    public CountryServiceImpl(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "Country", dataClass = DataClass.HEALTH, message = "Create country")
    @Caching(evict = {
            @CacheEvict(cacheNames = "countries", key = "#result.data.countryId", condition = "#result.isSuccess()"),
            @CacheEvict(cacheNames = "countries:active", key = "'active'")
    })
    public BaseResponse<Country> add(CountryDto request) {
        if (countryRepository.existsByCountryCdIgnoreCase(request.getCountryCd())) {
            return BaseResponse.error("ERR_COUNTRY_DUP_CD", "Country code already exists");
        }
        Country entity = new Country();
        entity.setCountryCd(request.getCountryCd());
        entity.setCountryEnNm(request.getCountryEnNm());
        entity.setCountryArNm(request.getCountryArNm());
        entity.setDialingCode(request.getDialingCode());
        entity.setNationalityEn(request.getNationalityEn());
        entity.setNationalityAr(request.getNationalityAr());
        entity.setIsActive(request.getIsActive());
        entity.setEnDescription(request.getEnDescription());
        entity.setArDescription(request.getArDescription());

        Country saved = countryRepository.save(entity);
        return BaseResponse.success("Country created", saved);
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Country", dataClass = DataClass.HEALTH, message = "Update country")
    @Caching(evict = {
            @CacheEvict(cacheNames = "countries", key = "#countryId"),
            @CacheEvict(cacheNames = "countries:active", key = "'active'")
    })
    public BaseResponse<Country> update(Integer countryId, CountryDto request) {
        Country existing = countryRepository.findById(countryId)
                .orElse(null);
        if (existing == null) {
            return BaseResponse.error("ERR_COUNTRY_NOT_FOUND", "Country not found");
        }
        if (request.getCountryCd() != null &&
            !request.getCountryCd().equalsIgnoreCase(existing.getCountryCd()) &&
            countryRepository.existsByCountryCdIgnoreCase(request.getCountryCd())) {
            return BaseResponse.error("ERR_COUNTRY_DUP_CD", "Country code already exists");
        }
        existing.setCountryCd(request.getCountryCd());
        existing.setCountryEnNm(request.getCountryEnNm());
        existing.setCountryArNm(request.getCountryArNm());
        existing.setDialingCode(request.getDialingCode());
        existing.setNationalityEn(request.getNationalityEn());
        existing.setNationalityAr(request.getNationalityAr());
        existing.setIsActive(request.getIsActive());
        existing.setEnDescription(request.getEnDescription());
        existing.setArDescription(request.getArDescription());

        Country saved = countryRepository.save(existing);
        return BaseResponse.success("Country updated", saved);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Country", dataClass = DataClass.HEALTH, message = "Get country")
    @Cacheable(cacheNames = "countries", key = "#countryId")
    public BaseResponse<Country> get(Integer countryId) {
        return countryRepository.findById(countryId)
                .map(c -> BaseResponse.success("Country", c))
                .orElseGet(() -> BaseResponse.error("ERR_COUNTRY_NOT_FOUND", "Country not found"));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Country", dataClass = DataClass.HEALTH, message = "List countries")
    public BaseResponse<?> list(Pageable pageable, String q, boolean unpaged) {
        Sort sort = SortUtils.sanitize(pageable != null ? pageable.getSort() : Sort.unsorted(),
                "countryEnNm", "countryArNm", "countryCd");
        Pageable pg = (pageable == null || !pageable.isPaged()
                ? Pageable.unpaged()
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));

        if (unpaged) {
            List<Country> list;
            if (q == null || q.isBlank()) {
                list = countryRepository.findAll(sort);
            } else {
                list = countryRepository
                        .findByCountryEnNmContainingIgnoreCaseOrCountryArNmContainingIgnoreCase(q, q, sort);
            }
            return BaseResponse.success("Countries list", list);
        }

        Page<Country> page;
        if (q == null || q.isBlank()) {
            page = countryRepository.findAll(pg);
        } else {
            page = countryRepository
                    .findByCountryEnNmContainingIgnoreCaseOrCountryArNmContainingIgnoreCase(q, q, pg);
        }
        return BaseResponse.success("Countries page", page);
    }

    // cache only the RAW list to avoid BaseResponse <-> LinkedHashMap casts in Redis
    @Cacheable(cacheNames = "countries:active", key = "'active'")
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Country> listActiveRaw() {
        return countryRepository.findByIsActiveTrueOrderByCountryEnNmAsc();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Country", dataClass = DataClass.HEALTH, message = "List active countries")
    public BaseResponse<List<Country>> listActive() {
        return BaseResponse.success("Active countries", listActiveRaw());
    }
}
