package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TierService {
    TierRes create(TierCreateReq req);
    TierRes update(Integer id, TierUpdateReq req);
    TierRes get(Integer id);
    Page<TierRes> list(Boolean active, Pageable pageable);
    void softDelete(Integer id);
}
