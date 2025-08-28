
package com.shared.moneytime.starter.web;

import org.springframework.format.Formatter;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import java.text.ParseException;
import java.util.Locale;

public class MonetaryAmountFormatter implements Formatter<MonetaryAmount> {

  private final MonetaryAmountFormat format;

  public MonetaryAmountFormatter(MonetaryAmountFormat format) {
    this.format = format;
  }

  @Override
  public MonetaryAmount parse(String text, Locale locale) throws ParseException {
    return format.parse(text);
  }

  @Override
  public String print(MonetaryAmount object, Locale locale) {
    return format.format(object);
  }
}
