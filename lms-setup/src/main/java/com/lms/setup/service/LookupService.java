package com.lms.setup.service;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Lookup;

import java.util.List;

public interface LookupService {

    BaseResponse<List<Lookup>> getAll();                 // full list

    BaseResponse<List<Lookup>> getByGroup(String groupCode);

    BaseResponse<Lookup> add(Lookup request);

    BaseResponse<Lookup> update(Integer lookupItemId, Lookup request);
    
    BaseResponse<List<Lookup>> getAllLookups();

}
