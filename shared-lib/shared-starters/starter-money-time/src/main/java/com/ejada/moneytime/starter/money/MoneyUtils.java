
package com.ejada.moneytime.starter.money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import org.javamoney.moneta.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {
  private MoneyUtils() {}

  public static MonetaryAmount of(BigDecimal amount, String currency) {
    if (amount == null || currency == null) return null;
    CurrencyUnit cu = Monetary.getCurrency(currency);
    return Money.of(amount, cu);
  }

  public static MonetaryAmount of(double amount, String currency) {
    CurrencyUnit cu = Monetary.getCurrency(currency);
    return Money.of(amount, cu);
  }

  public static BigDecimal amount(MonetaryAmount m) {
    return m == null ? null : m.getNumber().numberValueExact(BigDecimal.class);
  }

  public static String currency(MonetaryAmount m) {
    return m == null ? null : m.getCurrency().getCurrencyCode();
  }

  public static MonetaryAmount round(MonetaryAmount m, RoundingMode mode) {
    if (m == null) return null;
    int scale = m.getCurrency().getDefaultFractionDigits();
    return Money.of(m.getNumber().numberValueExact(BigDecimal.class).setScale(scale, mode), m.getCurrency());
  }
}
