package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import com.ejada.sec.service.UserService;
import com.ejada.starter_core.tenant.RequireTenant;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RequireTenant
public class UserController {

  private final UserService userService;

  @GetMapping
  public ResponseEntity<BaseResponse<List<UserDto>>> list() {
    return ResponseEntity.ok(userService.listByTenant());
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<UserDto>> get(@PathVariable("id") Long id) {
    return ResponseEntity.ok(userService.get(id));
  }

  @PostMapping
  public ResponseEntity<BaseResponse<UserDto>> create(@Valid @RequestBody CreateUserRequest req) {
    return ResponseEntity.ok(userService.create(req));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<BaseResponse<UserDto>> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody UpdateUserRequest req) {
    return ResponseEntity.ok(userService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable("id") Long id) {
    return ResponseEntity.ok(userService.delete(id));
  }

  @PostMapping("/{id}/enable")
  public ResponseEntity<BaseResponse<Void>> enable(@PathVariable("id") Long id) {
    return ResponseEntity.ok(userService.enable(id));
  }

  @PostMapping("/{id}/disable")
  public ResponseEntity<BaseResponse<Void>> disable(@PathVariable("id") Long id) {
    return ResponseEntity.ok(userService.disable(id));
  }

  @PostMapping("/{id}/lock")
  public ResponseEntity<BaseResponse<Void>> lock(@PathVariable("id") Long id) {
    return ResponseEntity.ok(userService.lock(id));
  }

  @PostMapping("/{id}/unlock")
  public ResponseEntity<BaseResponse<Void>> unlock(@PathVariable("id") Long id) {
    return ResponseEntity.ok(userService.unlock(id));
  }
}
