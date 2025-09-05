package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TierFeatureService {
    TierFeatureRes attach(TierFeatureCreateReq req);           // create policy for (tier, feature)
    TierFeatureRes update(Integer id, TierFeatureUpdateReq req);
    void detach(Integer id);                                   // soft delete
    Page<TierFeatureRes> listByTier(Integer tierId, Pageable pageable);
}
