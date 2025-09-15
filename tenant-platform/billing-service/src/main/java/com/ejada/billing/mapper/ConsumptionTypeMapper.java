package com.ejada.billing.mapper;

import com.ejada.billing.dto.ConsumptionType;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ConsumptionTypeMapper {

    default String toCode(ConsumptionType t) {
        return t == null ? null : t.name(); // TRANSACTION|USER|BALANCE
    }

    default ConsumptionType toEnum(String code) {
        return code == null ? null : ConsumptionType.valueOf(code);
    }
}
