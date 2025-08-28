package com.lms.setup.service;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CountryService {

    BaseResponse<Country> add(Country request);

    BaseResponse<Country> update(Integer countryId, Country request);

    BaseResponse<Country> get(Integer countryId);

    BaseResponse<?> list(Pageable pageable, String q, boolean all);

    BaseResponse<List<Country>> listActive();
}
