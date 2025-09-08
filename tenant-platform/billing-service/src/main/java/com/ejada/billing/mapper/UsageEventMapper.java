package com.ejada.billing.mapper;

import com.ejada.billing.model.UsageEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

/**
 * Builds the immutable audit row for Track Product Consumption calls.
 * The service supplies tokenHash and payload (JSON), we set codes/desc centrally.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UsageEventMapper {

    @Mapping(target = "usageEventId", ignore = true)
    @Mapping(target = "rqUid", expression = "java(rqUid)")
    @Mapping(target = "tokenHash", source = "tokenHash")
    @Mapping(target = "payload", source = "payloadJson")   // JSON string → jsonb
    @Mapping(target = "extProductId", source = "productId")
    @Mapping(target = "receivedAt", ignore = true)         // @PrePersist fills
    @Mapping(target = "processed", constant = "true")
    @Mapping(target = "statusCode", source = "statusCode") // e.g., I000000 | EINT000
    @Mapping(target = "statusDesc", source = "statusDesc") // e.g., Successful Operation
    @Mapping(target = "statusDtls", source = "statusDtlsJson")
    UsageEvent build(UUID rqUid,
                     String tokenHash,
                     String payloadJson,
                     Long productId,
                     String statusCode,
                     String statusDesc,
                     String statusDtlsJson);
}
