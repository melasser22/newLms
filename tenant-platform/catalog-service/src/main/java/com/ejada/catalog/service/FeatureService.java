package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import com.ejada.common.dto.BaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeatureService {
    BaseResponse<FeatureRes> create(FeatureCreateReq req);
    BaseResponse<FeatureRes> update(Integer id, FeatureUpdateReq req);
    BaseResponse<FeatureRes> get(Integer id);
    BaseResponse<Page<FeatureRes>> list(String category, Pageable pageable);
    BaseResponse<Void> softDelete(Integer id);
}
