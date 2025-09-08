package com.ejada.tenant.service;

import com.ejada.tenant.dto.TenantIntegrationKeyCreateReq;
import com.ejada.tenant.dto.TenantIntegrationKeyRes;
import com.ejada.tenant.dto.TenantIntegrationKeyUpdateReq;
import com.ejada.common.dto.BaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantIntegrationKeyService {
    BaseResponse<TenantIntegrationKeyRes> create(TenantIntegrationKeyCreateReq req);
    BaseResponse<TenantIntegrationKeyRes> update(Long tikId, TenantIntegrationKeyUpdateReq req);
    BaseResponse<Void>                    revoke(Long tikId); // soft-delete + status change
    BaseResponse<TenantIntegrationKeyRes> get(Long tikId);
    BaseResponse<Page<TenantIntegrationKeyRes>> listByTenant(Integer tenantId, Pageable pageable);
}