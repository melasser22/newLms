package com.ejada.subscription.mapper;

import com.ejada.common.marketplace.subscription.dto.EnvironmentIdentifierDto;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionEnvironmentIdentifier;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionEnvironmentIdentifierMapper {

    @Mapping(target = "subscriptionEnvId", ignore = true)
    @Mapping(target = "subscription", expression = "java(subscription)")
    @Mapping(target = "identifierCd",    source = "dto.identifierCd")
    @Mapping(target = "identifierValue", source = "dto.identifierValue")
    @Mapping(target = "createdAt", ignore = true)
    SubscriptionEnvironmentIdentifier toEntity(EnvironmentIdentifierDto dto, @Context Subscription subscription);

    // entity -> dto (no 'subscription' on DTO)
    EnvironmentIdentifierDto toDto(SubscriptionEnvironmentIdentifier entity);

    List<EnvironmentIdentifierDto> toDtoList(List<SubscriptionEnvironmentIdentifier> entities);

    List<SubscriptionEnvironmentIdentifier> toEntities(List<EnvironmentIdentifierDto> dtos, @Context Subscription subscription);
}
