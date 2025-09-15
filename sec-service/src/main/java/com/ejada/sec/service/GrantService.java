package com.ejada.sec.service;

import com.ejada.sec.dto.*;

public interface GrantService {
  void assignRolesToUser(AssignRolesToUserRequest req);
  void revokeRolesFromUser(RevokeRolesFromUserRequest req);

  void grantPrivilegesToRole(GrantPrivilegesToRoleRequest req);
  void revokePrivilegesFromRole(RevokePrivilegesFromRoleRequest req);

  void setUserPrivilegeOverride(SetUserPrivilegeOverrideRequest req);
}
