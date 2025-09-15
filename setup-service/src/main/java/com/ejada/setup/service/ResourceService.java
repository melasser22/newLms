package com.ejada.setup.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.ResourceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceService {

    BaseResponse<ResourceDto> add(ResourceDto request);

    BaseResponse<ResourceDto> update(Integer resourceId, ResourceDto request);

    BaseResponse<ResourceDto> get(Integer resourceId);

    BaseResponse<Page<ResourceDto>> list(Pageable pageable, String q, boolean unpaged);

    BaseResponse<List<ResourceDto>> listActive();

    BaseResponse<List<ResourceDto>> childrenOf(Integer parentResourceId);
}
