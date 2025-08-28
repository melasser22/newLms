package com.lms.setup.service;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceService {

    BaseResponse<Resource> add(Resource request);

    BaseResponse<Resource> update(Integer resourceId, Resource request);

    BaseResponse<Resource> get(Integer resourceId);

    BaseResponse<Page<Resource>> list(Pageable pageable, String q);

    BaseResponse<List<Resource>> childrenOf(Integer parentResourceId);
}
