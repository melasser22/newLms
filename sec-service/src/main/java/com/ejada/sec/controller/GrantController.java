package com.ejada.sec.controller;

import com.ejada.sec.dto.*;
import com.ejada.sec.service.GrantService;
import com.ejada.starter_core.tenant.RequireTenant;
import com.ejada.starter_security.authorization.PlatformServiceAuthorized;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/grants")
@RequiredArgsConstructor
@RequireTenant
@PlatformServiceAuthorized
public class GrantController {

  private final GrantService grantService;

  // user role membership
  @PostMapping("/users/assign-roles")
  public ResponseEntity<Void> assignRolesToUser(@Valid @RequestBody AssignRolesToUserRequest req) {
    grantService.assignRolesToUser(req);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/users/revoke-roles")
  public ResponseEntity<Void> revokeRolesFromUser(@Valid @RequestBody RevokeRolesFromUserRequest req) {
    grantService.revokeRolesFromUser(req);
    return ResponseEntity.noContent().build();
  }

  // role privileges
  @PostMapping("/roles/grant-privileges")
  public ResponseEntity<Void> grantPrivilegesToRole(@Valid @RequestBody GrantPrivilegesToRoleRequest req) {
    grantService.grantPrivilegesToRole(req);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/roles/revoke-privileges")
  public ResponseEntity<Void> revokePrivilegesFromRole(@Valid @RequestBody RevokePrivilegesFromRoleRequest req) {
    grantService.revokePrivilegesFromRole(req);
    return ResponseEntity.noContent().build();
  }

  // user overrides
  @PostMapping("/users/set-override")
  public ResponseEntity<Void> setUserOverride(@Valid @RequestBody SetUserPrivilegeOverrideRequest req) {
    grantService.setUserPrivilegeOverride(req);
    return ResponseEntity.noContent().build();
  }
}
