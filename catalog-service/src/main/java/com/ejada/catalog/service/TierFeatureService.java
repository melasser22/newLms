package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import com.ejada.common.dto.BaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TierFeatureService {
    BaseResponse<TierFeatureRes> attach(TierFeatureCreateReq req);           // create policy for (tier, feature)
    BaseResponse<TierFeatureRes> update(Integer id, TierFeatureUpdateReq req);
    BaseResponse<Void> detach(Integer id);                                   // soft delete
    BaseResponse<Page<TierFeatureRes>> listByTier(Integer tierId, Pageable pageable);
}
