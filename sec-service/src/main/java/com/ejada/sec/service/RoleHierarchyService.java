package com.ejada.sec.service;

import com.ejada.sec.domain.Role;
import com.ejada.sec.domain.RoleLevel;
import com.ejada.sec.domain.User;
import com.ejada.sec.domain.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for enforcing role hierarchy rules and validating
 * management permissions based on role levels.
 *
 * <p><b>Core Rules:</b>
 * <ol>
 *   <li>Users can only manage users with strictly lower role levels</li>
 *   <li>Users can only assign roles with strictly lower levels</li>
 *   <li>Platform admins bypass tenant restrictions</li>
 *   <li>Non-platform users require same-tenant for management</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleHierarchyService {

    /**
     * Gets the highest role level a user currently holds.
     *
     * @param user the user to check
     * @return highest RoleLevel the user has or {@link RoleLevel#GUEST}
     */
    public RoleLevel getUserHighestRole(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return RoleLevel.GUEST;
        }

        return user.getRoles().stream()
            .map(UserRole::getRole)
            .map(Role::getLevel)
            .max((a, b) -> Integer.compare(a.getLevel(), b.getLevel()))
            .orElse(RoleLevel.GUEST);
    }

    /**
     * Gets all role levels a user currently holds.
     *
     * @param user the user to check
     * @return set of all RoleLevels the user has
     */
    public Set<RoleLevel> getUserRoleLevels(User user) {
        if (user == null || user.getRoles() == null) {
            return Set.of(RoleLevel.GUEST);
        }

        return user.getRoles().stream()
            .map(UserRole::getRole)
            .map(Role::getLevel)
            .collect(Collectors.toSet());
    }

    /**
     * Checks if actor user can perform management actions on target user.
     *
     * @param actor the user attempting to manage
     * @param target the user being managed
     * @return true if management is allowed
     */
    public boolean canManageUser(User actor, User target) {
        if (actor == null || target == null) {
            log.warn("Cannot validate management: actor or target is null");
            return false;
        }

        if (actor.getId().equals(target.getId())) {
            log.debug("User {} cannot manage themselves", actor.getId());
            return false;
        }

        RoleLevel actorLevel = getUserHighestRole(actor);
        RoleLevel targetLevel = getUserHighestRole(target);

        if (actorLevel.isPlatformRole()) {
            log.debug("Platform admin {} can manage user {}", actor.getId(), target.getId());
            return true;
        }

        if (!actor.getTenantId().equals(target.getTenantId())) {
            log.debug("User {} cannot manage user {} - different tenants", actor.getId(), target.getId());
            return false;
        }

        boolean canManage = actorLevel.hasHigherPrivilege(targetLevel);

        log.debug("User {} (level {}) {} manage user {} (level {})",
            actor.getId(), actorLevel.getLevel(),
            canManage ? "CAN" : "CANNOT",
            target.getId(), targetLevel.getLevel());

        return canManage;
    }

    /**
     * Checks if user can assign a specific role to another user.
     *
     * @param actor the user attempting to assign role
     * @param targetRole the role being assigned
     * @return true if assignment is allowed
     */
    public boolean canAssignRole(User actor, Role targetRole) {
        if (actor == null || targetRole == null) {
            log.warn("Cannot validate role assignment: actor or role is null");
            return false;
        }

        RoleLevel actorLevel = getUserHighestRole(actor);

        if (actorLevel.isPlatformRole()) {
            log.debug("Platform admin {} can assign any role", actor.getId());
            return true;
        }

        if (!actor.getTenantId().equals(targetRole.getTenantId())) {
            log.debug("User {} cannot assign role {} - different tenants", actor.getId(), targetRole.getCode());
            return false;
        }

        boolean canAssign = actorLevel.hasHigherPrivilege(targetRole.getLevel());

        log.debug("User {} (level {}) {} assign role {} (level {})",
            actor.getId(), actorLevel.getLevel(),
            canAssign ? "CAN" : "CANNOT",
            targetRole.getCode(), targetRole.getLevel().getLevel());

        return canAssign;
    }

    /**
     * Gets all roles a user is permitted to assign from a given set.
     */
    public Set<Role> getAssignableRoles(User actor, Set<Role> allRoles) {
        if (actor == null || allRoles == null || allRoles.isEmpty()) {
            return Set.of();
        }

        RoleLevel actorLevel = getUserHighestRole(actor);
        UUID actorTenantId = actor.getTenantId();

        if (actorLevel.isPlatformRole()) {
            return allRoles;
        }

        return allRoles.stream()
            .filter(role -> role.getTenantId().equals(actorTenantId))
            .filter(role -> actorLevel.hasHigherPrivilege(role.getLevel()))
            .collect(Collectors.toSet());
    }

    /**
     * Validates if actor can revoke a role from target user.
     */
    public boolean canRevokeRole(User actor, User targetUser, Role roleToRevoke) {
        if (!canManageUser(actor, targetUser)) {
            return false;
        }

        return canAssignRole(actor, roleToRevoke);
    }

    /**
     * Checks if user can modify another user (enable/disable etc.).
     */
    public boolean canModifyUser(User actor, User target) {
        return canManageUser(actor, target);
    }

    /**
     * Checks if user can delete another user.
     */
    public boolean canDeleteUser(User actor, User target) {
        if (!canManageUser(actor, target)) {
            return false;
        }

        return true;
    }

    /**
     * Gets list of role levels that are manageable by given role level.
     */
    public List<RoleLevel> getManageableRoleLevels(RoleLevel actorLevel) {
        return actorLevel.getManageableRoles();
    }

    /**
     * Validates role assignment request for business logic violations.
     */
    public RoleAssignmentValidation validateRoleAssignment(
        User actor, User targetUser, List<Role> rolesToAssign) {

        RoleAssignmentValidation validation = new RoleAssignmentValidation();

        if (!canManageUser(actor, targetUser)) {
            validation.addError(String.format(
                "Cannot manage user %s - insufficient privileges or different tenant",
                targetUser.getUsername()));
            return validation;
        }

        for (Role role : rolesToAssign) {
            if (!canAssignRole(actor, role)) {
                validation.addError(String.format(
                    "Cannot assign role '%s' (level %d) - insufficient privileges",
                    role.getCode(), role.getLevel().getLevel()));
            }
        }

        return validation;
    }

    /**
     * Helper class for role assignment validation results.
     */
    @Getter
    public static class RoleAssignmentValidation {
        private boolean valid = true;
        private final List<String> errors = new ArrayList<>();

        public void addError(String error) {
            this.valid = false;
            this.errors.add(error);
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
