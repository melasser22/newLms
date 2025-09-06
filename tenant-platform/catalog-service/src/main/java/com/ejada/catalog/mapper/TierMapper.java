package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.model.*;
import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import org.mapstruct.*;

@Mapper(config = SharedMapstructConfig.class)
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
    @Mapping(target = "tierId", ignore = true)
    @Mapping(target = "tierCd", ignore = true)
    void update(@MappingTarget Tier entity, TierUpdateReq req);

    TierRes toRes(Tier entity);
}
