package com.ejada.sec.service;

import com.ejada.sec.domain.RolePrivilege;
import com.ejada.sec.domain.User;
import com.ejada.sec.domain.UserPrivilege;
import com.ejada.sec.domain.UserRole;
import com.ejada.sec.repository.RolePrivilegeRepository;
import com.ejada.sec.repository.UserPrivilegeRepository;
import com.ejada.sec.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * High-performance permission evaluation service with multi-level caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionEvaluationService {

    private final UserRepository userRepository;
    private final UserPrivilegeRepository userPrivilegeRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;
    private final CacheManager cacheManager;

    @Cacheable(value = "user-permissions", key = "#userId + ':' + #privilegeCode")
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, String privilegeCode) {
        log.debug("Evaluating permission: userId={}, privilege={}", userId, privilegeCode);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (!user.isEnabled() || user.isLocked()) {
            log.debug("Permission denied - user disabled or locked: {}", userId);
            return false;
        }

        Optional<UserPrivilege> override =
            userPrivilegeRepository.findByUserIdAndPrivilegeCode(userId, privilegeCode);

        if (override.isPresent()) {
            boolean granted = override.get().isGranted();
            log.debug("Permission {} for user {} resolved via override: {}",
                privilegeCode, userId, granted);
            return granted;
        }

        Set<Long> roleIds = user.getRoles().stream()
            .map(UserRole::getRole)
            .filter(role -> role != null && role.getId() != null)
            .map(role -> role.getId())
            .collect(Collectors.toCollection(HashSet::new));

        if (roleIds.isEmpty()) {
            log.debug("Permission denied - user {} has no roles", userId);
            return false;
        }

        boolean allowed = rolePrivilegeRepository.existsByRoleIdInAndPrivilegeCode(roleIds, privilegeCode);
        log.debug("Permission {} for user {} resolved via roles: {}",
            privilegeCode, userId, allowed);
        return allowed;
    }

    @Cacheable(value = "resource-permissions", key = "#userId + ':' + #resource + ':' + #action")
    @Transactional(readOnly = true)
    public boolean hasResourcePermission(Long userId, String resource, String action) {
        log.debug("Evaluating resource permission: userId={}, resource={}, action={}",
            userId, resource, action);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (!user.isEnabled() || user.isLocked()) {
            return false;
        }

        Set<Long> roleIds = user.getRoles().stream()
            .map(UserRole::getRole)
            .filter(role -> role != null && role.getId() != null)
            .map(role -> role.getId())
            .collect(Collectors.toCollection(HashSet::new));

        if (roleIds.isEmpty()) {
            return false;
        }

        return rolePrivilegeRepository.existsByRoleIdInAndResourceAndAction(roleIds, resource, action);
    }

    @Cacheable(value = "user-permission-map", key = "#userId")
    @Transactional(readOnly = true)
    public Map<String, Boolean> getUserPermissionMap(Long userId) {
        log.debug("Building permission map for user {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (!user.isEnabled() || user.isLocked()) {
            log.debug("Returning empty permissions - user disabled or locked: {}", userId);
            return Collections.emptyMap();
        }

        Map<String, Boolean> permissionMap = new HashMap<>();

        Set<Long> roleIds = user.getRoles().stream()
            .map(UserRole::getRole)
            .filter(role -> role != null && role.getId() != null)
            .map(role -> role.getId())
            .collect(Collectors.toSet());

        if (!roleIds.isEmpty()) {
            List<RolePrivilege> rolePrivileges = rolePrivilegeRepository.findByRoleIdIn(roleIds);
            rolePrivileges.forEach(rp -> permissionMap.put(rp.getPrivilege().getCode(), true));
            log.debug("Loaded {} role privileges for user {}", rolePrivileges.size(), userId);
        }

        List<UserPrivilege> overrides = userPrivilegeRepository.findByUserId(userId);
        overrides.forEach(up -> permissionMap.put(up.getPrivilege().getCode(), up.isGranted()));

        if (!overrides.isEmpty()) {
            log.debug("Applied {} user-specific overrides for user {}", overrides.size(), userId);
        }

        log.debug("Permission map for user {} contains {} entries", userId, permissionMap.size());
        return permissionMap;
    }

    public void invalidateUserPermissions(Long userId) {
        evictByPrefix("user-permissions", userId + ":");
        evictKey("user-permission-map", userId);
        evictByPrefix("resource-permissions", userId + ":");
        log.info("Invalidated cached permissions for user {}", userId);
    }

    public void invalidateRolePermissions(Long roleId) {
        List<Long> userIds = userRepository.findUserIdsByRoleId(roleId);
        userIds.forEach(this::invalidateUserPermissions);
        log.info("Invalidated permissions for {} users with role {}", userIds.size(), roleId);
    }

    public void invalidateAllPermissions() {
        clearCache("user-permissions");
        clearCache("user-permission-map");
        clearCache("resource-permissions");
        clearCache("role-privileges");
        log.warn("All permission caches cleared");
    }

    private void evictKey(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(key);
        }
    }

    private void evictByPrefix(String cacheName, String prefix) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache caffeineCache) {
            caffeineCache.getNativeCache().asMap().keySet().removeIf(key ->
                key instanceof String str && str.startsWith(prefix)
            );
        } else if (cache != null) {
            cache.clear();
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
