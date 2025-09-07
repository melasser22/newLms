package com.ejada.billing.mapper;

import com.ejada.billing.dto.ProductConsumptionStts;
import com.ejada.billing.dto.ProductSubscriptionStts;
import com.ejada.billing.dto.TrackProductConsumptionRs;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ConsumptionResponseMapper {

    /** Wrap list into subscription status object. */
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "subscriptionId", source = "subscriptionId")
    @Mapping(target = "productConsumptionStts", source = "consumption")
    ProductSubscriptionStts toSubscriptionStts(Long customerId, Long subscriptionId, List<ProductConsumptionStts> consumption);

    /** Top-level response wrapper. */
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "subscriptionsStts", source = "subs")
    TrackProductConsumptionRs toResponse(Long productId, List<ProductSubscriptionStts> subs);
}
