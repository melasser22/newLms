package com.ejada.subscription.mapper;

import com.ejada.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.model.SubscriptionUpdateEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubscriptionUpdateEventMapper {

    @Mapping(target = "subscriptionUpdateEventId", ignore = true)
    @Mapping(target = "rqUid", expression = "java(rqUid)")
    @Mapping(target = "extSubscriptionId", source = "rq.subscriptionId")
    @Mapping(target = "extCustomerId",    source = "rq.customerId")
    @Mapping(target = "updateType",       source = "rq.subscriptionUpdateType")
    @Mapping(target = "receivedAt", ignore = true)
    @Mapping(target = "processed", constant = "false")
    @Mapping(target = "processedAt", ignore = true)
    SubscriptionUpdateEvent toEvent(ReceiveSubscriptionUpdateRq rq, UUID rqUid);
}
