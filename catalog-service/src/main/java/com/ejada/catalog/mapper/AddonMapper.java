package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AddonMapper {

    @Mapping(target = "addonId", ignore = true)
    @Mapping(target = "addonCd", source = "addonCd")
    @Mapping(target = "addonEnNm", source = "addonEnNm")
    @Mapping(target = "addonArNm", source = "addonArNm")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "isActive", source = "isActive", defaultValue = "true")
    @Mapping(target = "isDeleted", constant = "false")
    Addon toEntity(AddonCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Addon entity, AddonUpdateReq req);

    AddonRes toRes(Addon entity);
}
