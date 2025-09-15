package com.ejada.catalog.service;

import com.ejada.catalog.dto.TierCreateReq;
import com.ejada.catalog.dto.TierRes;
import com.ejada.catalog.dto.TierUpdateReq;
import com.ejada.common.dto.BaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TierService {
    BaseResponse<TierRes> create(TierCreateReq req);
    BaseResponse<TierRes> update(Integer id, TierUpdateReq req);
    BaseResponse<TierRes> get(Integer id);
    BaseResponse<Page<TierRes>> list(Boolean active, Pageable pageable);
    BaseResponse<Void> softDelete(Integer id);
}
