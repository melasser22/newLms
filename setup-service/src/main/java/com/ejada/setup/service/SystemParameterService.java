package com.ejada.setup.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.SystemParameterRequest;
import com.ejada.setup.dto.SystemParameterResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SystemParameterService {

    BaseResponse<SystemParameterResponse> add(SystemParameterRequest request);

    BaseResponse<SystemParameterResponse> update(Integer paramId, SystemParameterRequest request);

    BaseResponse<SystemParameterResponse> get(Integer paramId);

    BaseResponse<Page<SystemParameterResponse>> list(Pageable pageable, String group, Boolean onlyActive);

    BaseResponse<List<SystemParameterResponse>> getByKeys(List<String> keys);

    BaseResponse<SystemParameterResponse> getByKey(String paramKey);

}
