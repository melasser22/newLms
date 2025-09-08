package com.ejada.sec.controller;

import com.ejada.sec.dto.*;
import com.ejada.sec.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  // List by tenant: pass tenantId as query param (or switch to header if you prefer)
  @GetMapping
  public ResponseEntity<List<UserDto>> list(@RequestParam("tenantId") UUID tenantId) {
    return ResponseEntity.ok(userService.listByTenant(tenantId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDto> get(@PathVariable("id") Long id) {
    return ResponseEntity.ok(userService.get(id));
  }

  @PostMapping
  public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest req) {
    return ResponseEntity.ok(userService.create(req));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<UserDto> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody UpdateUserRequest req) {
    return ResponseEntity.ok(userService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/enable")
  public ResponseEntity<Void> enable(@PathVariable("id") Long id) {
    userService.enable(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/disable")
  public ResponseEntity<Void> disable(@PathVariable("id") Long id) {
    userService.disable(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/lock")
  public ResponseEntity<Void> lock(@PathVariable("id") Long id) {
    userService.lock(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/unlock")
  public ResponseEntity<Void> unlock(@PathVariable("id") Long id) {
    userService.unlock(id);
    return ResponseEntity.noContent().build();
  }
}
