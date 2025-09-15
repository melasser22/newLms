package com.ejada.setup.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.model.Country;
import com.ejada.setup.dto.CountryDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CountryService {

    BaseResponse<Country> add(CountryDto request);

    BaseResponse<Country> update(Integer countryId, CountryDto request);

    BaseResponse<Country> get(Integer countryId);

    BaseResponse<?> list(Pageable pageable, String q, boolean unpaged);

    BaseResponse<List<Country>> listActive();
}
