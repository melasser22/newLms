package com.ejada.setup.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.DuplicateResourceException;
import com.ejada.common.exception.NotFoundException;
import com.ejada.common.service.BaseCrudService;
import com.ejada.common.sort.SortUtils;
import com.ejada.setup.dto.CountryDto;
import com.ejada.setup.model.Country;
import com.ejada.setup.repository.CountryRepository;
import com.ejada.setup.service.CountryService;
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
public class CountryServiceImpl
        extends BaseCrudService<Country, Integer, CountryDto, CountryDto, Country>
        implements CountryService {

    private final CountryRepository countryRepository;

    public CountryServiceImpl(final CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Override
    protected CountryRepository getRepository() {
        return countryRepository;
    }

    @Override
    protected boolean existsByUniqueField(final CountryDto dto) {
        return dto != null
                && dto.getCountryCd() != null
                && countryRepository.existsByCountryCdIgnoreCase(dto.getCountryCd());
    }

    @Override
    protected Country mapToEntity(final CountryDto dto) {
        Country entity = new Country();
        apply(dto, entity);
        return entity;
    }

    @Override
    protected void updateEntity(final Country entity, final CountryDto dto) {
        if (dto.getCountryCd() != null
                && !dto.getCountryCd().equalsIgnoreCase(entity.getCountryCd())
                && countryRepository.existsByCountryCdIgnoreCase(dto.getCountryCd())) {
            throw new DuplicateResourceException("Country code already exists");
        }
        apply(dto, entity);
    }

    @Override
    protected Country mapToDto(final Country entity) {
        return entity;
    }

    @Override
    protected String getEntityName() {
        return "Country";
    }

    @Override
    protected String duplicateResourceMessage(final CountryDto dto) {
        return "Country code already exists";
    }

    private void apply(final CountryDto source, final Country target) {
        if (source.getCountryCd() != null) {
            target.setCountryCd(source.getCountryCd());
        }
        if (source.getCountryEnNm() != null) {
            target.setCountryEnNm(source.getCountryEnNm());
        }
        if (source.getCountryArNm() != null) {
            target.setCountryArNm(source.getCountryArNm());
        }
        if (source.getDialingCode() != null) {
            target.setDialingCode(source.getDialingCode());
        }
        if (source.getNationalityEn() != null) {
            target.setNationalityEn(source.getNationalityEn());
        }
        if (source.getNationalityAr() != null) {
            target.setNationalityAr(source.getNationalityAr());
        }
        if (source.getIsActive() != null) {
            target.setIsActive(source.getIsActive());
        }
        if (source.getEnDescription() != null) {
            target.setEnDescription(source.getEnDescription());
        }
        if (source.getArDescription() != null) {
            target.setArDescription(source.getArDescription());
        }
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "Country", dataClass = DataClass.HEALTH, message = "Create country")
    @Caching(evict = {
            @CacheEvict(cacheNames = "countries", key = "#result.data.countryId", condition = "#result.isSuccess()"),
            @CacheEvict(cacheNames = "countries:active", key = "'active'")
    })
    public BaseResponse<Country> add(final CountryDto request) {
        try {
            return super.create(request);
        } catch (DuplicateResourceException ex) {
            return BaseResponse.error("ERR_COUNTRY_DUP_CD", ex.getMessage());
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Country", dataClass = DataClass.HEALTH, message = "Update country")
    @Caching(evict = {
            @CacheEvict(cacheNames = "countries", key = "#countryId"),
            @CacheEvict(cacheNames = "countries:active", key = "'active'")
    })
    public BaseResponse<Country> update(final Integer countryId, final CountryDto request) {
        try {
            return super.update(countryId, request);
        } catch (DuplicateResourceException ex) {
            return BaseResponse.error("ERR_COUNTRY_DUP_CD", ex.getMessage());
        } catch (NotFoundException ex) {
            return BaseResponse.error("ERR_COUNTRY_NOT_FOUND", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Country", dataClass = DataClass.HEALTH, message = "Get country")
    @Cacheable(cacheNames = "countries", key = "#countryId")
    public BaseResponse<Country> get(final Integer countryId) {
        try {
            return super.get(countryId);
        } catch (NotFoundException ex) {
            return BaseResponse.error("ERR_COUNTRY_NOT_FOUND", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Country", dataClass = DataClass.HEALTH, message = "List countries")
    public BaseResponse<?> list(final Pageable pageable, final String q, final boolean unpaged) {
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
