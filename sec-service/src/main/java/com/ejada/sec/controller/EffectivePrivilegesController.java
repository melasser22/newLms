package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.domain.EffectivePrivilegeProjection;
import com.ejada.sec.repository.EffectivePrivilegeViewRepository;
import com.ejada.sec.context.TenantContextProvider;
import com.ejada.starter_core.tenant.RequireTenant;
import com.ejada.sec.security.SecAuthorized;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/effective-privileges")
@RequiredArgsConstructor
@RequireTenant
@SecAuthorized
public class EffectivePrivilegesController {

  private final EffectivePrivilegeViewRepository viewRepo;
  private final TenantContextProvider tenantContextProvider;

  @GetMapping("/{userId}")
  public ResponseEntity<BaseResponse<List<EffectivePrivilegeProjection>>> list(@PathVariable Long userId) {
    var tenantId = tenantContextProvider.requireTenantId();
    return ResponseEntity.ok(
        BaseResponse.success("Effective privileges listed",
            viewRepo.findEffectiveByUserAndTenant(userId, tenantId)));
  }
}
