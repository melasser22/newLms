package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        imports = {Addon.class, Feature.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AddonFeatureMapper {

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
    AddonFeature toEntity(AddonFeatureCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget AddonFeature entity, AddonFeatureUpdateReq req);

    @Mapping(target = "addonId", source = "addon.addonId")
    @Mapping(target = "featureId", source = "feature.featureId")
    AddonFeatureRes toRes(AddonFeature entity);
}
