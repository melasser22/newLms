package com.lms.setup.service;

import com.common.dto.BaseResponse;
import com.lms.setup.dto.CityDto;
import com.lms.setup.model.City;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CityService {
  BaseResponse<CityDto> add(CityDto request);
  BaseResponse<CityDto> update(Integer id, CityDto request);
  BaseResponse<Void>    delete(Integer id);
  BaseResponse<CityDto> get(Integer id);
  public BaseResponse<?> list(Pageable pageable, String q, boolean all) ;
  BaseResponse<List<CityDto>> listActiveByCountry(Integer countryId);
}
