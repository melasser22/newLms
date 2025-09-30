package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.ApiStatusMapper;
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
    BaseResponse<List<RoleDto>> response = roleService.listByTenant();
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<RoleDto>> get(@PathVariable("id") Long id) {
    BaseResponse<RoleDto> response = roleService.get(id);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

  @PostMapping
  public ResponseEntity<BaseResponse<RoleDto>> create(@Valid @RequestBody CreateRoleRequest req) {
    BaseResponse<RoleDto> response = roleService.create(req);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<BaseResponse<RoleDto>> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody UpdateRoleRequest req) {
    BaseResponse<RoleDto> response = roleService.update(id, req);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable("id") Long id) {
    BaseResponse<Void> response = roleService.delete(id);
    return ResponseEntity.status(ApiStatusMapper.toHttpStatus(response)).body(response);
  }
}
