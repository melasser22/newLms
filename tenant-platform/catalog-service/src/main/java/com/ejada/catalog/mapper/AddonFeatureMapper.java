package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.AddonFeatureCreateReq;
import com.ejada.catalog.dto.AddonFeatureRes;
import com.ejada.catalog.dto.AddonFeatureUpdateReq;
import com.ejada.catalog.model.Addon;
import com.ejada.catalog.model.AddonFeature;
import com.ejada.catalog.model.Enforcement;
import com.ejada.catalog.model.Feature;
import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.lang.NonNull;

@Mapper(config = SharedMapstructConfig.class,
        imports = {Addon.class, Feature.class, Enforcement.class})
public interface AddonFeatureMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"addonId", "featureId"})
    @Mapping(target = "addonFeatureId", ignore = true)
    @Mapping(target = "addon", expression = "java(Addon.ref(req.addonId()))")
    @Mapping(target = "feature", expression = "java(Feature.ref(req.featureId()))")
    @Mapping(target = "enabled", source = "enabled", defaultValue = "true")
    @Mapping(target = "enforcement", source = "enforcement", defaultExpression = "java(Enforcement.ALLOW)")
    @Mapping(target = "softLimit", source = "softLimit")
    @Mapping(target = "hardLimit", source = "hardLimit")
    @Mapping(target = "limitWindow", source = "limitWindow")
    @Mapping(target = "measureUnit", source = "measureUnit")
    @Mapping(target = "resetCron", source = "resetCron")
    @Mapping(target = "overageEnabled", source = "overageEnabled", defaultValue = "false")
    @Mapping(target = "overageUnitPrice", source = "overageUnitPrice")
    @Mapping(target = "overageCurrency", source = "overageCurrency", defaultValue = "SAR")
    @Mapping(target = "meta", source = "meta")
    @Mapping(target = "isDeleted", constant = "false")
    AddonFeature toEntity(@NonNull AddonFeatureCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "addonFeatureId", ignore = true)
    @Mapping(target = "addon", ignore = true)
    @Mapping(target = "feature", ignore = true)
    void update(@MappingTarget @NonNull AddonFeature entity, @NonNull AddonFeatureUpdateReq req);

    @Mapping(target = "addonId", source = "addon.addonId")
    @Mapping(target = "featureId", source = "feature.featureId")
    @Mapping(target = "isDeleted", source = "isDeleted")
    AddonFeatureRes toRes(@NonNull AddonFeature entity);
}
