package com.ejada.admin.mapper;

import com.ejada.admin.domain.Superadmin;
import com.ejada.admin.dto.CreateSuperadminRequest;
import com.ejada.admin.dto.SuperadminDto;
import com.ejada.admin.dto.UpdateSuperadminRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class SuperadminMapper {

    public Superadmin toEntity(CreateSuperadminRequest request) {
        if (request == null) {
            return null;
        }

        return Superadmin.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .enabled(true)
            .locked(false)
            .role("EJADA_OFFICER")
            .firstLoginCompleted(false)
            .failedLoginAttempts(0)
            .twoFactorEnabled(false)
            .build();
    }

    public SuperadminDto toDto(Superadmin entity) {
        if (entity == null) {
            return null;
        }

        return SuperadminDto.builder()
            .id(entity.getId())
            .username(entity.getUsername())
            .email(entity.getEmail())
            .role(entity.getRole())
            .enabled(entity.isEnabled())
            .locked(entity.isLocked())
            .lastLoginAt(entity.getLastLoginAt())
            .createdAt(toLocalDateTime(entity.getCreatedAt()))
            .updatedAt(toLocalDateTime(entity.getUpdatedAt()))
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .phoneNumber(entity.getPhoneNumber())
            .firstLoginCompleted(entity.isFirstLoginCompleted())
            .passwordChangedAt(entity.getPasswordChangedAt())
            .passwordExpiresAt(entity.getPasswordExpiresAt())
            .twoFactorEnabled(entity.isTwoFactorEnabled())
            .build();
    }

    public void updateEntity(Superadmin entity, UpdateSuperadminRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getEmail() != null) {
            entity.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) {
            entity.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            entity.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            entity.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (request.getLocked() != null) {
            entity.setLocked(request.getLocked());
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
