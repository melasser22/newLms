package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import com.ejada.sec.service.RoleService;
import com.ejada.starter_core.tenant.RequireTenant;
import com.ejada.sec.security.SecAuthorized;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@RequireTenant
@SecAuthorized
public class RoleController {

  private final RoleService roleService;

  @GetMapping
  public ResponseEntity<BaseResponse<List<RoleDto>>> list() {
    return ResponseEntity.ok(roleService.listByTenant());
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<RoleDto>> get(@PathVariable("id") Long id) {
    return ResponseEntity.ok(roleService.get(id));
  }

  @PostMapping
  public ResponseEntity<BaseResponse<RoleDto>> create(@Valid @RequestBody CreateRoleRequest req) {
    return ResponseEntity.ok(roleService.create(req));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<BaseResponse<RoleDto>> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody UpdateRoleRequest req) {
    return ResponseEntity.ok(roleService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable("id") Long id) {
    return ResponseEntity.ok(roleService.delete(id));
  }
}
