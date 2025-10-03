package com.ejada.subscription.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.ejada.common.marketplace.subscription.dto.SubscriptionInfoDto;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionApprovalStatus;

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
    @Mapping(target = "usersLimitResetType", source = "usersLimitResetType")
    @Mapping(target = "unlimitedTransFlag", source = "unlimitedTransFlag", defaultValue = "false")
    @Mapping(target = "transactionsLimit",  source = "transactionsLimit")
    @Mapping(target = "transLimitResetType", source = "transLimitResetType")
    @Mapping(target = "balanceLimit",       source = "balanceLimit")
    @Mapping(target = "balanceLimitResetType", source = "balanceLimitResetType")
    @Mapping(target = "environmentSizeCd",  source = "environmentSizeCd")
    @Mapping(target = "isAutoProvEnabled",  source = "isAutoProvEnabled", defaultValue = "false")
    @Mapping(target = "prevSubscriptionId", source = "prevSubscriptionId")
    @Mapping(target = "prevSubscriptionUpdateAction", source = "prevSubscriptionUpdateAction")
    @Mapping(target = "approvalStatus", expression = "java(defaultApprovalStatus())")
    @Mapping(target = "approvalRequired", expression = "java(defaultApprovalRequired())")
    @Mapping(target = "submittedAt", expression = "java(nullOffsetDateTime())")
    @Mapping(target = "submittedBy", expression = "java(nullString())")
    @Mapping(target = "approvedAt", expression = "java(nullOffsetDateTime())")
    @Mapping(target = "approvedBy", expression = "java(nullString())")
    @Mapping(target = "rejectedAt", expression = "java(nullOffsetDateTime())")
    @Mapping(target = "rejectedBy", expression = "java(nullString())")
    @Mapping(target = "tenantId", expression = "java(nullLong())")
    @Mapping(target = "adminUserId", expression = "java(nullLong())")
    @Mapping(target = "tenantCode", expression = "java(nullString())")
    @Mapping(target = "securityTenantId", expression = "java(nullUuid())")
    @Mapping(target = "meta", expression = "java(nullString())")
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "createdAt", expression = "java(currentTimestamp())")
    @Mapping(target = "createdBy", expression = "java(nullString())")
    @Mapping(target = "updatedAt", expression = "java(nullOffsetDateTime())")
    @Mapping(target = "updatedBy", expression = "java(nullString())")
    Subscription toEntity(SubscriptionInfoDto dto);

    @BeanMapping(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        ignoreByDefault = true
    )
    @InheritConfiguration(name = "toEntity")
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
    @Mapping(target = "usersLimitResetType", source = "usersLimitResetType")
    @Mapping(target = "unlimitedTransFlag", source = "unlimitedTransFlag")
    @Mapping(target = "transactionsLimit",  source = "transactionsLimit")
    @Mapping(target = "transLimitResetType", source = "transLimitResetType")
    @Mapping(target = "balanceLimit",       source = "balanceLimit")
    @Mapping(target = "balanceLimitResetType", source = "balanceLimitResetType")
    @Mapping(target = "environmentSizeCd",  source = "environmentSizeCd")
    @Mapping(target = "isAutoProvEnabled",  source = "isAutoProvEnabled")
    @Mapping(target = "prevSubscriptionId", source = "prevSubscriptionId")
    @Mapping(target = "prevSubscriptionUpdateAction", source = "prevSubscriptionUpdateAction")
    @Mapping(target = "approvalStatus", expression = "java(entity.getApprovalStatus())")
    @Mapping(target = "approvalRequired", expression = "java(entity.getApprovalRequired())")
    @Mapping(target = "submittedAt", expression = "java(entity.getSubmittedAt())")
    @Mapping(target = "submittedBy", expression = "java(entity.getSubmittedBy())")
    @Mapping(target = "approvedAt", expression = "java(entity.getApprovedAt())")
    @Mapping(target = "approvedBy", expression = "java(entity.getApprovedBy())")
    @Mapping(target = "rejectedAt", expression = "java(entity.getRejectedAt())")
    @Mapping(target = "rejectedBy", expression = "java(entity.getRejectedBy())")
    @Mapping(target = "tenantId", expression = "java(entity.getTenantId())")
    @Mapping(target = "adminUserId", expression = "java(entity.getAdminUserId())")
    @Mapping(target = "tenantCode", expression = "java(entity.getTenantCode())")
    @Mapping(target = "securityTenantId", expression = "java(entity.getSecurityTenantId())")
    @Mapping(target = "meta", expression = "java(entity.getMeta())")
    @Mapping(target = "isDeleted", expression = "java(entity.getIsDeleted())")
    @Mapping(target = "createdAt", expression = "java(entity.getCreatedAt())")
    @Mapping(target = "createdBy", expression = "java(entity.getCreatedBy())")
    @Mapping(target = "updatedAt", expression = "java(entity.getUpdatedAt())")
    @Mapping(target = "updatedBy", expression = "java(entity.getUpdatedBy())")
    void update(@MappingTarget Subscription entity, SubscriptionInfoDto dto);

    default String defaultApprovalStatus() {
        return SubscriptionApprovalStatus.PENDING_APPROVAL.name();
    }

    default Boolean defaultApprovalRequired() {
        return Boolean.TRUE;
    }

    default OffsetDateTime currentTimestamp() {
        return OffsetDateTime.now();
    }

    default OffsetDateTime nullOffsetDateTime() {
        return null;
    }

    default String nullString() {
        return null;
    }

    default Long nullLong() {
        return null;
    }

    default UUID nullUuid() {
        return null;
    }
}
