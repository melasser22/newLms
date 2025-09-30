package com.ejada.setup.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.CountryDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CountryService {

    BaseResponse<CountryDto> add(CountryDto request);

    BaseResponse<CountryDto> update(Integer countryId, CountryDto request);

    BaseResponse<CountryDto> get(Integer countryId);

    BaseResponse<?> list(Pageable pageable, String q, boolean unpaged);

    BaseResponse<List<CountryDto>> listActive();
}
