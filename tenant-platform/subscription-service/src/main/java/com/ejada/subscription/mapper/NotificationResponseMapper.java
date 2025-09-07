package com.ejada.subscription.mapper;


import org.mapstruct.*;

import com.ejada.subscription.dto.EnvironmentIdentifierDto;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRs;
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
