package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import com.ejada.common.dto.BaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddonFeatureService {
    BaseResponse<AddonFeatureRes> attach(AddonFeatureCreateReq req);
    BaseResponse<AddonFeatureRes> update(Integer id, AddonFeatureUpdateReq req);
    BaseResponse<Void> detach(Integer id);
    BaseResponse<Page<AddonFeatureRes>> listByAddon(Integer addonId, Pageable pageable);
}
