package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;
import com.ejada.sec.domain.EffectivePrivilegeProjection;
import com.ejada.sec.repository.EffectivePrivilegeViewRepository;
import com.ejada.starter_core.tenant.RequireTenant;
import com.ejada.starter_security.authorization.PlatformServiceAuthorized;
import com.ejada.sec.util.TenantContextResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/effective-privileges")
@RequiredArgsConstructor
@RequireTenant
@PlatformServiceAuthorized
public class EffectivePrivilegesController extends BaseResponseController {

  private final EffectivePrivilegeViewRepository viewRepo;

  @GetMapping("/{userId}")
  public ResponseEntity<BaseResponse<List<EffectivePrivilegeProjection>>> list(@PathVariable Long userId) {
    return respond(() -> BaseResponse.success(
            "Effective privileges listed",
            viewRepo.findEffectiveByUserAndTenant(userId, TenantContextResolver.requireTenantId())));
  }
}
