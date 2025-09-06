package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import com.ejada.common.dto.BaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TierAddonService {
    BaseResponse<TierAddonRes> allow(TierAddonCreateReq req);    // allow/bundle addon for tier
    BaseResponse<TierAddonRes> update(Integer id, TierAddonUpdateReq req);
    BaseResponse<Void> remove(Integer id);                       // soft delete
    BaseResponse<Page<TierAddonRes>> listByTier(Integer tierId, Pageable pageable);
}
