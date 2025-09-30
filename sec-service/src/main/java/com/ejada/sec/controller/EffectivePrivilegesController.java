package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.ApiStatusMapper;
import com.ejada.sec.domain.EffectivePrivilegeProjection;
import com.ejada.sec.repository.EffectivePrivilegeViewRepository;
import com.ejada.starter_core.tenant.RequireTenant;
import com.ejada.sec.security.SecAuthorized;
import com.ejada.sec.util.TenantContextResolver;
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

  @GetMapping("/{userId}")
  public ResponseEntity<BaseResponse<List<EffectivePrivilegeProjection>>> list(@PathVariable Long userId) {
    BaseResponse<List<EffectivePrivilegeProjection>> response =
        BaseResponse.success(
            "Effective privileges listed",
            viewRepo.findEffectiveByUserAndTenant(userId, TenantContextResolver.requireTenantId()));
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }
}
