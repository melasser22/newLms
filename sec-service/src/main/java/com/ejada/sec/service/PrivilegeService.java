package com.ejada.sec.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import java.util.List;

public interface PrivilegeService {
  BaseResponse<PrivilegeDto> create(CreatePrivilegeRequest req);
  BaseResponse<PrivilegeDto> update(Long privilegeId, UpdatePrivilegeRequest req);
  BaseResponse<Void>         delete(Long privilegeId);
  BaseResponse<PrivilegeDto> get(Long privilegeId);
  BaseResponse<List<PrivilegeDto>> listByTenant();
}
