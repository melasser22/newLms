package com.ejada.admin.mapper;

import com.ejada.sec.domain.Superadmin;
import com.ejada.sec.dto.admin.CreateSuperadminRequest;
import com.ejada.sec.dto.admin.SuperadminDto;
import com.ejada.sec.dto.admin.UpdateSuperadminRequest;
import org.mapstruct.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        builder = @Builder(disableBuilder = false))
public interface SuperadminMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "locked", constant = "false")
    @Mapping(target = "role", constant = "EJADA_OFFICER")
    @Mapping(target = "firstLoginCompleted", constant = "false")
    @Mapping(target = "failedLoginAttempts", constant = "0")
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "passwordExpiresAt", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "twoFactorEnabled", constant = "false")
    @Mapping(target = "twoFactorSecret", ignore = true)
    Superadmin toEntity(CreateSuperadminRequest request);

    SuperadminDto toDto(Superadmin entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateEntity(@MappingTarget Superadmin entity, UpdateSuperadminRequest request);

    // 🔥 Custom mappers for Instant ↔ LocalDateTime
    default LocalDateTime map(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    default Instant map(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
