package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.TierAddonCreateReq;
import com.ejada.catalog.dto.TierAddonRes;
import com.ejada.catalog.dto.TierAddonUpdateReq;
import com.ejada.catalog.model.Addon;
import com.ejada.catalog.model.Tier;
import com.ejada.catalog.model.TierAddon;
import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.lang.NonNull;

@Mapper(config = SharedMapstructConfig.class,
        imports = {Tier.class, Addon.class})
public interface TierAddonMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"tierId", "addonId"})
    @Mapping(target = "tierAddonId", ignore = true)
    @Mapping(target = "tier", expression = "java(Tier.ref(req.tierId()))")
    @Mapping(target = "addon", expression = "java(Addon.ref(req.addonId()))")
    @Mapping(target = "included", source = "included", defaultValue = "false")
    @Mapping(target = "sortOrder", source = "sortOrder", defaultValue = "0")
    @Mapping(target = "basePrice", source = "basePrice")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "deleted", constant = "false")
    TierAddon toEntity(@NonNull TierAddonCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "tierAddonId", ignore = true)
    @Mapping(target = "tier", ignore = true)
    @Mapping(target = "addon", ignore = true)
    void update(@MappingTarget @NonNull TierAddon entity, @NonNull TierAddonUpdateReq req);

    @Mapping(target = "tierId", source = "tier.tierId")
    @Mapping(target = "addonId", source = "addon.addonId")
    @Mapping(target = "isDeleted", source = "deleted")
    TierAddonRes toRes(@NonNull TierAddon entity);
}
