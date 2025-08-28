
package com.shared.moneytime.starter.web;

import org.springframework.format.Formatter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.text.ParseException;
import java.util.Locale;

public class InstantFormatter implements Formatter<Instant> {
  @Override
  public Instant parse(String text, Locale locale) throws ParseException {
    // Accept ISO-8601 instants or offset datetimes
    try {
      return Instant.parse(text);
    } catch (Exception e) {
      return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
    }
  }

  @Override
  public String print(Instant object, Locale locale) {
    return DateTimeFormatter.ISO_INSTANT.format(object);
  }
}
