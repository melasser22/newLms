package com.ejada.tenant.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.tenant.dto.TenantHealthScoreRes;

public interface TenantHealthService {

    BaseResponse<TenantHealthScoreRes> getHealthScore(Integer tenantId);

    TenantHealthScoreRes calculateAndStoreHealthScore(Integer tenantId);
}
