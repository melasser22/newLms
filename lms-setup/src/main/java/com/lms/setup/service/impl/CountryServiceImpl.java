package com.lms.setup.service.impl;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Country;
import com.lms.setup.repository.CountryRepository;
import com.lms.setup.service.CountryService;
import com.shared.audit.starter.api.AuditAction;
import com.shared.audit.starter.api.DataClass;
import com.shared.audit.starter.api.annotations.Audited;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
@Transactional
public class CountryServiceImpl implements CountryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CountryServiceImpl.class);

    private final CountryRepository countryRepository;

    public CountryServiceImpl(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Override
    @Audited(action = AuditAction.CREATE, entity = "Country", dataClass = DataClass.HEALTH, message = "Create country")
    @CacheEvict(cacheNames = {"countries:active"}, allEntries = true)
    public BaseResponse<Country> add(Country request) {
        try {
            if (request.getCountryCd() == null || request.getCountryCd().isBlank()) {
                return BaseResponse.error("ERR_COUNTRY_CD_REQUIRED", "Country code is required");
            }
            if (countryRepository.existsByCountryCdIgnoreCase(request.getCountryCd())) {
                return BaseResponse.error("ERR_COUNTRY_DUP_CD", "Country code already exists");
            }
            Country saved = countryRepository.save(request);
            return BaseResponse.success("Country created", saved);
        } catch (Exception ex) {
            LOGGER.error("Add country failed", ex);
            return BaseResponse.error("ERR_COUNTRY_ADD", ex.getMessage());
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Country", dataClass = DataClass.HEALTH, message = "Update country")
    @CacheEvict(cacheNames = {"countries:active"}, allEntries = true)
    public BaseResponse<Country> update(Integer countryId, Country request) {
        try {
            Country existing = countryRepository.findById(countryId)
                    .orElse(null);
            if (existing == null) {
                return BaseResponse.error("ERR_COUNTRY_NOT_FOUND", "Country not found");
            }
            // If code changes, ensure uniqueness
            if (request.getCountryCd() != null &&
                !request.getCountryCd().equalsIgnoreCase(existing.getCountryCd()) &&
                countryRepository.existsByCountryCdIgnoreCase(request.getCountryCd())) {
                return BaseResponse.error("ERR_COUNTRY_DUP_CD", "Country code already exists");
            }

            // Update mutable fields
            if (request.getCountryCd() != null) existing.setCountryCd(request.getCountryCd());
            if (request.getCountryEnNm() != null) existing.setCountryEnNm(request.getCountryEnNm());
            if (request.getCountryArNm() != null) existing.setCountryArNm(request.getCountryArNm());
            if (request.getIsActive() != null)   existing.setIsActive(request.getIsActive());

            Country saved = countryRepository.save(existing);
            return BaseResponse.success("Country updated", saved);
        } catch (Exception ex) {
            LOGGER.error("Update country failed", ex);
            return BaseResponse.error("ERR_COUNTRY_UPDATE", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Country", dataClass = DataClass.HEALTH, message = "Get country")
    public BaseResponse<Country> get(Integer countryId) {
        try {
            return countryRepository.findById(countryId)
                    .map(c -> BaseResponse.success("Country", c))
                    .orElseGet(() -> BaseResponse.error("ERR_COUNTRY_NOT_FOUND", "Country not found"));
        } catch (Exception ex) {
            LOGGER.error("Get country failed", ex);
            return BaseResponse.error("ERR_COUNTRY_GET", ex.getMessage());
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    @Audited(action = AuditAction.READ, entity = "Country", dataClass = DataClass.HEALTH, message = "List countries")
    public BaseResponse<?> list(Pageable pageable, String q, boolean all) {
        try {
        	  if (all) {
                  // return all records as list
                  List<Country> list;
                  if (q == null || q.isBlank()) {
                      list = countryRepository.findAll(Sort.by("countryEnNm").ascending());
                  } else {
                      list = countryRepository
                              .findByCountryEnNmContainingIgnoreCaseOrCountryArNmContainingIgnoreCase(q, q,
                                      Sort.by("countryEnNm").ascending());
                  }
                  return BaseResponse.success("Countries list", list);
              }
        	  
            Page<Country> page;
            if (q == null || q.isBlank()) {
                page = countryRepository.findAll(pageable);
            } else {
                page = countryRepository
                        .findByCountryEnNmContainingIgnoreCaseOrCountryArNmContainingIgnoreCase(q, q, pageable);
            }
            return BaseResponse.success("Countries page", page);
        } catch (Exception ex) {
            LOGGER.error("List countries failed", ex);
            return BaseResponse.error("ERR_COUNTRY_LIST", ex.getMessage());
        }
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
        try {
            return BaseResponse.success("Active countries", listActiveRaw());
        } catch (Exception ex) {
            LOGGER.error("List active countries failed", ex);
            return BaseResponse.error("ERR_COUNTRY_LIST_ACTIVE", ex.getMessage());
        }
    }
}
