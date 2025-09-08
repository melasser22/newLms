package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import com.ejada.sec.service.PrivilegeService;
import com.ejada.starter_core.tenant.RequireTenant;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/privileges")
@RequiredArgsConstructor
@RequireTenant
public class PrivilegeController {

  private final PrivilegeService privilegeService;

  @GetMapping
  public ResponseEntity<BaseResponse<List<PrivilegeDto>>> list() {
    return ResponseEntity.ok(privilegeService.listByTenant());
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<PrivilegeDto>> get(@PathVariable("id") Long id) {
    return ResponseEntity.ok(privilegeService.get(id));
  }

  @PostMapping
  public ResponseEntity<BaseResponse<PrivilegeDto>> create(@Valid @RequestBody CreatePrivilegeRequest req) {
    return ResponseEntity.ok(privilegeService.create(req));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<BaseResponse<PrivilegeDto>> update(@PathVariable("id") Long id,
                                             @Valid @RequestBody UpdatePrivilegeRequest req) {
    return ResponseEntity.ok(privilegeService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable("id") Long id) {
    return ResponseEntity.ok(privilegeService.delete(id));
  }
}
