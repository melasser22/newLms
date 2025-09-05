package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        imports = {Tier.class, Addon.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TierAddonMapper {

    @Mapping(target = "tierAddonId", ignore = true)
    @Mapping(target = "tier", expression = "java(Tier.ref(req.tierId()))")
    @Mapping(target = "addon", expression = "java(Addon.ref(req.addonId()))")
    @Mapping(target = "included", source = "included", defaultValue = "false")
    @Mapping(target = "sortOrder", source = "sortOrder", defaultValue = "0")
    @Mapping(target = "basePrice", source = "basePrice")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "isDeleted", constant = "false")
    TierAddon toEntity(TierAddonCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget TierAddon entity, TierAddonUpdateReq req);

    @Mapping(target = "tierId", source = "tier.tierId")
    @Mapping(target = "addonId", source = "addon.addonId")
    TierAddonRes toRes(TierAddon entity);
}
