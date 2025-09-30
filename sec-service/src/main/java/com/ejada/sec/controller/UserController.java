package com.ejada.sec.controller;

import static com.ejada.common.http.BaseResponseEntityFactory.build;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.*;
import com.ejada.sec.service.UserService;
import com.ejada.starter_core.tenant.RequireTenant;
import com.ejada.sec.security.SecAuthorized;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RequireTenant
@SecAuthorized
public class UserController {

  private final UserService userService;

  @GetMapping
  public ResponseEntity<BaseResponse<List<UserDto>>> list() {
    BaseResponse<List<UserDto>> response = userService.listByTenant();
    return build(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<UserDto>> get(@PathVariable("id") Long id) {
    BaseResponse<UserDto> response = userService.get(id);
    return build(response);
  }

  @PostMapping
  public ResponseEntity<BaseResponse<UserDto>> create(@Valid @RequestBody CreateUserRequest req) {
    BaseResponse<UserDto> response = userService.create(req);
    return build(response, HttpStatus.CREATED);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<BaseResponse<UserDto>> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody UpdateUserRequest req) {
    BaseResponse<UserDto> response = userService.update(id, req);
    return build(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable("id") Long id) {
    BaseResponse<Void> response = userService.delete(id);
    return build(response);
  }

  @PostMapping("/{id}/enable")
  public ResponseEntity<BaseResponse<Void>> enable(@PathVariable("id") Long id) {
    BaseResponse<Void> response = userService.enable(id);
    return build(response);
  }

  @PostMapping("/{id}/disable")
  public ResponseEntity<BaseResponse<Void>> disable(@PathVariable("id") Long id) {
    BaseResponse<Void> response = userService.disable(id);
    return build(response);
  }

  @PostMapping("/{id}/lock")
  public ResponseEntity<BaseResponse<Void>> lock(@PathVariable("id") Long id) {
    BaseResponse<Void> response = userService.lock(id);
    return build(response);
  }

  @PostMapping("/{id}/unlock")
  public ResponseEntity<BaseResponse<Void>> unlock(@PathVariable("id") Long id) {
    BaseResponse<Void> response = userService.unlock(id);
    return build(response);
  }
}
