package com.ejada.setup.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.CityDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CityService {
  BaseResponse<CityDto> add(CityDto request);
  BaseResponse<CityDto> update(Integer id, CityDto request);
  BaseResponse<Void>    delete(Integer id);
  BaseResponse<CityDto> get(Integer id);
  BaseResponse<Page<CityDto>> list(Pageable pageable, String q, boolean unpaged);
  BaseResponse<List<CityDto>> listActiveByCountry(Integer countryId);
}
