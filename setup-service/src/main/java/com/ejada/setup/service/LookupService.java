package com.ejada.setup.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.LookupCreateRequest;
import com.ejada.setup.dto.LookupResponse;
import com.ejada.setup.dto.LookupUpdateRequest;

import java.util.List;

public interface LookupService {

    BaseResponse<List<LookupResponse>> getAll();

    BaseResponse<List<LookupResponse>> getByGroup(String groupCode);

    BaseResponse<LookupResponse> add(LookupCreateRequest request);

    BaseResponse<LookupResponse> update(Integer lookupItemId, LookupUpdateRequest request);

    default BaseResponse<List<LookupResponse>> getAllLookups() {
        return getAll();
    }

}
