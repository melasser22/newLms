package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddonFeatureService {
    AddonFeatureRes attach(AddonFeatureCreateReq req);
    AddonFeatureRes update(Integer id, AddonFeatureUpdateReq req);
    void detach(Integer id);
    Page<AddonFeatureRes> listByAddon(Integer addonId, Pageable pageable);
}
