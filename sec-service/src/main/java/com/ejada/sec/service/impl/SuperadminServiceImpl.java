package com.ejada.sec.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.crypto.JwtTokenService;
import com.ejada.crypto.password.PasswordHasher;
import com.ejada.sec.domain.Superadmin;
import com.ejada.sec.domain.SuperadminPasswordHistory;
import com.ejada.sec.dto.admin.*;
import com.ejada.sec.exception.PasswordHistoryUnavailableException;
import com.ejada.sec.mapper.SuperadminMapper;
import com.ejada.sec.repository.SuperadminPasswordHistoryRepository;
import com.ejada.sec.repository.SuperadminRepository;
import com.ejada.sec.service.SuperadminService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SuperadminServiceImpl implements SuperadminService {
    
    private final SuperadminRepository superadminRepository;
    private final SuperadminMapper superadminMapper;
    private final JwtTokenService jwtTokenService;
    private final SuperadminPasswordHistoryRepository passwordHistoryRepository;
    private final SuperadminAuditService superadminAuditService;
    
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
    
    @Override
    public BaseResponse<SuperadminDto> createSuperadmin(CreateSuperadminRequest request) {
        log.info("Creating new superadmin with username: {}", request.getUsername());
        
        // Validate that the current user is a superadmin
        validateSuperadminAccess();
        
        // Check if username already exists
        if (superadminRepository.existsByUsername(request.getUsername())) {
            log.error("Username already exists: {}", request.getUsername());
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        
        // Check if email already exists
        if (superadminRepository.existsByEmail(request.getEmail())) {
            log.error("Email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        
        // Validate password complexity
        validatePasswordComplexity(request.getPassword());
        
        // Map request to entity
        Superadmin superadmin = superadminMapper.toEntity(request);
        
        // Set password hash
        superadmin.setPasswordHash(PasswordHasher.bcrypt(request.getPassword()));
        
        // Set initial password expiry
        superadmin.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays));
        
        // Set audit fields (only if they exist in your AuditableEntity)
        // If AuditableEntity handles these automatically, you can remove these lines
        if (superadmin.getCreatedAt() == null) {
            superadmin.setCreatedAt(LocalDateTime.now());
        }
        // Remove these if your AuditableEntity doesn't have createdBy/updatedBy
        // superadmin.setCreatedBy(getCurrentSuperadminUsername());
        
        // Save to database
        superadmin = superadminRepository.save(superadmin);

        recordPasswordHistory(superadmin);

        // Log the action for audit
        logSuperadminAction(
            "CREATE_SUPERADMIN",
            getCurrentSuperadminIdOrNull(),
            getCurrentSuperadminUsername(),
            String.format("Created new superadmin: %s (%s)", request.getUsername(), request.getEmail()));
        
        // Send welcome email (optional)
        sendWelcomeEmail(superadmin);
        
        log.info("Superadmin created successfully with ID: {}", superadmin.getId());
        
        // Map to DTO and return
        SuperadminDto dto = superadminMapper.toDto(superadmin);
        return BaseResponse.success("Superadmin created successfully", dto);
    }
    
    @Override
    public BaseResponse<SuperadminDto> updateSuperadmin(Long id, UpdateSuperadminRequest request) {
        log.info("Updating superadmin with ID: {}", id);
        
        // Validate superadmin access
        validateSuperadminAccess();
        
        // Find existing superadmin
        Superadmin superadmin = superadminRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Superadmin not found with ID: " + id));
        
        // Check if email is being changed and if it's already taken
        if (request.getEmail() != null && !request.getEmail().equals(superadmin.getEmail())) {
            if (superadminRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
        }
        
        // Update entity fields
        superadminMapper.updateEntity(superadmin, request);
        
        // Update audit fields
        superadmin.setUpdatedAt(LocalDateTime.now());
        // Remove this if your AuditableEntity doesn't have updatedBy
        // superadmin.setUpdatedBy(getCurrentSuperadminUsername());
        
        // Save changes
        superadmin = superadminRepository.save(superadmin);
        
        // Log the action
        logSuperadminAction(
            "UPDATE_SUPERADMIN",
            getCurrentSuperadminIdOrNull(),
            getCurrentSuperadminUsername(),
            String.format("Updated superadmin ID: %d", id));
        
        log.info("Superadmin updated successfully");
        
        return BaseResponse.success("Superadmin updated successfully", 
            superadminMapper.toDto(superadmin));
    }
    
    @Override
    public BaseResponse<Void> deleteSuperadmin(Long id) {
        log.info("Attempting to delete superadmin with ID: {}", id);
        
        // Validate superadmin access
        validateSuperadminAccess();
        
        // Prevent deleting yourself
        Long currentSuperadminId = getCurrentSuperadminId();
        if (id.equals(currentSuperadminId)) {
            throw new IllegalStateException("Cannot delete your own superadmin account");
        }
        
        // Check minimum superadmin count
        long activeSuperadmins = superadminRepository.countActiveSuperadmins();
        if (activeSuperadmins <= minActiveSuperadmins) {
            throw new IllegalStateException(
                String.format("Cannot delete superadmin. At least %d active superadmin(s) must exist", 
                    minActiveSuperadmins));
        }
        
        // Find superadmin
        Superadmin superadmin = superadminRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Superadmin not found with ID: " + id));
        
        // Soft delete (disable and lock the account instead of hard delete)
        superadmin.setEnabled(false);
        superadmin.setLocked(true);
        superadmin.setUpdatedAt(LocalDateTime.now());
        // Remove this if your AuditableEntity doesn't have updatedBy
        // superadmin.setUpdatedBy(getCurrentSuperadminUsername());
        
        superadminRepository.save(superadmin);
        
        // Log the action
        logSuperadminAction(
            "DELETE_SUPERADMIN",
            getCurrentSuperadminIdOrNull(),
            getCurrentSuperadminUsername(),
            String.format("Deleted (disabled) superadmin: %s", superadmin.getUsername()));
        
        log.info("Superadmin deleted (disabled) successfully");
        
        return BaseResponse.success("Superadmin deleted successfully", null);
    }
    
    @Override
    public BaseResponse<SuperadminDto> getSuperadmin(Long id) {
        log.debug("Fetching superadmin with ID: {}", id);
        
        // Validate superadmin access
        validateSuperadminAccess();
        
        Superadmin superadmin = superadminRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Superadmin not found with ID: " + id));
        
        return BaseResponse.success("Superadmin fetched successfully", 
            superadminMapper.toDto(superadmin));
    }
    
    @Override
    public BaseResponse<Page<SuperadminDto>> listSuperadmins(Pageable pageable) {
        log.debug("Listing all superadmins with pagination: {}", pageable);
        
        // Validate superadmin access
        validateSuperadminAccess();
        
        Page<Superadmin> superadminPage = superadminRepository.findAll(pageable);
        Page<SuperadminDto> dtoPage = superadminPage.map(superadminMapper::toDto);
        
        return BaseResponse.success("Superadmins listed successfully", dtoPage);
    }
    
    @Override
    @Transactional(noRollbackFor = NoSuchElementException.class)
    public BaseResponse<SuperadminAuthResponse> login(SuperadminLoginRequest request) {
        log.info("Superadmin login attempt for: {}", request.getIdentifier());
        
        // Find superadmin by username or email
        Superadmin superadmin = superadminRepository
            .findByIdentifier(request.getIdentifier())
            .orElseThrow(() -> {
                log.warn("Login failed: User not found - {}", request.getIdentifier());
                return new NoSuchElementException("Invalid credentials");
            });
        
        // Check if account is temporarily locked
        if (superadmin.getLockedUntil() != null && 
            LocalDateTime.now().isBefore(superadmin.getLockedUntil())) {
            log.warn("Login denied: Account temporarily locked for {}", superadmin.getUsername());
            throw new IllegalStateException("Account is temporarily locked. Please try again later.");
        }
        
        // Verify password
        if (!PasswordHasher.matchesBcrypt(request.getPassword(), superadmin.getPasswordHash())) {
            handleFailedLogin(superadmin);
            log.warn("Invalid password for superadmin: {}", request.getIdentifier());
            throw new NoSuchElementException("Invalid credentials");
        }
        
        // Check if account is enabled
        if (!superadmin.isEnabled()) {
            log.warn("Login denied: Account disabled for {}", superadmin.getUsername());
            throw new IllegalStateException("Account is disabled. Please contact system administrator.");
        }
        
        if (superadmin.isLocked()) {
            log.warn("Login denied: Account locked for {}", superadmin.getUsername());
            throw new IllegalStateException("Account is locked. Please contact system administrator.");
        }
        
        // Check if first login is required
        if (!superadmin.isFirstLoginCompleted()) {
            log.info("First login detected for superadmin: {}", superadmin.getUsername());
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
        
        // Check if password has expired
        if (superadmin.getPasswordExpiresAt() != null && 
            LocalDateTime.now().isAfter(superadmin.getPasswordExpiresAt())) {
            log.info("Password expired for superadmin: {}", superadmin.getUsername());
            String expiredPasswordToken = generateExpiredPasswordToken(superadmin);
            
            return BaseResponse.success("Password expired - change required",
                SuperadminAuthResponse.builder()
                    .accessToken(expiredPasswordToken)
                    .tokenType("Bearer")
                    .expiresInSeconds(superadminTokenTtl.getSeconds())
                    .role("EJADA_OFFICER")
                    .permissions(List.of("CHANGE_PASSWORD"))
                    .passwordExpired(true)
                    .build());
        }
        
        // Successful login
        superadmin.setFailedLoginAttempts(0);
        superadmin.setLockedUntil(null);
        superadmin.setLastLoginAt(LocalDateTime.now());
        superadminRepository.save(superadmin);
        
        // Generate full access JWT token
        String token = generateSuperadminToken(superadmin);
        
        log.info("Superadmin {} logged in successfully", superadmin.getUsername());
        
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
    }
    
    @Override
    public BaseResponse<Void> completeFirstLogin(FirstLoginRequest request) {
        log.info("Processing first login completion");
        
        // Get the current authenticated superadmin from the JWT token
        Long superadminId = getCurrentSuperadminId();
        
        Superadmin superadmin = superadminRepository.findById(superadminId)
            .orElseThrow(() -> new NoSuchElementException("Superadmin not found"));
        
        // Verify this is actually a first login scenario
        if (superadmin.isFirstLoginCompleted()) {
            log.warn("First login already completed for: {}", superadmin.getUsername());
            throw new IllegalStateException("First login has already been completed");
        }
        
        // Verify current password
        if (!PasswordHasher.matchesBcrypt(request.getCurrentPassword(), superadmin.getPasswordHash())) {
            log.warn("Invalid current password during first login for: {}", superadmin.getUsername());
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Validate new password is different
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }
        
        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        
        // Validate password complexity
        validatePasswordComplexity(request.getNewPassword());
        
        ensurePasswordNotReused(superadmin.getId(), request.getNewPassword());

        // Update superadmin record
        superadmin.setPasswordHash(PasswordHasher.bcrypt(request.getNewPassword()));
        superadmin.setFirstLoginCompleted(true);
        superadmin.setPasswordChangedAt(LocalDateTime.now());
        superadmin.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays));
        
        // Update profile if provided
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
        recordPasswordHistory(superadmin);

        // Log the action
        logSuperadminAction(
            "FIRST_LOGIN_COMPLETED",
            superadmin.getId(),
            superadmin.getUsername(),
            "First login completed and password changed");
        
        log.info("First login completed successfully for: {}", superadmin.getUsername());
        
        return BaseResponse.success(
            "First login completed successfully. Please login again with your new password.", 
            null);
    }
    
    @Override
    public BaseResponse<Void> changePassword(ChangePasswordRequest request) {
        log.info("Processing password change request");
        
        Long superadminId = getCurrentSuperadminId();
        
        Superadmin superadmin = superadminRepository.findById(superadminId)
            .orElseThrow(() -> new NoSuchElementException("Superadmin not found"));
        
        // Verify current password
        if (!PasswordHasher.matchesBcrypt(request.getCurrentPassword(), superadmin.getPasswordHash())) {
            log.warn("Invalid current password for password change: {}", superadmin.getUsername());
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Validate new password
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        
        validatePasswordComplexity(request.getNewPassword());
        ensurePasswordNotReused(superadmin.getId(), request.getNewPassword());

        // Update password
        superadmin.setPasswordHash(PasswordHasher.bcrypt(request.getNewPassword()));
        superadmin.setPasswordChangedAt(LocalDateTime.now());
        superadmin.setPasswordExpiresAt(LocalDateTime.now().plusDays(passwordExpiryDays));

        superadminRepository.save(superadmin);
        recordPasswordHistory(superadmin);

        // Log the action
        logSuperadminAction(
            "PASSWORD_CHANGED",
            superadmin.getId(),
            superadmin.getUsername(),
            "Password changed successfully");
        
        log.info("Password changed successfully for: {}", superadmin.getUsername());
        
        return BaseResponse.success("Password changed successfully", null);
    }
    
    // Helper methods
    
    private void validateSuperadminAccess() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user found");
        }

        boolean isSuperadmin = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_EJADA_OFFICER"));

        if (!isSuperadmin) {
            throw new AccessDeniedException("Access denied. Only superadmins can perform this action");
        }
    }

    private Long getCurrentSuperadminId() {
        Long id = getCurrentSuperadminIdOrNull();
        if (id != null) {
            return id;
        }
        throw new AuthenticationCredentialsNotFoundException("No authenticated superadmin found");
    }

    private Long getCurrentSuperadminIdOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Object uid = jwt.getClaim("uid");
            if (uid != null) {
                try {
                    return Long.valueOf(uid.toString());
                } catch (NumberFormatException ex) {
                    log.warn("Invalid uid claim: {}", uid);
                }
            }
        }
        return null;
    }
    
    private String getCurrentSuperadminUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return "system";
    }
    
    private void validatePasswordComplexity(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.length() < 12) {
            errors.add("Password must be at least 12 characters long");
        }
        
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        
        if (!Pattern.compile("\\d").matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }
        
        if (!Pattern.compile("[@$!%*?&]").matcher(password).find()) {
            errors.add("Password must contain at least one special character (@$!%*?&)");
        }
        
        // Check for common passwords
        Set<String> commonPasswords = Set.of(
            "Password123!", "Admin123!", "Welcome123!", 
            "Password@123", "Admin@123", "Welcome@123"
        );
        
        if (commonPasswords.stream().anyMatch(common -> common.equalsIgnoreCase(password))) {
            errors.add("This password is too common. Please choose a more unique password");
        }
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Password validation failed: " + String.join(", ", errors));
        }
    }
    
    private void handleFailedLogin(Superadmin superadmin) {
        superadmin.setFailedLoginAttempts(superadmin.getFailedLoginAttempts() + 1);
        
        if (superadmin.getFailedLoginAttempts() >= maxFailedAttempts) {
            superadmin.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            log.warn("Account temporarily locked due to {} failed attempts: {}", 
                maxFailedAttempts, superadmin.getUsername());
        }
        
        superadminRepository.save(superadmin);
        
        logSuperadminAction(
            "LOGIN_FAILED",
            superadmin.getId(),
            superadmin.getUsername(),
            "Failed login attempt #" + superadmin.getFailedLoginAttempts());
    }
    
    private String generateFirstLoginToken(Superadmin superadmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", superadmin.getUsername());
        claims.put("uid", superadmin.getId());
        claims.put("email", superadmin.getEmail());
        claims.put("roles", List.of("EJADA_OFFICER"));
        claims.put("isSuperadmin", true);
        claims.put("requiresPasswordChange", true);
        claims.put("accountState", "FIRST_LOGIN");

        return jwtTokenService.createToken(
            superadmin.getUsername(),
            null,
            List.of("EJADA_OFFICER"),
            claims,
            superadminTokenTtl);
    }

    private String generateExpiredPasswordToken(Superadmin superadmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", superadmin.getUsername());
        claims.put("uid", superadmin.getId());
        claims.put("email", superadmin.getEmail());
        claims.put("roles", List.of("EJADA_OFFICER"));
        claims.put("isSuperadmin", true);
        claims.put("passwordExpired", true);
        claims.put("accountState", "PASSWORD_EXPIRED");

        return jwtTokenService.createToken(
            superadmin.getUsername(),
            null,
            List.of("EJADA_OFFICER"),
            claims,
            superadminTokenTtl);
    }
    
    private String generateSuperadminToken(Superadmin superadmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", superadmin.getUsername());
        claims.put("uid", superadmin.getId());
        claims.put("email", superadmin.getEmail());
        claims.put("roles", List.of("EJADA_OFFICER"));
        claims.put("isSuperadmin", true);
        claims.put("accountState", "ACTIVE");
        
        String fullName = "";
        if (superadmin.getFirstName() != null && superadmin.getLastName() != null) {
            fullName = superadmin.getFirstName() + " " + superadmin.getLastName();
        }
        claims.put("fullName", fullName);
        
        return jwtTokenService.createToken(
            superadmin.getUsername(),
            null,
            List.of("EJADA_OFFICER"),
            claims,
            superadminTokenTtl);
    }
    
    private void logSuperadminAction(String action, Long superadminId, String username, String details) {
        try {
            superadminAuditService.logSuperadminAction(action, superadminId, details);
        } catch (DataAccessException ex) {
            log.warn("Failed to persist superadmin audit entry for action {}", action, ex);
        }
        log.info("SUPERADMIN_AUDIT: Action={}, User={}, Details={}", action, username, details);
    }

    private void recordPasswordHistory(Superadmin superadmin) {
        if (superadmin.getId() == null) {
            return;
        }
        SuperadminPasswordHistory history = SuperadminPasswordHistory.builder()
            .superadminId(superadmin.getId())
            .passwordHash(superadmin.getPasswordHash())
            .build();
        try {
            passwordHistoryRepository.save(history);
        } catch (DataAccessException ex) {
            log.warn("Failed to record password history for superadmin {}", superadmin.getId(), ex);
        }
    }

    private void ensurePasswordNotReused(Long superadminId, String candidatePassword) {
        if (superadminId == null) {
            return;
        }
        try {
            List<SuperadminPasswordHistory> recentPasswords =
                passwordHistoryRepository.findTop5BySuperadminIdOrderByCreatedAtDesc(superadminId);
            for (SuperadminPasswordHistory entry : recentPasswords) {
                if (PasswordHasher.matchesBcrypt(candidatePassword, entry.getPasswordHash())) {
                    throw new IllegalArgumentException("New password cannot match any of your last 5 passwords");
                }
            }
        } catch (DataAccessException ex) {
            log.error("Unable to verify password history for superadmin {}", superadminId, ex);
            throw new PasswordHistoryUnavailableException(
                "Unable to verify password history at the moment. Please try again later or contact support.", ex);
        }
    }
    
    private void sendWelcomeEmail(Superadmin superadmin) {
        // Email service implementation
        log.info("Welcome email sent to: {}", superadmin.getEmail());
        // In production, integrate with email service:
        // emailService.sendSuperadminWelcome(superadmin.getEmail(), superadmin.getUsername());
    }
}