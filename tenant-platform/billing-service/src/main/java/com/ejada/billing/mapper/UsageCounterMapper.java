package com.ejada.billing.mapper;

import com.ejada.billing.dto.ConsumptionType;
import com.ejada.billing.dto.ProductConsumptionStts;
import com.ejada.billing.model.UsageCounter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring",
        uses = ConsumptionTypeMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UsageCounterMapper {

    /**
     * Map a single UsageCounter to ProductConsumptionStts while respecting the Swagger contract:
     * - TRANSACTION/USER -> fill currentConsumption, keep currentConsumedAmount null
     * - BALANCE          -> fill currentConsumedAmount, keep currentConsumption null
     */
    @Mapping(target = "consumptionTypCd", expression = "java(toEnum(counter.getConsumptionTypCd()))")
    @Mapping(target = "currentConsumption", expression =
            "java(fillCount(counter.getConsumptionTypCd(), counter.getCurrentConsumption()))")
    @Mapping(target = "currentConsumedAmount", expression =
            "java(fillAmount(counter.getConsumptionTypCd(), counter.getCurrentConsumedAmt()))")
    ProductConsumptionStts toDto(UsageCounter counter);

    default ConsumptionType toEnum(String code) {
        return code == null ? null : ConsumptionType.valueOf(code);
    }

    default Long fillCount(final String typ, final Long count) {
        if (typ == null) {
            return null;
        }
        return switch (typ) {
            case "TRANSACTION", "USER" -> count == null ? Long.valueOf(0L) : count;
            default -> null;
        };
    }

    default Double fillAmount(final String typ, final BigDecimal amount) {
        if (typ == null) {
            return null;
        }
        return switch (typ) {
            case "BALANCE" -> amount == null ? 0d : amount.doubleValue();
            default -> null;
        };
    }

    /** Bulk mapping helper. */
    default List<ProductConsumptionStts> toDtoList(final List<UsageCounter> counters) {
        if (counters == null) {
            return List.of();
        }
        List<ProductConsumptionStts> list = new ArrayList<>(counters.size());
        for (UsageCounter c : counters) {
            list.add(toDto(c));
        }
        return list;
    }
}
