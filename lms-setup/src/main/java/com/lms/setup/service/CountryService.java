package com.lms.setup.service;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Country;
import com.lms.setup.dto.CountryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CountryService {

    BaseResponse<Country> add(CountryDto request);

    BaseResponse<Country> update(Integer countryId, CountryDto request);

    BaseResponse<Country> get(Integer countryId);

    BaseResponse<?> list(Pageable pageable, String q, boolean unpaged);

    BaseResponse<List<Country>> listActive();
}
