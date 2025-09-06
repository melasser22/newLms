package com.ejada.catalog.mapper;
import com.ejada.catalog.dto.*;
import com.ejada.catalog.model.Addon;
import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import org.mapstruct.*;

@Mapper(config = SharedMapstructConfig.class)
public interface AddonMapper {

    // ---------- Create ----------
    @Mapping(target = "addonId", ignore = true)
    @Mapping(target = "addonCd", source = "addonCd")
    @Mapping(target = "addonEnNm", source = "addonEnNm")
    @Mapping(target = "addonArNm", source = "addonArNm")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "isDeleted", constant = "false")
    Addon toEntity(AddonCreateReq req);

    @AfterMapping
    default void defaults(@MappingTarget Addon e, AddonCreateReq req) {
        if (e.getIsActive() == null) e.setIsActive(Boolean.TRUE);
        if (e.getIsDeleted() == null) e.setIsDeleted(Boolean.FALSE);
    }

    // ---------- Update ----------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "addonId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void update(@MappingTarget Addon entity, AddonUpdateReq req);

    // ---------- Response ----------
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AddonRes toRes(Addon entity);
}
