package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TierMapper {

    @Mapping(target = "tierId", ignore = true)
    @Mapping(target = "tierCd", source = "tierCd")
    @Mapping(target = "tierEnNm", source = "tierEnNm")
    @Mapping(target = "tierArNm", source = "tierArNm")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "rankOrder", source = "rankOrder", defaultValue = "0")
    @Mapping(target = "isActive", source = "isActive", defaultValue = "true")
    @Mapping(target = "isDeleted", constant = "false")
    Tier toEntity(TierCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Tier entity, TierUpdateReq req);

    TierRes toRes(Tier entity);
}
