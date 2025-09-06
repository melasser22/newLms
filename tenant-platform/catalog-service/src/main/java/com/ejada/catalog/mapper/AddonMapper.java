package com.ejada.catalog.mapper;
import com.ejada.catalog.dto.*;
import com.ejada.catalog.model.Addon;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
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
    void update(@MappingTarget Addon entity, AddonUpdateReq req);

    // ---------- Response ----------
    AddonRes toRes(Addon entity);
}
