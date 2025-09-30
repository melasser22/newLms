package com.ejada.subscription.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ejada.common.marketplace.dto.EnvironmentIdentifierDto;
import com.ejada.common.marketplace.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.subscription.model.SubscriptionEnvironmentIdentifier;

import java.util.List;

@Mapper(componentModel = "spring", uses = SubscriptionEnvironmentIdentifierMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationResponseMapper {

    default ReceiveSubscriptionNotificationRs toRs(boolean received,
                                                  List<SubscriptionEnvironmentIdentifier> identifiers,
                                                  SubscriptionEnvironmentIdentifierMapper idMapper) {
        List<EnvironmentIdentifierDto> ids = idMapper.toDtoList(identifiers);
        return new ReceiveSubscriptionNotificationRs(received, ids);
    }
}
