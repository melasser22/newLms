package com.lms.setup.service;

import com.common.dto.BaseResponse;
import com.lms.setup.model.SystemParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SystemParameterService {

    BaseResponse<SystemParameter> add(SystemParameter request);

    BaseResponse<SystemParameter> update(Integer paramId, SystemParameter request);

    BaseResponse<SystemParameter> get(Integer paramId);

    BaseResponse<Page<SystemParameter>> list(Pageable pageable, String group, Boolean onlyActive);

    BaseResponse<List<SystemParameter>> getByKeys(List<String> keys);
    
    BaseResponse<SystemParameter> getByKey(String paramKey);

}
