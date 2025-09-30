package com.ejada.subscription.mapper;

import com.ejada.common.marketplace.subscription.dto.ProductPropertyDto;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionProductProperty;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionProductPropertyMapper {

    @Mapping(target = "subscriptionProductPropertyId", ignore = true)
    @Mapping(target = "subscription", expression = "java(subscription)")
    @Mapping(target = "propertyCd", source = "dto.propertyCd")
    @Mapping(target = "propertyValue", source = "dto.propertyValue")
    @Mapping(target = "createdAt", ignore = true)
    SubscriptionProductProperty toEntity(ProductPropertyDto dto, @Context Subscription subscription);

    List<SubscriptionProductProperty> toEntities(List<ProductPropertyDto> dtos, @Context Subscription subscription);
}
