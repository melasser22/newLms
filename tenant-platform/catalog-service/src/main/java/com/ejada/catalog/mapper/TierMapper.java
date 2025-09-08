package com.ejada.catalog.mapper;

import com.ejada.catalog.dto.TierCreateReq;
import com.ejada.catalog.dto.TierRes;
import com.ejada.catalog.dto.TierUpdateReq;
import com.ejada.catalog.model.Tier;
import com.ejada.mapstruct.starter.config.SharedMapstructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.lang.NonNull;

@Mapper(config = SharedMapstructConfig.class)
public interface TierMapper {

    @Mapping(target = "tierId", ignore = true)
    @Mapping(target = "tierCd", source = "tierCd")
    @Mapping(target = "tierEnNm", source = "tierEnNm")
    @Mapping(target = "tierArNm", source = "tierArNm")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "rankOrder", source = "rankOrder", defaultValue = "0")
    @Mapping(target = "active", source = "isActive", defaultValue = "true")
    @Mapping(target = "deleted", constant = "false")
    Tier toEntity(@NonNull TierCreateReq req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "tierId", ignore = true)
    @Mapping(target = "tierCd", ignore = true)
    void update(@MappingTarget @NonNull Tier entity, @NonNull TierUpdateReq req);

    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isDeleted", source = "deleted")
    TierRes toRes(@NonNull Tier entity);
}
