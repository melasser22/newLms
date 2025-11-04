package com.ejada.admin.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.admin.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SuperadminService {
	 BaseResponse<SuperadminDto> createSuperadmin(CreateSuperadminRequest request);
	    BaseResponse<SuperadminDto> updateSuperadmin(Long id, UpdateSuperadminRequest request);
	    BaseResponse<Void> deleteSuperadmin(Long id);
	    BaseResponse<SuperadminDto> getSuperadmin(Long id);
	    BaseResponse<Page<SuperadminDto>> listSuperadmins(Pageable pageable);
	    BaseResponse<Void> changePassword(ChangePasswordRequest request);
	    BaseResponse<SuperadminAuthResponse> login(SuperadminLoginRequest request);
	    BaseResponse<Void> completeFirstLogin(FirstLoginRequest request);
}