package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;
import com.ejada.sec.dto.*;
import com.ejada.sec.service.PrivilegeService;
import com.ejada.starter_core.tenant.RequireTenant;
import com.ejada.starter_security.authorization.PlatformServiceAuthorized;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/privileges")
@RequiredArgsConstructor
@RequireTenant
@PlatformServiceAuthorized
public class PrivilegeController extends BaseResponseController {

  private final PrivilegeService privilegeService;

  @GetMapping
  public ResponseEntity<BaseResponse<List<PrivilegeDto>>> list() {
    return respond(privilegeService::listByTenant);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<PrivilegeDto>> get(@PathVariable("id") Long id) {
    return respond(() -> privilegeService.get(id));
  }

  @PostMapping
  public ResponseEntity<BaseResponse<PrivilegeDto>> create(@Valid @RequestBody CreatePrivilegeRequest req) {
    return respond(() -> privilegeService.create(req), HttpStatus.CREATED);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<BaseResponse<PrivilegeDto>> update(@PathVariable("id") Long id,
                                             @Valid @RequestBody UpdatePrivilegeRequest req) {
    return respond(() -> privilegeService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable("id") Long id) {
    return respond(() -> privilegeService.delete(id));
}
}
