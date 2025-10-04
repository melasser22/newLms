package com.ejada.sec.service.impl;

import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;
import com.ejada.sec.domain.*;
import com.ejada.sec.dto.*;
import com.ejada.sec.mapper.ReferenceResolver;
import com.ejada.sec.repository.*;
import com.ejada.sec.service.GrantService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GrantServiceImpl implements GrantService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PrivilegeRepository privilegeRepository;
  private final UserRoleRepository userRoleRepository;
  private final RolePrivilegeRepository rolePrivilegeRepository;
  private final UserPrivilegeRepository userPrivilegeRepository;
  private final ReferenceResolver resolver;

  @Transactional
  @Override
  @Audited(action = AuditAction.UPDATE, entity = "UserRole", dataClass = DataClass.CREDENTIALS,
      message = "Assign roles to user")
  public void assignRolesToUser(AssignRolesToUserRequest req) {
    User user = userRepository.findByIdSecure(req.getUserId())
        .orElseThrow(() -> new NoSuchElementException("User not found: " + req.getUserId()));
    var roles = resolver.rolesByCodes(req.getTenantId(), req.getRoleCodes());
    roles.forEach(role -> {
      var id = new UserRoleId(user.getId(), role.getId());
      if (!userRoleRepository.existsById(id)) {
        userRoleRepository.save(UserRole.builder().id(id).user(user).role(role).build());
      }
    });
  }

  @Transactional
  @Override
  @Audited(action = AuditAction.UPDATE, entity = "UserRole", dataClass = DataClass.CREDENTIALS,
      message = "Revoke roles from user")
  public void revokeRolesFromUser(RevokeRolesFromUserRequest req) {
    User user = userRepository.findByIdSecure(req.getUserId())
        .orElseThrow(() -> new NoSuchElementException("User not found: " + req.getUserId()));
    user.getRoles().removeIf(ur -> req.getRoleCodes().contains(ur.getRole().getCode()));
    userRepository.save(user);
  }

  @Transactional
  @Override
  @Audited(action = AuditAction.UPDATE, entity = "RolePrivilege", dataClass = DataClass.CREDENTIALS,
      message = "Grant privileges to role")
  public void grantPrivilegesToRole(GrantPrivilegesToRoleRequest req) {
    Role role = roleRepository.findByTenantIdAndCode(req.getTenantId(), req.getRoleCode())
        .orElseThrow(() -> new NoSuchElementException("Role not found: " + req.getRoleCode()));
    var privs = resolver.privilegesByCodes(req.getTenantId(), req.getPrivilegeCodes());
    privs.forEach(p -> {
      var id = new RolePrivilegeId(role.getId(), p.getId());
      if (!rolePrivilegeRepository.existsById(id)) {
        rolePrivilegeRepository.save(RolePrivilege.builder().id(id).role(role).privilege(p).build());
      }
    });
  }

  @Transactional
  @Override
  @Audited(action = AuditAction.UPDATE, entity = "RolePrivilege", dataClass = DataClass.CREDENTIALS,
      message = "Revoke privileges from role")
  public void revokePrivilegesFromRole(RevokePrivilegesFromRoleRequest req) {
    Role role = roleRepository.findByTenantIdAndCode(req.getTenantId(), req.getRoleCode())
        .orElseThrow(() -> new NoSuchElementException("Role not found: " + req.getRoleCode()));
    role.getRolePrivileges().removeIf(rp -> req.getPrivilegeCodes().contains(rp.getPrivilege().getCode()));
    roleRepository.save(role);
  }

  @Transactional
  @Override
  @Audited(action = AuditAction.UPDATE, entity = "UserPrivilege", dataClass = DataClass.CREDENTIALS,
      message = "Set user privilege override")
  public void setUserPrivilegeOverride(SetUserPrivilegeOverrideRequest req) {
    User user = userRepository.findByIdSecure(req.getUserId())
        .orElseThrow(() -> new NoSuchElementException("User not found: " + req.getUserId()));
    Privilege p = privilegeRepository.findByTenantIdAndCode(req.getTenantId(), req.getPrivilegeCode())
        .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + req.getPrivilegeCode()));

    var id = new UserPrivilegeId(user.getId(), p.getId());
    var up = userPrivilegeRepository.findById(id)
        .orElse(UserPrivilege.builder().id(id).user(user).privilege(p).build());
    up.setGranted(Boolean.TRUE.equals(req.getGranted()));
    userPrivilegeRepository.save(up);
  }
}
