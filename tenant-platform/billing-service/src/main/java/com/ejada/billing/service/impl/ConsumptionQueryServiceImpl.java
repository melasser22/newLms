package com.ejada.billing.service.impl;

import com.ejada.billing.dto.ProductConsumptionStts;
import com.ejada.billing.dto.ProductSubscriptionStts;
import com.ejada.billing.mapper.UsageCounterMapper;
import com.ejada.billing.model.UsageCounter;
import com.ejada.billing.repository.UsageCounterRepository;
import com.ejada.billing.service.ConsumptionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsumptionQueryServiceImpl implements ConsumptionQueryService {

    private final UsageCounterRepository repo;
    private final UsageCounterMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public ProductSubscriptionStts getSnapshot(final Long extSubscriptionId, final Long customerIdNullable) {
        List<UsageCounter> counters = repo.findByExtSubscriptionId(extSubscriptionId);
        Long custId = customerIdNullable;

        List<ProductConsumptionStts> list = mapper.toDtoList(counters);
        if (custId == null && !counters.isEmpty()) {
            custId = counters.getFirst().getExtCustomerId();
        }
        return new ProductSubscriptionStts(custId == null ? Long.valueOf(-1L) : custId, extSubscriptionId, list);
    }
}
