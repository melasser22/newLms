package com.ejada.common.dto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * Utility for translating marketplace {@link ServiceResult} status codes into HTTP status codes.
 */
public final class ServiceResultHttpStatusMapper {

  private static final Pattern MARKETPLACE_ERROR_PATTERN = Pattern.compile("^E(\\d{3}).*");

  private ServiceResultHttpStatusMapper() {
  }

  /**
   * Resolves the HTTP status that should be returned for the provided marketplace status code.
   *
   * @param statusCode marketplace status code
   * @return resolved HTTP status (defaults to {@link HttpStatus#BAD_REQUEST} for non-internal errors)
   */
  public static HttpStatus resolve(String statusCode) {
    if (!StringUtils.hasText(statusCode)) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    if (statusCode.startsWith("EINT")) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    Matcher matcher = MARKETPLACE_ERROR_PATTERN.matcher(statusCode);
    if (matcher.matches()) {
      int code = Integer.parseInt(matcher.group(1));
      HttpStatus resolved = HttpStatus.resolve(code);
      if (resolved != null && resolved.is4xxClientError()) {
        return resolved;
      }
    }

    return HttpStatus.BAD_REQUEST;
  }
}

