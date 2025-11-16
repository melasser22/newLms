package com.ejada.sec.mapper;

import com.ejada.sec.domain.*;
import com.ejada.sec.dto.*;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface GrantMapper {

  default void applyGrantToRole(Role role, GrantPrivilegesToRoleRequest req,
                                @Context ReferenceResolver resolver) {
    var privs = resolver.privilegesByCodes(req.getTenantId(), req.getPrivilegeCodes());
    role.setRolePrivileges(resolver.toRolePrivileges(role.getId(), privs));
  }

  default void applyRevokeFromRole(Role role, RevokePrivilegesFromRoleRequest req,
                                   @Context ReferenceResolver resolver) {
      if (role.getRolePrivileges() == null) {
        return;
      }
      role.getRolePrivileges().removeIf(rp -> req.getPrivilegeCodes()
          .contains(rp.getPrivilege().getCode()));
  }

  default void applyAssignRoles(User user, AssignRolesToUserRequest req,
                                @Context ReferenceResolver resolver) {
    var roles = resolver.rolesByCodes(req.getTenantId(), req.getRoleCodes());
    user.setRoles(resolver.toUserRoles(user, roles));
  }

  default void applyRevokeRoles(User user, RevokeRolesFromUserRequest req) {
      if (user.getRoles() == null) {
        return;
      }
      user.getRoles().removeIf(ur -> req.getRoleCodes().contains(ur.getRole().getCode()));
  }
}
