package com.ejada.admin.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.JwtTokenService;
import com.ejada.crypto.password.PasswordHasher;
import com.ejada.admin.domain.Superadmin;
import com.ejada.admin.domain.SuperadminPasswordHistory;
import com.ejada.admin.dto.*;
import com.ejada.admin.exception.PasswordHistoryUnavailableException;
import com.ejada.admin.mapper.SuperadminMapper;
import com.ejada.admin.repository.SuperadminPasswordHistoryRepository;
import com.ejada.admin.repository.SuperadminRepository;
import com.ejada.admin.service.SuperadminService;
import com.ejada.starter_security.Role;
import com.ejada.starter_security.RoleChecker;

import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.DataClass;
import com.ejada.audit.starter.api.annotations.Audited;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SuperadminServiceImpl implements SuperadminService {

    private final SuperadminRepository superadminRepository;
    private final SuperadminMapper superadminMapper;
    private final JwtTokenService jwtTokenService;
    private final SuperadminPasswordHistoryRepository passwordHistoryRepository;
    private final RoleChecker roleChecker;

    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d\\d\\$[./0-9A-Za-z]{53}$");

    @Value("${shared.security.superadmin.password-expiry-days:90}")
    private int passwordExpiryDays;

    @Value("${shared.security.superadmin.min-active-admins:1}")
    private int minActiveSuperadmins;

    @Value("${shared.security.jwt.superadmin-ttl:PT24H}")
    private Duration superadminTokenTtl;

    @Value("${shared.security.superadmin.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${shared.security.superadmin.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    /* ========= Raw (cacheable) readers ========= */

    @Cacheable(cacheNames = "superadmins:byId", key = "#id")
    public Superadmin getRawById(Long id) {
        return superadminRepository.findById(id).orElse(null);
    }

    @Cacheable(cacheNames = "superadmins:byIdentifier", key = "#identifier")
    public Superadmin getRawByIdentifier(String identifier) {
        return superadminRepository.findByIdentifier(identifier).orElse(null);
    }

    public Page<Superadmin> getRawPage(Pageable pageable) {
        return superadminRepository.findAll(pageable);
    }

    /* ========= API methods (audited + safe responses) ========= */

    @Override
    @Audited(action = AuditAction.CREATE, entity = "Superadmin", dataClass = DataClass.CREDENTIALS, message = "Create superadmin")
    @CacheEvict(cacheNames = {"superadmins:byId", "superadmins:byIdentifier", "superadmins:list"}, allEntries = true)
    public BaseResponse<SuperadminDto> createSuperadmin(CreateSuperadminRequest request) {
        try {
            if (!validateSuperadminAccessSafe()) {
                return BaseResponse.error("ERR_FORBIDDEN", "Access denied. Only superadmins can perform this action");
            }

            if (superadminRepository.existsByUsername(request.getUsername())) {
                return BaseResponse.error("ERR_USERNAME_EXISTS", "Username already exists: " + request.getUsername());
            }
            if (superadminRepository.existsByEmail(request.getEmail())) {
                return BaseResponse.error("ERR_EMAIL_EXISTS", "Email already exists: " + request.getEmail());
            }

            try {
                validatePasswordComplexity(request.getPassword());
            } catch (IllegalArgumentException iae) {
                return BaseResponse.error("ERR_PASSWORD_POLICY", iae.getMessage());
            }

            Superadmin entity = superadminMapper.toEntity(request);
            entity.setPasswordHash(PasswordHasher.bcrypt(request.getPassword()));
            entity.setPasswordChangedAt(LocalDateTime.now());
            entity.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays));

            entity = superadminRepository.save(entity);
            recordPasswordHistorySafe(entity);

            BaseResponse<SuperadminDto> resp =
                    BaseResponse.success("Superadmin created successfully", superadminMapper.toDto(entity));
            resp.setCode("SUCCESS-201");
            return resp;

        } catch (Exception ex) {
            log.error("Create superadmin failed", ex);
            return BaseResponse.error("ERR_SUPERADMIN_CREATE", "Failed to create superadmin");
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Superadmin", dataClass = DataClass.CREDENTIALS, message = "Update superadmin")
    @CacheEvict(cacheNames = {"superadmins:byId", "superadmins:byIdentifier", "superadmins:list"}, allEntries = true)
    public BaseResponse<SuperadminDto> updateSuperadmin(Long id, UpdateSuperadminRequest request) {
        try {
            if (!validateSuperadminAccessSafe()) {
                return BaseResponse.error("ERR_FORBIDDEN", "Access denied. Only superadmins can perform this action");
            }

            Superadmin superadmin = getRawById(id);
            if (superadmin == null) {
                return BaseResponse.error("ERR_NOT_FOUND", "Superadmin not found with ID: " + id);
            }

            if (request.getEmail() != null && !request.getEmail().equals(superadmin.getEmail())) {
                if (superadminRepository.existsByEmail(request.getEmail())) {
                    return BaseResponse.error("ERR_EMAIL_EXISTS", "Email already exists: " + request.getEmail());
                }
            }

            superadminMapper.updateEntity(superadmin, request);
            superadmin = superadminRepository.save(superadmin);

            return BaseResponse.success("Superadmin updated successfully", superadminMapper.toDto(superadmin));
        } catch (Exception ex) {
            log.error("Update superadmin failed", ex);
            return BaseResponse.error("ERR_SUPERADMIN_UPDATE", "Failed to update superadmin");
        }
    }

    @Override
    @Audited(action = AuditAction.DELETE, entity = "Superadmin", dataClass = DataClass.CREDENTIALS, message = "Disable (soft delete) superadmin")
    @CacheEvict(cacheNames = {"superadmins:byId", "superadmins:byIdentifier", "superadmins:list"}, allEntries = true)
    public BaseResponse<Void> deleteSuperadmin(Long id) {
        try {
            if (!validateSuperadminAccessSafe()) {
                return BaseResponse.error("ERR_FORBIDDEN", "Access denied. Only superadmins can perform this action");
            }

            Long currentId = getCurrentSuperadminIdOrNull();
            if (currentId != null && id.equals(currentId)) {
                return BaseResponse.error("ERR_DELETE_SELF", "Cannot delete your own superadmin account");
            }

            long active = superadminRepository.countActiveSuperadmins();
            if (active <= minActiveSuperadmins) {
                return BaseResponse.error("ERR_MIN_ACTIVE",
                        String.format("Cannot delete superadmin. At least %d active superadmin(s) must exist", minActiveSuperadmins));
            }

            Superadmin superadmin = getRawById(id);
            if (superadmin == null) {
                return BaseResponse.error("ERR_NOT_FOUND", "Superadmin not found with ID: " + id);
            }

            superadmin.setEnabled(false);
            superadmin.setLocked(true);
            superadminRepository.save(superadmin);

            return BaseResponse.success("Superadmin deleted successfully", null);
        } catch (Exception ex) {
            log.error("Delete superadmin failed", ex);
            return BaseResponse.error("ERR_SUPERADMIN_DELETE", "Failed to delete superadmin");
        }
    }

    @Override
    @Audited(action = AuditAction.READ, entity = "Superadmin", dataClass = DataClass.CREDENTIALS, message = "Get superadmin by id")
    public BaseResponse<SuperadminDto> getSuperadmin(Long id) {
        try {
            if (!validateSuperadminAccessSafe()) {
                return BaseResponse.error("ERR_FORBIDDEN", "Access denied. Only superadmins can perform this action");
            }
            Superadmin s = getRawById(id);
            if (s == null) {
                return BaseResponse.error("ERR_NOT_FOUND", "Superadmin not found with ID: " + id);
            }
            return BaseResponse.success("Superadmin fetched successfully", superadminMapper.toDto(s));
        } catch (Exception ex) {
            log.error("Fetch superadmin failed", ex);
            return BaseResponse.error("ERR_SUPERADMIN_GET", "Failed to fetch superadmin");
        }
    }

    @Override
    @Audited(action = AuditAction.READ, entity = "Superadmin", dataClass = DataClass.CREDENTIALS, message = "List superadmins")
    public BaseResponse<Page<SuperadminDto>> listSuperadmins(Pageable pageable) {
        try {
            if (!validateSuperadminAccessSafe()) {
                return BaseResponse.error("ERR_FORBIDDEN", "Access denied. Only superadmins can perform this action");
            }

            Page<Superadmin> page = getRawPage(pageable);

            if (page.isEmpty() && page.getTotalElements() > 0 && pageable.getPageNumber() >= page.getTotalPages()) {
                int last = Math.max(0, page.getTotalPages() - 1);
                page = getRawPage(
                    PageRequest.of(last, pageable.getPageSize(), pageable.getSort())
                );            }

            Page<SuperadminDto> dtoPage = page.map(superadminMapper::toDto);
            return BaseResponse.success("Superadmins listed successfully", dtoPage);
        } catch (Exception ex) {
            log.error("List superadmins failed", ex);
            return BaseResponse.success("Superadmins listed successfully",
                    new PageImpl<>(Collections.emptyList(), pageable, 0));
        }
    }

    @Override
    @Audited(action = AuditAction.ACCESS, entity = "Superadmin", dataClass = DataClass.CREDENTIALS, message = "Superadmin login")
    public BaseResponse<SuperadminAuthResponse> login(SuperadminLoginRequest request) {
        try {
            Superadmin superadmin = getRawByIdentifier(request.getIdentifier());
            if (superadmin == null) {
                log.warn("Login failed: user not found {}", request.getIdentifier());
                return BaseResponse.error("ERR_LOGIN_INVALID", "Invalid credentials");
            }

            if (superadmin.getLockedUntil() != null && LocalDateTime.now().isBefore(superadmin.getLockedUntil())) {
                return BaseResponse.error("ERR_ACCOUNT_TEMP_LOCKED", "Account is temporarily locked. Please try again later.");
            }

            String currentHash = requirePasswordHash(superadmin);
            if (!PasswordHasher.matchesBcrypt(request.getPassword(), currentHash)) {
                handleFailedLogin(superadmin);
                return BaseResponse.error("ERR_LOGIN_INVALID", "Invalid credentials");
            }

            if (!superadmin.isEnabled()) {
                return BaseResponse.error("ERR_ACCOUNT_DISABLED", "Account is disabled. Please contact system administrator.");
            }
            if (superadmin.isLocked()) {
                return BaseResponse.error("ERR_ACCOUNT_LOCKED", "Account is locked. Please contact system administrator.");
            }

            if (!superadmin.isFirstLoginCompleted()) {
                String firstLoginToken = generateFirstLoginToken(superadmin);
                return BaseResponse.success("First login - password change required",
                        SuperadminAuthResponse.builder()
                                .accessToken(firstLoginToken)
                                .tokenType("Bearer")
                                .expiresInSeconds(superadminTokenTtl.getSeconds())
                                .role("EJADA_OFFICER")
                                .permissions(List.of("CHANGE_PASSWORD"))
                                .requiresPasswordChange(true)
                                .build());
            }

            if (superadmin.getPasswordExpiresAt() != null &&
                    LocalDateTime.now().isAfter(superadmin.getPasswordExpiresAt())) {
                String expiredToken = generateExpiredPasswordToken(superadmin);
                return BaseResponse.success("Password expired - change required",
                        SuperadminAuthResponse.builder()
                                .accessToken(expiredToken)
                                .tokenType("Bearer")
                                .expiresInSeconds(superadminTokenTtl.getSeconds())
                                .role("EJADA_OFFICER")
                                .permissions(List.of("CHANGE_PASSWORD"))
                                .passwordExpired(true)
                                .build());
            }

            superadmin.setFailedLoginAttempts(0);
            superadmin.setLockedUntil(null);
            superadmin.setLastLoginAt(LocalDateTime.now());
            superadminRepository.save(superadmin);

            String token = generateSuperadminToken(superadmin);
            return BaseResponse.success("Login successful",
                    SuperadminAuthResponse.builder()
                            .accessToken(token)
                            .tokenType("Bearer")
                            .expiresInSeconds(superadminTokenTtl.getSeconds())
                            .role("EJADA_OFFICER")
                            .permissions(List.of(
                                    "TENANT_CREATE", "TENANT_UPDATE", "TENANT_DELETE",
                                    "TENANT_VIEW", "GLOBAL_CONFIG", "SYSTEM_ADMIN"))
                            .requiresPasswordChange(false)
                            .passwordExpired(false)
                            .build());
        } catch (Exception ex) {
            log.error("Login failed", ex);
            return BaseResponse.error("ERR_LOGIN", "Failed to login");
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Superadmin", dataClass = DataClass.CREDENTIALS, message = "Complete first login")
    @CacheEvict(cacheNames = {"superadmins:byId", "superadmins:byIdentifier", "superadmins:list"}, allEntries = true)
    public BaseResponse<Void> completeFirstLogin(FirstLoginRequest request) {
        try {
            Long id = getCurrentSuperadminId();
            Superadmin superadmin = getRawById(id);
            if (superadmin == null) {
                return BaseResponse.error("ERR_NOT_FOUND", "Superadmin not found");
            }
            if (superadmin.isFirstLoginCompleted()) {
                return BaseResponse.error("ERR_ALREADY_DONE", "First login has already been completed");
            }

            String currentHash = requirePasswordHash(superadmin);
            if (!PasswordHasher.matchesBcrypt(request.getCurrentPassword(), currentHash)) {
                return BaseResponse.error("ERR_CURRENT_PASSWORD", "Current password is incorrect");
            }
            if (request.getCurrentPassword().equals(request.getNewPassword())) {
                return BaseResponse.error("ERR_SAME_PASSWORD", "New password must be different from current password");
            }
            if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
                return BaseResponse.error("ERR_CONFIRM_PASSWORD", "New password and confirmation do not match");
            }

            try {
                validatePasswordComplexity(request.getNewPassword());
                ensurePasswordNotReused(superadmin.getId(), request.getNewPassword());
            } catch (PasswordHistoryUnavailableException phe) {
                return BaseResponse.error("ERR_PASSWORD_HISTORY", phe.getMessage());
            } catch (IllegalArgumentException iae) {
                return BaseResponse.error("ERR_PASSWORD_POLICY", iae.getMessage());
            }

            superadmin.setPasswordHash(PasswordHasher.bcrypt(request.getNewPassword()));
            superadmin.setFirstLoginCompleted(true);
            superadmin.setPasswordChangedAt(LocalDateTime.now());
            superadmin.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays));

            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                superadmin.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null && !request.getLastName().isBlank()) {
                superadmin.setLastName(request.getLastName());
            }
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
                superadmin.setPhoneNumber(request.getPhoneNumber());
            }

            superadminRepository.save(superadmin);
            recordPasswordHistorySafe(superadmin);

            return BaseResponse.success(
                    "First login completed successfully. Please login again with your new password.", null);
        } catch (AuthenticationCredentialsNotFoundException | AccessDeniedException sec) {
            return BaseResponse.error("ERR_UNAUTHENTICATED", sec.getMessage());
        } catch (Exception ex) {
            log.error("Complete first login failed", ex);
            return BaseResponse.error("ERR_FIRST_LOGIN", "Failed to complete first login");
        }
    }

    @Override
    @Audited(action = AuditAction.UPDATE, entity = "Superadmin", dataClass = DataClass.CREDENTIALS, message = "Change password")
    @CacheEvict(cacheNames = {"superadmins:byId", "superadmins:byIdentifier", "superadmins:list"}, allEntries = true)
    public BaseResponse<Void> changePassword(ChangePasswordRequest request) {
        try {
            Long id = getCurrentSuperadminId();
            Superadmin superadmin = getRawById(id);
            if (superadmin == null) {
                return BaseResponse.error("ERR_NOT_FOUND", "Superadmin not found");
            }

            String currentHash = requirePasswordHash(superadmin);
            if (!PasswordHasher.matchesBcrypt(request.getCurrentPassword(), currentHash)) {
                return BaseResponse.error("ERR_CURRENT_PASSWORD", "Current password is incorrect");
            }
            if (request.getCurrentPassword().equals(request.getNewPassword())) {
                return BaseResponse.error("ERR_SAME_PASSWORD", "New password must be different from current password");
            }
            if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
                return BaseResponse.error("ERR_CONFIRM_PASSWORD", "New password and confirmation do not match");
            }

            try {
                validatePasswordComplexity(request.getNewPassword());
                ensurePasswordNotReused(superadmin.getId(), request.getNewPassword());
            } catch (PasswordHistoryUnavailableException phe) {
                return BaseResponse.error("ERR_PASSWORD_HISTORY", phe.getMessage());
            } catch (IllegalArgumentException iae) {
                return BaseResponse.error("ERR_PASSWORD_POLICY", iae.getMessage());
            }

            superadmin.setPasswordHash(PasswordHasher.bcrypt(request.getNewPassword()));
            superadmin.setPasswordChangedAt(LocalDateTime.now());
            superadmin.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays));

            superadminRepository.save(superadmin);
            recordPasswordHistorySafe(superadmin);

            return BaseResponse.success("Password changed successfully", null);
        } catch (AuthenticationCredentialsNotFoundException | AccessDeniedException sec) {
            return BaseResponse.error("ERR_UNAUTHENTICATED", sec.getMessage());
        } catch (Exception ex) {
            log.error("Change password failed", ex);
            return BaseResponse.error("ERR_PASSWORD_CHANGE", "Failed to change password");
        }
    }

    /* ========= Helpers ========= */

    private boolean validateSuperadminAccessSafe() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) return false;
        if (!roleChecker.hasRole(authentication, Role.EJADA_OFFICER)) return false;
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Long id = resolveSuperadminIdFromJwt(jwt);
            return id != null;
        }
        return true;
    }

    private Long getCurrentSuperadminId() {
        Long id = getCurrentSuperadminIdOrNull();
        if (id != null) return id;
        throw new AuthenticationCredentialsNotFoundException("No authenticated superadmin found");
    }

    private Long getCurrentSuperadminIdOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        if (auth.getPrincipal() instanceof Jwt jwt) {
            return resolveSuperadminIdFromJwt(jwt);
        }
        String name = auth.getName();
        if (name != null && !name.isBlank()) {
            return superadminRepository.findByIdentifier(name).map(Superadmin::getId).orElse(null);
        }
        return null;
    }

    private Long resolveSuperadminIdFromJwt(Jwt jwt) {
        Superadmin s = null;

        Object uid = jwt.getClaim("uid");
        if (uid != null) {
            try {
                Long id = Long.valueOf(uid.toString());
                s = superadminRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignored) {
                log.warn("Invalid uid claim: {}", uid);
            }
        }

        if (s == null) {
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                s = superadminRepository.findByIdentifier(sub).orElse(null);
            }
        }

        if (s == null) return null;

        ensureTokenFreshness(jwt, s);
        return s.getId();
    }

    private void ensureTokenFreshness(Jwt jwt, Superadmin s) {
        LocalDateTime changed = s.getPasswordChangedAt();
        if (changed == null) return;

        Instant iat = jwt.getIssuedAt();
        Instant changedInstant = changed.atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.MILLIS);

        if (iat == null || iat.isBefore(changedInstant)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication token is no longer valid because the password was changed. Please sign in again.");
        }
    }

    private String requirePasswordHash(Superadmin s) {
        String hash = s.getPasswordHash();
        if (hash == null || hash.isBlank()) {
            throw new IllegalStateException("The account password is not set. Please contact a system administrator.");
        }
        return hash;
    }

    private void handleFailedLogin(Superadmin s) {
        s.setFailedLoginAttempts(s.getFailedLoginAttempts() + 1);
        if (s.getFailedLoginAttempts() >= maxFailedAttempts) {
            s.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            log.warn("Account temporarily locked due to {} failed attempts: {}", maxFailedAttempts, s.getUsername());
        }
        superadminRepository.save(s);
    }

    private void recordPasswordHistorySafe(Superadmin s) {
        if (s.getId() == null) return;
        try {
            SuperadminPasswordHistory h = SuperadminPasswordHistory.builder()
                    .superadminId(s.getId())
                    .passwordHash(s.getPasswordHash())
                    .build();
            passwordHistoryRepository.save(h);
        } catch (DataAccessException ex) {
            log.warn("Failed to record password history for superadmin {}", s.getId(), ex);
        }
    }

    private void ensurePasswordNotReused(Long superadminId, String candidatePassword) {
        if (superadminId == null) return;

        try {
            List<SuperadminPasswordHistory> recent =
                    passwordHistoryRepository.findTop5BySuperadminIdOrderByCreatedAtDesc(superadminId);

            for (SuperadminPasswordHistory entry : recent) {
                String historicalHash = entry.getPasswordHash();
                if (historicalHash == null || historicalHash.isBlank()) {
                    log.warn("Skipping password history entry {} for superadmin {} due to empty hash",
                            entry.getId(), superadminId);
                    continue;
                }

                if (!BCRYPT_PATTERN.matcher(historicalHash).matches()) {
                    log.error("Invalid password hash format in history entry {} for superadmin {}", entry.getId(), superadminId);
                    throw new PasswordHistoryUnavailableException(
                            "Unable to verify password history at the moment. Please try again later.",
                            new IllegalStateException("Invalid password hash format for history entry " + entry.getId()));
                }

                boolean matches;
                try {
                    matches = PasswordHasher.matchesBcrypt(candidatePassword, historicalHash);
                } catch (IllegalArgumentException ex) {
                    log.error("Invalid password hash detected for superadmin {} in history entry {}", superadminId, entry.getId(), ex);
                    throw new PasswordHistoryUnavailableException(
                            "Unable to verify password history at the moment. Please try again later.", ex);
                }

                if (matches) {
                    throw new IllegalArgumentException("New password cannot match any of your last 5 passwords");
                }
            }
        } catch (DataAccessException ex) {
            log.error("Unable to verify password history for superadmin {}", superadminId, ex);
            throw new PasswordHistoryUnavailableException(
                    "Unable to verify password history at the moment. Please try again later.", ex);
        }
    }

    private String generateFirstLoginToken(Superadmin s) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", s.getUsername());
        claims.put("uid", s.getId());
        claims.put("email", s.getEmail());
        claims.put("roles", List.of("EJADA_OFFICER"));
        claims.put("isSuperadmin", true);
        claims.put("requiresPasswordChange", true);
        claims.put("accountState", "FIRST_LOGIN");

        return jwtTokenService.createToken(s.getUsername(), null, List.of("EJADA_OFFICER"), claims, superadminTokenTtl);
    }

    private String generateExpiredPasswordToken(Superadmin s) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", s.getUsername());
        claims.put("uid", s.getId());
        claims.put("email", s.getEmail());
        claims.put("roles", List.of("EJADA_OFFICER"));
        claims.put("isSuperadmin", true);
        claims.put("passwordExpired", true);
        claims.put("accountState", "PASSWORD_EXPIRED");

        return jwtTokenService.createToken(s.getUsername(), null, List.of("EJADA_OFFICER"), claims, superadminTokenTtl);
    }

    private String generateSuperadminToken(Superadmin s) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", s.getUsername());
        claims.put("uid", s.getId());
        claims.put("email", s.getEmail());
        claims.put("roles", List.of("EJADA_OFFICER"));
        claims.put("isSuperadmin", true);
        claims.put("accountState", "ACTIVE");

        String fullName = "";
        if (s.getFirstName() != null && s.getLastName() != null) {
            fullName = s.getFirstName() + " " + s.getLastName();
        }
        claims.put("fullName", fullName);

        return jwtTokenService.createToken(s.getUsername(), null, List.of("EJADA_OFFICER"), claims, superadminTokenTtl);
    }

    private void validatePasswordComplexity(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < 8) errors.add("Password must be at least 8 characters long");
        if (!Pattern.compile("[A-Z]").matcher(password).find()) errors.add("Password must contain at least one uppercase letter");
        if (!Pattern.compile("[a-z]").matcher(password).find()) errors.add("Password must contain at least one lowercase letter");
        if (!Pattern.compile("\\d").matcher(password).find()) errors.add("Password must contain at least one digit");
        if (!Pattern.compile("[@$!%*?&]").matcher(password).find()) errors.add("Password must contain at least one special character (@$!%*?&)");

        Set<String> commonPasswords = Set.of("Password123!", "Admin123!", "Welcome123!", "Password@123", "Admin@123", "Welcome@123");
        if (commonPasswords.stream().anyMatch(common -> common.equalsIgnoreCase(password))) {
            errors.add("This password is too common. Please choose a more unique password");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Password validation failed: " + String.join(", ", errors));
        }
    }
}
