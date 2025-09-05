package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
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
    Feature toEntity(FeatureCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Feature entity, FeatureUpdateReq req);

    FeatureRes toRes(Feature entity);
}
