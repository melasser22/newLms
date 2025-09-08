package com.ejada.sec.service;

import com.ejada.sec.dto.*;
import java.util.List;
import java.util.UUID;

public interface PrivilegeService {
  PrivilegeDto create(CreatePrivilegeRequest req);
  PrivilegeDto update(Long privilegeId, UpdatePrivilegeRequest req);
  void         delete(Long privilegeId);
  PrivilegeDto get(Long privilegeId);
  List<PrivilegeDto> listByTenant(UUID tenantId);
}
