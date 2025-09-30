package com.ejada.subscription.mapper;

import com.ejada.common.marketplace.subscription.dto.SubscriptionAdditionalServiceDto;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionAdditionalService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionAdditionalServiceMapper {

    @Mapping(target = "subscriptionAdditionalServiceId", ignore = true)
    @Mapping(target = "subscription", expression = "java(subscription)")
    @Mapping(target = "productAdditionalServiceId", source = "dto.productAdditionalServiceId")
    @Mapping(target = "serviceCd", source = "dto.serviceCd")
    @Mapping(target = "serviceNameEn", source = "dto.serviceNameEn")
    @Mapping(target = "serviceNameAr", source = "dto.serviceNameAr")
    @Mapping(target = "serviceDescEn", source = "dto.serviceDescEn")
    @Mapping(target = "serviceDescAr", source = "dto.serviceDescAr")
    @Mapping(target = "servicePrice", source = "dto.servicePrice")
    @Mapping(target = "totalAmount", source = "dto.totalAmount")
    @Mapping(target = "currency", source = "dto.currency")
    @Mapping(target = "isCountable", source = "dto.isCountable", defaultValue = "false")
    @Mapping(target = "requestedCount", source = "dto.requestedCount")
    @Mapping(target = "paymentTypeCd", source = "dto.paymentTypeCd")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SubscriptionAdditionalService toEntity(SubscriptionAdditionalServiceDto dto, @Context Subscription subscription);

    List<SubscriptionAdditionalService> toEntities(List<SubscriptionAdditionalServiceDto> dtos, @Context Subscription subscription);
}
