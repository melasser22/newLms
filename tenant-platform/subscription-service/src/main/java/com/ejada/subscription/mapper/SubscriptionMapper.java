package com.ejada.subscription.mapper;

import org.mapstruct.*;

import com.ejada.subscription.dto.SubscriptionInfoDto;
import com.ejada.subscription.model.Subscription;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionMapper {

    @Mapping(target = "subscriptionId", ignore = true)
    @Mapping(target = "extSubscriptionId", source = "subscriptionId")
    @Mapping(target = "extCustomerId",    source = "customerId")
    @Mapping(target = "extProductId",     source = "productId")
    @Mapping(target = "extTierId",        source = "tierId")
    @Mapping(target = "tierNmEn",         source = "tierNameEn")
    @Mapping(target = "tierNmAr",         source = "tierNameAr")
    @Mapping(target = "startDt",          source = "startDt")
    @Mapping(target = "endDt",            source = "endDt")
    @Mapping(target = "subscriptionAmount", source = "subscriptionAmount")
    @Mapping(target = "totalBilledAmount",  source = "totalBilledAmount")
    @Mapping(target = "totalPaidAmount",    source = "totalPaidAmount")
    @Mapping(target = "subscriptionSttsCd", source = "subscriptionSttsCd")
    @Mapping(target = "createChannel",      source = "createChannel")
    @Mapping(target = "unlimitedUsersFlag", source = "unlimitedUsersFlag", defaultValue = "false")
    @Mapping(target = "usersLimit",         source = "usersLimit")
    @Mapping(target = "usersLimitResetType",source = "usersLimitResetType")
    @Mapping(target = "unlimitedTransFlag", source = "unlimitedTransFlag", defaultValue = "false")
    @Mapping(target = "transactionsLimit",  source = "transactionsLimit")
    @Mapping(target = "transLimitResetType",source = "transLimitResetType")
    @Mapping(target = "balanceLimit",       source = "balanceLimit")
    @Mapping(target = "balanceLimitResetType", source = "balanceLimitResetType")
    @Mapping(target = "environmentSizeCd",  source = "environmentSizeCd")
    @Mapping(target = "isAutoProvEnabled",  source = "isAutoProvEnabled", defaultValue = "false")
    @Mapping(target = "prevSubscriptionId", source = "prevSubscriptionId")
    @Mapping(target = "prevSubscriptionUpdateAction", source = "prevSubscriptionUpdateAction")
    @Mapping(target = "meta", ignore = true)
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Subscription toEntity(SubscriptionInfoDto dto);

    @BeanMapping(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        ignoreByDefault = true
    )
    @Mapping(target = "extSubscriptionId", source = "subscriptionId")
    @Mapping(target = "extCustomerId",    source = "customerId")
    @Mapping(target = "extProductId",     source = "productId")
    @Mapping(target = "extTierId",        source = "tierId")
    @Mapping(target = "tierNmEn",         source = "tierNameEn")
    @Mapping(target = "tierNmAr",         source = "tierNameAr")
    @Mapping(target = "startDt",          source = "startDt")
    @Mapping(target = "endDt",            source = "endDt")
    @Mapping(target = "subscriptionAmount", source = "subscriptionAmount")
    @Mapping(target = "totalBilledAmount",  source = "totalBilledAmount")
    @Mapping(target = "totalPaidAmount",    source = "totalPaidAmount")
    @Mapping(target = "subscriptionSttsCd", source = "subscriptionSttsCd")
    @Mapping(target = "createChannel",      source = "createChannel")
    @Mapping(target = "unlimitedUsersFlag", source = "unlimitedUsersFlag")
    @Mapping(target = "usersLimit",         source = "usersLimit")
    @Mapping(target = "usersLimitResetType",source = "usersLimitResetType")
    @Mapping(target = "unlimitedTransFlag", source = "unlimitedTransFlag")
    @Mapping(target = "transactionsLimit",  source = "transactionsLimit")
    @Mapping(target = "transLimitResetType",source = "transLimitResetType")
    @Mapping(target = "balanceLimit",       source = "balanceLimit")
    @Mapping(target = "balanceLimitResetType", source = "balanceLimitResetType")
    @Mapping(target = "environmentSizeCd",  source = "environmentSizeCd")
    @Mapping(target = "isAutoProvEnabled",  source = "isAutoProvEnabled")
    @Mapping(target = "prevSubscriptionId", source = "prevSubscriptionId")
    @Mapping(target = "prevSubscriptionUpdateAction", source = "prevSubscriptionUpdateAction")
    void update(@MappingTarget Subscription entity, SubscriptionInfoDto dto);
}
