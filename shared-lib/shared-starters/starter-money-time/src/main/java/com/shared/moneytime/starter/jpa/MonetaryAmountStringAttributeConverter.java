
package com.shared.moneytime.starter.jpa;

import com.shared.moneytime.starter.money.MoneyUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;

@Converter(autoApply = false)
public class MonetaryAmountStringAttributeConverter implements AttributeConverter<MonetaryAmount, String> {

  @Override
  public String convertToDatabaseColumn(MonetaryAmount attribute) {
    if (attribute == null) return null;
    return MoneyUtils.currency(attribute) + " " + MoneyUtils.amount(attribute).toPlainString();
  }

  @Override
  public MonetaryAmount convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) return null;
    String[] parts = dbData.trim().split("\s+");
    if (parts.length != 2) return null;
    return MoneyUtils.of(new BigDecimal(parts[1]), parts[0]);
  }
}
