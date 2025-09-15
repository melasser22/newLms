package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.FeatureCreateReq;
import com.ejada.catalog.dto.FeatureRes;
import com.ejada.catalog.dto.FeatureUpdateReq;
import com.ejada.catalog.model.Feature;
import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.lang.NonNull;

@Mapper(config = SharedMapstructConfig.class)
public interface FeatureMapper {

    @Mapping(target = "featureId", ignore = true)
    @Mapping(target = "featureKey", source = "featureKey")
    @Mapping(target = "featureEnNm", source = "featureEnNm")
    @Mapping(target = "featureArNm", source = "featureArNm")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "isMetered", source = "isMetered", defaultValue = "false")
    @Mapping(target = "isActive", source = "isActive", defaultValue = "true")
    @Mapping(target = "isDeleted", constant = "false")
    Feature toEntity(@NonNull FeatureCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "featureId", ignore = true)
    @Mapping(target = "featureKey", ignore = true)
    void update(@MappingTarget @NonNull Feature entity, @NonNull FeatureUpdateReq req);

    @Mapping(target = "isMetered", source = "isMetered")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "isDeleted", source = "isDeleted")
    FeatureRes toRes(@NonNull Feature entity);
}
