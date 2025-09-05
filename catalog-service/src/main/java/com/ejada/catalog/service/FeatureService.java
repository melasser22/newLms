package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeatureService {
    FeatureRes create(FeatureCreateReq req);
    FeatureRes update(Integer id, FeatureUpdateReq req);
    FeatureRes get(Integer id);
    Page<FeatureRes> list(String category, Pageable pageable);
    void softDelete(Integer id);
}
