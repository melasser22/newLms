package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import com.ejada.common.dto.BaseResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddonService {
	BaseResponse<AddonRes> create(AddonCreateReq req);
	BaseResponse<AddonRes> update(Integer id, AddonUpdateReq req);
	BaseResponse<AddonRes> get(Integer id);
	BaseResponse<Page<AddonRes>> list(String category, Pageable pageable);
	 BaseResponse<Void> softDelete(Integer id);
}
