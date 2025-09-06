package com.ejada.tenant.service;

import com.ejada.tenant.dto.*;
import com.ejada.common.dto.BaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantService {
    BaseResponse<TenantRes> create(TenantCreateReq req);
    BaseResponse<TenantRes> update(Integer id, TenantUpdateReq req);
    BaseResponse<Void>      softDelete(Integer id);
    BaseResponse<TenantRes> get(Integer id);
    BaseResponse<Page<TenantRes>> list(String name, Boolean active, Pageable pageable);
}