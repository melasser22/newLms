
package com.shared.moneytime.starter.time;

import java.time.*;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

public class TimeService {
  private final Clock clock;
  private final ZoneId zone;
  private final Set<DayOfWeek> businessDays;
  private final LocalTime workStart;
  private final LocalTime workEnd;

  public TimeService(Clock clock, String zoneId, String daysCsv, String workStart, String workEnd) {
    this.clock = clock;
    this.zone = ZoneId.of(zoneId);
    this.businessDays = parseDays(daysCsv);
    this.workStart = LocalTime.parse(workStart);
    this.workEnd = LocalTime.parse(workEnd);
  }

  public Instant nowInstant() { return clock.instant(); }
  public OffsetDateTime nowOffset() { return OffsetDateTime.ofInstant(clock.instant(), zone); }
  public ZonedDateTime nowZoned() { return ZonedDateTime.ofInstant(clock.instant(), zone); }
  public ZoneId getZone() { return zone; }

  public boolean isBusinessDay(LocalDate date) {
    return businessDays.contains(date.getDayOfWeek());
  }

  public boolean isWithinWorkingHours(ZonedDateTime dt) {
    if (!isBusinessDay(dt.toLocalDate())) return false;
    LocalTime t = dt.toLocalTime();
    return !t.isBefore(workStart) && t.isBefore(workEnd);
  }

  public ZonedDateTime nextBusinessStart(ZonedDateTime from) {
    ZonedDateTime d = from.withHour(workStart.getHour()).withMinute(workStart.getMinute()).withSecond(0).withNano(0);
    if (from.toLocalTime().isAfter(workStart)) {
      d = d.plusDays(1);
    }
    while (!isBusinessDay(d.toLocalDate())) {
      d = d.plusDays(1);
    }
    return d;
  }

  public ZonedDateTime addBusinessHours(ZonedDateTime start, long hours) {
    ZonedDateTime cur = start;
    long remaining = hours;
    while (remaining > 0) {
      if (!isWithinWorkingHours(cur)) {
        cur = nextBusinessStart(cur);
      } else {
        long todayLeft = Duration.between(cur.toLocalTime(), workEnd).toHours();
        long step = Math.min(todayLeft, remaining);
        cur = cur.plusHours(step);
        remaining -= step;
        if (remaining > 0) cur = nextBusinessStart(cur);
      }
    }
    return cur;
  }

  private static Set<DayOfWeek> parseDays(String csv) {
    EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
    if (csv == null || csv.isBlank()) return set;
    Stream.of(csv.split("\s*,\s*"))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(String::toUpperCase)
        .map(DayOfWeek::valueOf)
        .forEach(set::add);
    return set;
  }
}
