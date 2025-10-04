package com.ejada.sec.service.impl;

import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;
import com.ejada.sec.domain.Privilege;
import com.ejada.sec.domain.Role;
import com.ejada.sec.domain.RolePrivilege;
import com.ejada.sec.domain.RolePrivilegeId;
import com.ejada.sec.domain.User;
import com.ejada.sec.domain.UserPrivilege;
import com.ejada.sec.domain.UserPrivilegeId;
import com.ejada.sec.domain.UserRole;
import com.ejada.sec.domain.UserRoleId;
import com.ejada.sec.dto.AssignRolesToUserRequest;
import com.ejada.sec.dto.GrantPrivilegesToRoleRequest;
import com.ejada.sec.dto.RevokePrivilegesFromRoleRequest;
import com.ejada.sec.dto.RevokeRolesFromUserRequest;
import com.ejada.sec.dto.SetUserPrivilegeOverrideRequest;
import com.ejada.sec.mapper.ReferenceResolver;
import com.ejada.sec.repository.PrivilegeRepository;
import com.ejada.sec.repository.RolePrivilegeRepository;
import com.ejada.sec.repository.RoleRepository;
import com.ejada.sec.repository.UserPrivilegeRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.repository.UserRoleRepository;
import com.ejada.sec.service.GrantService;
import com.ejada.sec.service.PermissionEvaluationService;
import com.ejada.sec.service.RoleHierarchyService;
import com.ejada.sec.util.SecurityContextUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing role and privilege assignments with hierarchy enforcement.
 * All operations validate role hierarchy rules before making changes.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GrantServiceImpl implements GrantService {

    private final RoleHierarchyService roleHierarchyService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;
    private final UserPrivilegeRepository userPrivilegeRepository;
    private final PermissionEvaluationService permissionEvaluationService;
    private final ReferenceResolver resolver;

    @Override
    @Audited(
        action = AuditAction.UPDATE,
        entity = "UserRole",
        dataClass = DataClass.CREDENTIALS,
        message = "Assign roles to user"
    )
    public void assignRolesToUser(AssignRolesToUserRequest req) {
        Long currentUserId = SecurityContextUtils.getCurrentUserId();
        User currentUser = userRepository.findByIdSecure(currentUserId)
            .orElseThrow(() -> new SecurityException("Current user not found"));

        User targetUser = userRepository.findByIdSecure(req.getUserId())
            .orElseThrow(() -> new NoSuchElementException("User not found: " + req.getUserId()));

        if (!roleHierarchyService.canManageUser(currentUser, targetUser)) {
            log.warn("User {} attempted to manage user {} - insufficient privileges",
                currentUser.getId(), targetUser.getId());
            throw new AccessDeniedException(String.format(
                "Cannot manage user %s - insufficient privileges or different tenant",
                targetUser.getUsername()));
        }

        List<String> requestedCodes = req.getRoleCodes();
        if (requestedCodes == null || requestedCodes.isEmpty()) {
            log.debug("No roles provided for assignment to user {}", targetUser.getId());
            return;
        }

        Set<String> uniqueCodes = new HashSet<>(requestedCodes);
        List<Role> rolesToAssign = roleRepository.findByTenantIdAndCodeIn(
            targetUser.getTenantId(), uniqueCodes);

        if (rolesToAssign.size() != uniqueCodes.size()) {
            throw new IllegalArgumentException("One or more role codes not found in tenant");
        }

        for (Role role : rolesToAssign) {
            if (!roleHierarchyService.canAssignRole(currentUser, role)) {
                log.warn("User {} attempted to assign role {} (level {}) - insufficient privileges",
                    currentUser.getId(), role.getCode(), role.getLevel().getLevel());
                throw new AccessDeniedException(String.format(
                    "Cannot assign role '%s' (level %d) - insufficient privileges",
                    role.getCode(), role.getLevel().getLevel()));
            }
        }

        boolean rolesAdded = false;
        for (Role role : rolesToAssign) {
            UserRoleId id = new UserRoleId(targetUser.getId(), role.getId());
            if (!userRoleRepository.existsById(id)) {
                userRoleRepository.save(
                    UserRole.builder()
                        .id(id)
                        .user(targetUser)
                        .role(role)
                        .build()
                );
                log.info("User {} assigned role {} (level {}) to user {}",
                    currentUser.getId(), role.getCode(), role.getLevel().getLevel(), targetUser.getId());
                rolesAdded = true;
            }
        }

        if (rolesAdded) {
            permissionEvaluationService.invalidateUserPermissions(targetUser.getId());
            log.info("Invalidated permission cache after role assignment for user {}", targetUser.getId());
        }
    }

    @Override
    @Audited(
        action = AuditAction.UPDATE,
        entity = "UserRole",
        dataClass = DataClass.CREDENTIALS,
        message = "Revoke roles from user"
    )
    public void revokeRolesFromUser(RevokeRolesFromUserRequest req) {
        Long currentUserId = SecurityContextUtils.getCurrentUserId();
        User currentUser = userRepository.findByIdSecure(currentUserId)
            .orElseThrow(() -> new SecurityException("Current user not found"));

        User targetUser = userRepository.findByIdSecure(req.getUserId())
            .orElseThrow(() -> new NoSuchElementException("User not found: " + req.getUserId()));

        if (!roleHierarchyService.canManageUser(currentUser, targetUser)) {
            throw new AccessDeniedException("Cannot manage user - insufficient privileges");
        }

        List<String> requestedCodes = req.getRoleCodes();
        if (requestedCodes == null || requestedCodes.isEmpty()) {
            log.debug("No roles provided for revocation from user {}", targetUser.getId());
            return;
        }

        List<Role> rolesToRevoke = targetUser.getRoles().stream()
            .map(UserRole::getRole)
            .filter(role -> requestedCodes.contains(role.getCode()))
            .collect(Collectors.toList());

        for (Role role : rolesToRevoke) {
            if (!roleHierarchyService.canRevokeRole(currentUser, targetUser, role)) {
                log.warn("User {} attempted to revoke role {} from user {} - insufficient privileges",
                    currentUser.getId(), role.getCode(), targetUser.getId());
                throw new AccessDeniedException(String.format(
                    "Cannot revoke role '%s' - insufficient privileges",
                    role.getCode()));
            }
        }

        boolean rolesRemoved = targetUser.getRoles().removeIf(ur -> requestedCodes.contains(ur.getRole().getCode()));
        if (rolesRemoved) {
            userRepository.save(targetUser);
            permissionEvaluationService.invalidateUserPermissions(targetUser.getId());
            log.info("User {} revoked roles {} from user {}",
                currentUser.getId(), requestedCodes, targetUser.getId());
        }
    }

    @Override
    @Audited(
        action = AuditAction.UPDATE,
        entity = "RolePrivilege",
        dataClass = DataClass.CREDENTIALS,
        message = "Grant privileges to role"
    )
    public void grantPrivilegesToRole(GrantPrivilegesToRoleRequest req) {
        Long currentUserId = SecurityContextUtils.getCurrentUserId();
        User currentUser = userRepository.findByIdSecure(currentUserId)
            .orElseThrow(() -> new SecurityException("Current user not found"));

        Role role = roleRepository.findByTenantIdAndCode(req.getTenantId(), req.getRoleCode())
            .orElseThrow(() -> new NoSuchElementException("Role not found: " + req.getRoleCode()));

        if (!roleHierarchyService.canAssignRole(currentUser, role)) {
            log.warn("User {} attempted to grant privileges to role {} (level {}) - insufficient privileges",
                currentUser.getId(), role.getCode(), role.getLevel().getLevel());
            throw new AccessDeniedException(String.format(
                "Cannot modify role '%s' (level %d) - insufficient privileges",
                role.getCode(), role.getLevel().getLevel()));
        }

        List<Privilege> privileges = resolver.privilegesByCodes(req.getTenantId(), req.getPrivilegeCodes());

        boolean privilegesAdded = false;
        for (Privilege privilege : privileges) {
            RolePrivilegeId id = new RolePrivilegeId(role.getId(), privilege.getId());
            if (!rolePrivilegeRepository.existsById(id)) {
                rolePrivilegeRepository.save(
                    RolePrivilege.builder()
                        .id(id)
                        .role(role)
                        .privilege(privilege)
                        .build()
                );
                privilegesAdded = true;
            }
        }

        if (privilegesAdded) {
            permissionEvaluationService.invalidateRolePermissions(role.getId());
            log.info("User {} granted {} privileges to role {}",
                currentUser.getId(), privileges.size(), role.getCode());
        }
    }

    @Override
    @Audited(
        action = AuditAction.UPDATE,
        entity = "RolePrivilege",
        dataClass = DataClass.CREDENTIALS,
        message = "Revoke privileges from role"
    )
    public void revokePrivilegesFromRole(RevokePrivilegesFromRoleRequest req) {
        Long currentUserId = SecurityContextUtils.getCurrentUserId();
        User currentUser = userRepository.findByIdSecure(currentUserId)
            .orElseThrow(() -> new SecurityException("Current user not found"));

        Role role = roleRepository.findByTenantIdAndCode(req.getTenantId(), req.getRoleCode())
            .orElseThrow(() -> new NoSuchElementException("Role not found: " + req.getRoleCode()));

        if (!roleHierarchyService.canAssignRole(currentUser, role)) {
            throw new AccessDeniedException("Cannot modify role - insufficient privileges");
        }

        List<String> privilegeCodes = req.getPrivilegeCodes();
        if (privilegeCodes == null || privilegeCodes.isEmpty()) {
            log.debug("No privilege codes provided for revocation from role {}", role.getCode());
            return;
        }

        boolean removed = role.getRolePrivileges().removeIf(
            rp -> privilegeCodes.contains(rp.getPrivilege().getCode())
        );

        if (removed) {
            roleRepository.save(role);
            permissionEvaluationService.invalidateRolePermissions(role.getId());
            log.info("User {} revoked privileges from role {}",
                currentUser.getId(), role.getCode());
        }
    }

    @Override
    @Audited(
        action = AuditAction.UPDATE,
        entity = "UserPrivilege",
        dataClass = DataClass.CREDENTIALS,
        message = "Set user privilege override"
    )
    public void setUserPrivilegeOverride(SetUserPrivilegeOverrideRequest req) {
        Long currentUserId = SecurityContextUtils.getCurrentUserId();
        User currentUser = userRepository.findByIdSecure(currentUserId)
            .orElseThrow(() -> new SecurityException("Current user not found"));

        User targetUser = userRepository.findByIdSecure(req.getUserId())
            .orElseThrow(() -> new NoSuchElementException("User not found: " + req.getUserId()));

        if (!roleHierarchyService.canManageUser(currentUser, targetUser)) {
            throw new AccessDeniedException("Cannot manage user - insufficient privileges");
        }

        Privilege privilege = privilegeRepository.findByTenantIdAndCode(
            req.getTenantId(), req.getPrivilegeCode())
            .orElseThrow(() -> new NoSuchElementException("Privilege not found: " + req.getPrivilegeCode()));

        UserPrivilegeId id = new UserPrivilegeId(targetUser.getId(), privilege.getId());
        UserPrivilege userPrivilege = userPrivilegeRepository.findById(id)
            .orElse(UserPrivilege.builder()
                .id(id)
                .user(targetUser)
                .privilege(privilege)
                .build());

        userPrivilege.setGranted(Boolean.TRUE.equals(req.getGranted()));
        userPrivilege.setNotedBy(currentUserId);
        userPrivilege.setNotedAt(java.time.Instant.now());
        userPrivilegeRepository.save(userPrivilege);
        permissionEvaluationService.invalidateUserPermissions(targetUser.getId());

        log.info("User {} set privilege override {} = {} for user {}",
            currentUser.getId(), privilege.getCode(), req.getGranted(), targetUser.getId());
    }
}
