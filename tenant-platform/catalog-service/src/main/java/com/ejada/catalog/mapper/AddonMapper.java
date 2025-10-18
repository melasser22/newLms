package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.AddonCreateReq;
import com.ejada.catalog.dto.AddonRes;
import com.ejada.catalog.dto.AddonUpdateReq;
import com.ejada.catalog.model.Addon;
import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.lang.NonNull;

// Explicitly declare componentModel to ensure Spring Bean generation
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, config = SharedMapstructConfig.class)
public abstract class AddonMapper {

    // ---------- Create ----------
    @Mapping(target = "addonId", ignore = true)
    @Mapping(target = "addonCd", source = "addonCd")
    @Mapping(target = "addonEnNm", source = "addonEnNm")
    @Mapping(target = "addonArNm", source = "addonArNm")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "isDeleted", constant = "false")
    public abstract Addon toEntity(@NonNull AddonCreateReq req);

    @AfterMapping
    protected void defaults(@MappingTarget final Addon e, final AddonCreateReq req) {
        if (e.getIsActive() == null) {
            e.setIsActive(Boolean.TRUE);
        }
        if (e.getIsDeleted() == null) {
            e.setIsDeleted(Boolean.FALSE);
        }
    }

    // ---------- Update ----------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "addonId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    public abstract void update(@MappingTarget @NonNull Addon entity, @NonNull AddonUpdateReq req);

    // ---------- Response ----------
    public AddonRes toRes(@NonNull Addon entity) {
        final Boolean isActive = Boolean.TRUE.equals(entity.getIsActive()) ? Boolean.TRUE : Boolean.FALSE;
        final Boolean isDeleted = Boolean.TRUE.equals(entity.getIsDeleted()) ? Boolean.TRUE : Boolean.FALSE;

        return new AddonRes(
                entity.getAddonId(),
                entity.getAddonCd(),
                entity.getAddonEnNm(),
                entity.getAddonArNm(),
                entity.getDescription(),
                entity.getCategory(),
                isActive,
                isDeleted,
                null,
                null
        );
    }
}
