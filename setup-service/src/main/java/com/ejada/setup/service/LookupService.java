package com.ejada.setup.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.model.Lookup;

import java.util.List;

public interface LookupService {

    BaseResponse<List<Lookup>> getAll();                 // full list

    BaseResponse<List<Lookup>> getByGroup(String groupCode);

    BaseResponse<Lookup> add(Lookup request);

    BaseResponse<Lookup> update(Integer lookupItemId, Lookup request);

    default BaseResponse<List<Lookup>> getAllLookups() {
        return getAll();
    }

}
