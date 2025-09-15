
package com.ejada.moneytime.starter.mapstruct;

import com.ejada.moneytime.starter.money.MoneyUtils;
import org.mapstruct.Named;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;

public final class MoneyTimeMappers {
  private MoneyTimeMappers() {}

  @Named("moneyFromAmountCurrency")
  public static MonetaryAmount moneyFromAmountCurrency(BigDecimal amount, String currency) {
    return MoneyUtils.of(amount, currency);
  }

  @Named("amountFromMoney")
  public static BigDecimal amountFromMoney(MonetaryAmount m) {
    return MoneyUtils.amount(m);
  }

  @Named("currencyFromMoney")
  public static String currencyFromMoney(MonetaryAmount m) {
    return MoneyUtils.currency(m);
  }
}
