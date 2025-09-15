package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.TierFeatureCreateReq;
import com.ejada.catalog.dto.TierFeatureRes;
import com.ejada.catalog.dto.TierFeatureUpdateReq;
import com.ejada.catalog.model.Enforcement;
import com.ejada.catalog.model.Feature;
import com.ejada.catalog.model.Tier;
import com.ejada.catalog.model.TierFeature;
import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.lang.NonNull;

@Mapper(config = SharedMapstructConfig.class,
        imports = {Tier.class, Feature.class, Enforcement.class})
public interface TierFeatureMapper {

    @BeanMapping(ignoreUnmappedSourceProperties = {"tierId", "featureId"})
    @Mapping(target = "tierFeatureId", ignore = true)
    @Mapping(target = "tier", expression = "java(Tier.ref(req.tierId()))")
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
    TierFeature toEntity(@NonNull TierFeatureCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "tierFeatureId", ignore = true)
    @Mapping(target = "tier", ignore = true)
    @Mapping(target = "feature", ignore = true)
    void update(@MappingTarget @NonNull TierFeature entity, @NonNull TierFeatureUpdateReq req);

    @Mapping(target = "tierId", source = "tier.tierId")
    @Mapping(target = "featureId", source = "feature.featureId")
    @Mapping(target = "isDeleted", source = "isDeleted")
    TierFeatureRes toRes(@NonNull TierFeature entity);
}
