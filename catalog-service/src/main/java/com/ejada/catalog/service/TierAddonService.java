package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TierAddonService {
    TierAddonRes allow(TierAddonCreateReq req);    // allow/bundle addon for tier
    TierAddonRes update(Integer id, TierAddonUpdateReq req);
    void remove(Integer id);                       // soft delete
    Page<TierAddonRes> listByTier(Integer tierId, Pageable pageable);
}
