package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddonService {
    AddonRes create(AddonCreateReq req);
    AddonRes update(Integer id, AddonUpdateReq req);
    AddonRes get(Integer id);
    Page<AddonRes> list(String category, Pageable pageable);
    void softDelete(Integer id);
}
