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
import com.ejada.setup.mapper.CountryMapper;
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
        extends BaseCrudService<Country, Integer, CountryDto, CountryDto, CountryDto>
        implements CountryService {

    private final CountryRepository countryRepository;
    private final CountryMapper mapper;

    public CountryServiceImpl(final CountryRepository countryRepository, final CountryMapper mapper) {
        this.countryRepository = countryRepository;
        this.mapper = mapper;
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
        return mapper.toEntity(dto);
    }

    @Override
    protected void updateEntity(final Country entity, final CountryDto dto) {
        if (dto.getCountryCd() != null
                && !dto.getCountryCd().equalsIgnoreCase(entity.getCountryCd())
                && countryRepository.existsByCountryCdIgnoreCase(dto.getCountryCd())) {
            throw new DuplicateResourceException("Country code already exists");
        }
        mapper.updateEntity(dto, entity);
    }

    @Override
    protected CountryDto mapToDto(final Country entity) {
        return mapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Country";
    }

    @Override
    protected String duplicateResourceMessage(final CountryDto dto) {
        return "Country code already exists";
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "Country", dataClass = DataClass.HEALTH, message = "Create country")
    @Caching(evict = {
            @CacheEvict(cacheNames = "countries", key = "#result.data.countryId", condition = "#result.isSuccess()"),
            @CacheEvict(cacheNames = "countries:active", key = "'active'")
    })
    public BaseResponse<CountryDto> add(final CountryDto request) {
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
    public BaseResponse<CountryDto> update(final Integer countryId, final CountryDto request) {
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
    public BaseResponse<CountryDto> get(final Integer countryId) {
        try {
            return super.get(countryId);
        } catch (NotFoundException ex) {
            return BaseResponse.error("ERR_COUNTRY_NOT_FOUND", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Country", dataClass = DataClass.HEALTH, message = "List countries")
    public BaseResponse<Object> list(final Pageable pageable, final String q, final boolean unpaged) {
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
            return BaseResponse.<Object>success("Countries list", mapper.toDtoList(list));
        }

        Page<Country> page;
        if (q == null || q.isBlank()) {
            page = countryRepository.findAll(pg);
        } else {
            page = countryRepository
                    .findByCountryEnNmContainingIgnoreCaseOrCountryArNmContainingIgnoreCase(q, q, pg);
        }
        return BaseResponse.<Object>success("Countries page", mapper.toDtoPage(page));
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
    public BaseResponse<List<CountryDto>> listActive() {
        return BaseResponse.success("Active countries", mapper.toDtoList(listActiveRaw()));
    }
}
