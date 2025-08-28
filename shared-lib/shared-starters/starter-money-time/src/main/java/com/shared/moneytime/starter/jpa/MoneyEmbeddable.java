
package com.shared.moneytime.starter.jpa;

import javax.money.MonetaryAmount;
import com.shared.moneytime.starter.money.MoneyUtils;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class MoneyEmbeddable {
  private BigDecimal amount;
  private String currency;

  public MoneyEmbeddable() {}

  public MoneyEmbeddable(BigDecimal amount, String currency) {
    this.amount = amount;
    this.currency = currency;
  }

  public static MoneyEmbeddable from(MonetaryAmount m) {
    return m == null ? null : new MoneyEmbeddable(MoneyUtils.amount(m), MoneyUtils.currency(m));
  }

  public MonetaryAmount toMonetaryAmount() {
    return MoneyUtils.of(amount, currency);
  }

  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
}
