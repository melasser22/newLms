package com.ejada.subscription.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ServiceResult", description = "Standard subscription response envelope")
public record ServiceResult<T>(
    @Schema(description = "Echoed request identifier", example = "c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd")
    String rqUID,
    @Schema(allowableValues = {"I000000", "EINT000"}, example = "I000000")
    String statusCode,
    @Schema(description = "Status description", example = "Successful Operation")
    String statusDesc,
    @Schema(description = "Wrapped response payload")
    T returnedObject,
    @Schema(description = "Debug correlation identifier when errors occur", example = "a1b2c3d4")
    String debugId,
    @Schema(description = "Optional status details", example = "['Invalid credentials']")
    List<String> statusDtls,
    @Schema(description = "Flag indicating success")
    boolean success
) {

  public static final String SUCCESS_CODE = "I000000";
  public static final String ERROR_CODE = "EINT000";
  private static final String SUCCESS_DESC = "Successful Operation";
  private static final String ERROR_DESC = "Unexpected Error";

  public ServiceResult {
    statusDtls = statusDtls == null ? Collections.emptyList() : List.copyOf(statusDtls);
  }

  @Override
  public List<String> statusDtls() {
    return statusDtls == null ? List.of() : List.copyOf(statusDtls);
  }

  public static <T> ServiceResult<T> success(final String rqUid, final T returnedObject) {
    return new ServiceResult<>(rqUid, SUCCESS_CODE, SUCCESS_DESC, returnedObject, null, List.of(), true);
  }

  public static <T> ServiceResult<T> failure(
      final String rqUid, final List<String> details) {
    return new ServiceResult<>(rqUid, ERROR_CODE, ERROR_DESC, null, null, safeDetails(details), false);
  }

  public static <T> ServiceResult<T> failure(
      final String rqUid, final String debugId, final List<String> details) {
    return new ServiceResult<>(rqUid, ERROR_CODE, ERROR_DESC, null, debugId, safeDetails(details), false);
  }

  public static <T> ServiceResult<T> failure(
      final String rqUid,
      final String statusCode,
      final String statusDesc,
      final List<String> details) {
    return new ServiceResult<>(rqUid, statusCode, statusDesc, null, null, safeDetails(details), false);
  }

  private static List<String> safeDetails(final List<String> details) {
    if (details == null || details.isEmpty()) {
      return List.of();
    }
    return Collections.unmodifiableList(new ArrayList<>(details));
  }
}
