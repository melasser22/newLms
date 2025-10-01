package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;
import com.ejada.sec.dto.*;
import com.ejada.sec.service.RoleService;
import com.ejada.starter_core.tenant.RequireTenant;
import com.ejada.starter_security.authorization.PlatformServiceAuthorized;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@RequireTenant
@PlatformServiceAuthorized
public class RoleController extends BaseResponseController {

  private final RoleService roleService;

  @GetMapping
  public ResponseEntity<BaseResponse<List<RoleDto>>> list() {
    return respond(roleService::listByTenant);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<RoleDto>> get(@PathVariable("id") Long id) {
    return respond(() -> roleService.get(id));
  }

  @PostMapping
  public ResponseEntity<BaseResponse<RoleDto>> create(@Valid @RequestBody CreateRoleRequest req) {
    return respond(() -> roleService.create(req), HttpStatus.CREATED);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<BaseResponse<RoleDto>> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody UpdateRoleRequest req) {
    return respond(() -> roleService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable("id") Long id) {
    return respond(() -> roleService.delete(id));
}
}
