
package com.ejada.moneytime.starter.web;

import org.springframework.format.Formatter;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.text.ParseException;
import java.util.Locale;

public class DurationFormatter implements Formatter<Duration> {
  @Override
  public Duration parse(String text, Locale locale) throws ParseException {
    // Accept ISO-8601 (PT..), or seconds as plain number
    try {
      return Duration.parse(text);
    } catch (DateTimeParseException ex) {
      return Duration.ofSeconds(Long.parseLong(text));
    }
  }

  @Override
  public String print(Duration object, Locale locale) {
    return object.toString();
  }
}
