package com.lms.setup.service;

import com.common.dto.BaseResponse;
import com.lms.setup.dto.ResourceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceService {

    BaseResponse<ResourceDto> add(ResourceDto request);

    BaseResponse<ResourceDto> update(Integer resourceId, ResourceDto request);

    BaseResponse<ResourceDto> get(Integer resourceId);

    BaseResponse<Page<ResourceDto>> list(Pageable pageable, String q, boolean all);

    BaseResponse<List<ResourceDto>> listActive();

    BaseResponse<List<ResourceDto>> childrenOf(Integer parentResourceId);
}
