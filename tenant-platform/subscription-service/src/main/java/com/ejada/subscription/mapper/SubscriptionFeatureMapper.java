package com.ejada.subscription.mapper;

import com.ejada.common.marketplace.dto.SubscriptionFeatureDto;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionFeature;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionFeatureMapper {

    @Mapping(target = "subscriptionFeatureId", ignore = true)
    @Mapping(target = "subscription", expression = "java(subscription)")
    @Mapping(target = "featureCd", source = "dto.featureCd")
    @Mapping(target = "featureCount", source = "dto.featureCount")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SubscriptionFeature toEntity(SubscriptionFeatureDto dto, @Context Subscription subscription);

    List<SubscriptionFeature> toEntities(List<SubscriptionFeatureDto> dtos, @Context Subscription subscription);
}
