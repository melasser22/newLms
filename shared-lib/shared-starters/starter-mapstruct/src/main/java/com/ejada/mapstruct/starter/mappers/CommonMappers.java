
package com.ejada.mapstruct.starter.mappers;

import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public final class CommonMappers {
  private CommonMappers() {}

  @Named("uuidToString")
  public static String uuidToString(UUID id) {
    return id == null ? null : id.toString();
  }

  @Named("stringToUuid")
  public static UUID stringToUuid(String id) {
    return (id == null || id.isBlank()) ? null : UUID.fromString(id);
  }

  @Named("trim")
  public static String trim(String s) {
    return s == null ? null : s.trim();
  }

  @Named("odtToInstant")
  public static Instant odtToInstant(OffsetDateTime odt) {
    return odt == null ? null : odt.toInstant();
  }

  @Named("instantToOdtUtc")
  public static OffsetDateTime instantToOdtUtc(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }

  @Named("localDateTimeToInstantUtc")
  public static Instant localDateTimeToInstantUtc(LocalDateTime ldt) {
    return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
  }
}
