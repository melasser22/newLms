package com.ejada.sec.mapper;

import com.ejada.sec.domain.*;
import com.ejada.sec.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReferenceResolver {

  private final RoleRepository roleRepository;
  private final PrivilegeRepository privilegeRepository;

  public Role roleByCode(UUID tenantId, String code) {
    return roleRepository.findByTenantIdAndCode(tenantId, code)
        .orElseThrow(() -> new NoSuchElementException("Role not found: " + code));
  }

  public List<Role> rolesByCodes(UUID tenantId, Collection<String> codes) {
      if (codes == null || codes.isEmpty()) {
        return List.of();
      }
      return codes.stream().map(c -> roleByCode(tenantId, c)).toList();
  }

  public Privilege privilegeByCode(UUID tenantId, String code) {
    return privilegeRepository.findByTenantIdAndCode(tenantId, code)
        .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + code));
  }

  public List<Privilege> privilegesByCodes(UUID tenantId, Collection<String> codes) {
      if (codes == null || codes.isEmpty()) {
        return List.of();
      }
      return codes.stream().map(c -> privilegeByCode(tenantId, c)).toList();
  }

  public List<String> toRoleCodes(Collection<UserRole> userRoles) {
      if (userRoles == null) {
        return List.of();
      }
      return userRoles.stream().map(ur -> ur.getRole().getCode()).toList();
  }

  public List<String> toPrivilegeCodes(Collection<RolePrivilege> rolePrivileges) {
      if (rolePrivileges == null) {
        return List.of();
      }
      return rolePrivileges.stream().map(rp -> rp.getPrivilege().getCode()).toList();
  }

  public Set<UserRole> toUserRoles(User user, List<Role> roles) {
      if (roles == null || roles.isEmpty()) {
        return Set.of();
      }
      return roles.stream().map(r -> {
      var id = new UserRoleId(user.getId(), r.getId());
      return UserRole.builder()
          .id(id)
          .user(user)
          .role(r)
          .build();
    }).collect(Collectors.toSet());
  }

  public Set<RolePrivilege> toRolePrivileges(Long roleId, List<Privilege> privilegeList) {
      if (privilegeList == null) {
        return Set.of();
      }
      return privilegeList.stream().map(p -> {
      var id = new RolePrivilegeId(roleId, p.getId());
      return RolePrivilege.builder().id(id).privilege(p).build();
    }).collect(Collectors.toSet());
  }
}
