package com.ejada.sec.controller;

import static com.ejada.common.http.BaseResponseEntityFactory.build;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import com.ejada.sec.service.PrivilegeService;
import com.ejada.starter_core.tenant.RequireTenant;
import com.ejada.sec.security.SecAuthorized;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/privileges")
@RequiredArgsConstructor
@RequireTenant
@SecAuthorized
public class PrivilegeController {

  private final PrivilegeService privilegeService;

  @GetMapping
  public ResponseEntity<BaseResponse<List<PrivilegeDto>>> list() {
    BaseResponse<List<PrivilegeDto>> response = privilegeService.listByTenant();
    return build(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<PrivilegeDto>> get(@PathVariable("id") Long id) {
    BaseResponse<PrivilegeDto> response = privilegeService.get(id);
    return build(response);
  }

  @PostMapping
  public ResponseEntity<BaseResponse<PrivilegeDto>> create(@Valid @RequestBody CreatePrivilegeRequest req) {
    BaseResponse<PrivilegeDto> response = privilegeService.create(req);
    return build(response, HttpStatus.CREATED);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<BaseResponse<PrivilegeDto>> update(@PathVariable("id") Long id,
                                             @Valid @RequestBody UpdatePrivilegeRequest req) {
    BaseResponse<PrivilegeDto> response = privilegeService.update(id, req);
    return build(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable("id") Long id) {
    BaseResponse<Void> response = privilegeService.delete(id);
    return build(response);
}
}
