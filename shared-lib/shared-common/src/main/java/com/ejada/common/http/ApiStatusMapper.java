package com.ejada.common.http;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.dto.BaseResponse;
import com.ejada.common.enums.StatusEnums.ApiStatus;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * Utility for mapping {@link BaseResponse} status codes to HTTP {@link HttpStatus} values.
 */
public final class ApiStatusMapper {

    private static final Pattern TRAILING_HTTP_STATUS = Pattern.compile("-(\\d{3})$");

    private static final Map<ApiStatus, HttpStatus> DEFAULT_STATUS = Map.of(
            ApiStatus.SUCCESS, HttpStatus.OK,
            ApiStatus.ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
            ApiStatus.WARNING, HttpStatus.BAD_REQUEST);

    private static final Map<String, HttpStatus> EXACT_CODE_STATUS;
    private static final Map<String, HttpStatus> KEYWORD_STATUS;

    static {
        Map<String, HttpStatus> exact = new LinkedHashMap<>();
        exact.put(ErrorCodes.AUTH_INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
        exact.put(ErrorCodes.AUTH_EXPIRED_TOKEN, HttpStatus.UNAUTHORIZED);
        exact.put(ErrorCodes.AUTH_UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        exact.put(ErrorCodes.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN);
        exact.put(ErrorCodes.AUTH_MISSING_CREDENTIALS, HttpStatus.BAD_REQUEST);
        exact.put(ErrorCodes.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        exact.put(ErrorCodes.AUTH_HISTORY_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        exact.put(ErrorCodes.AUTH_DATA_ACCESS, HttpStatus.SERVICE_UNAVAILABLE);
        exact.put(ErrorCodes.TENANT_NOT_FOUND, HttpStatus.NOT_FOUND);
        exact.put(ErrorCodes.TENANT_DISABLED, HttpStatus.FORBIDDEN);
        exact.put(ErrorCodes.TENANT_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        exact.put(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);
        exact.put(ErrorCodes.DATA_NOT_FOUND, HttpStatus.NOT_FOUND);
        exact.put(ErrorCodes.DATA_DUPLICATE, HttpStatus.CONFLICT);
        exact.put(ErrorCodes.DATA_INTEGRITY, HttpStatus.CONFLICT);
        exact.put(ErrorCodes.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        exact.put(ErrorCodes.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        exact.put(ErrorCodes.TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
        exact.put(ErrorCodes.DEPENDENCY_FAILURE, HttpStatus.BAD_GATEWAY);
        exact.put(ErrorCodes.API_BAD_REQUEST, HttpStatus.BAD_REQUEST);
        exact.put(ErrorCodes.API_UNSUPPORTED_MEDIA, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        exact.put(ErrorCodes.API_RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        exact.put(ErrorCodes.API_UNPROCESSABLE_ENTITY, HttpStatus.UNPROCESSABLE_ENTITY);
        exact.put(ErrorCodes.PAYMENT_FAILED, HttpStatus.PAYMENT_REQUIRED);
        exact.put(ErrorCodes.PAYMENT_DECLINED, HttpStatus.PAYMENT_REQUIRED);
        exact.put(ErrorCodes.PAYMENT_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
        exact.put(ErrorCodes.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY);
        exact.put(ErrorCodes.NOT_FOUND, HttpStatus.NOT_FOUND);
        exact.put("ERR-AUTH-INVALID", HttpStatus.UNAUTHORIZED);
        exact.put("ERR-AUTH-LOCKED", HttpStatus.LOCKED);
        EXACT_CODE_STATUS = Collections.unmodifiableMap(exact);

        Map<String, HttpStatus> keyword = new LinkedHashMap<>();
        keyword.put("NOT_FOUND", HttpStatus.NOT_FOUND);
        keyword.put("ACCESS_DENIED", HttpStatus.FORBIDDEN);
        keyword.put("FORBIDDEN", HttpStatus.FORBIDDEN);
        keyword.put("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        keyword.put("INVALID", HttpStatus.BAD_REQUEST);
        keyword.put("REQUIRED", HttpStatus.BAD_REQUEST);
        keyword.put("DUP", HttpStatus.CONFLICT);
        keyword.put("CONFLICT", HttpStatus.CONFLICT);
        keyword.put("LOCKED", HttpStatus.LOCKED);
        keyword.put("TIMEOUT", HttpStatus.GATEWAY_TIMEOUT);
        keyword.put("UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        keyword.put("RATE_LIMIT", HttpStatus.TOO_MANY_REQUESTS);
        keyword.put("TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS);
        keyword.put("UNPROCESSABLE", HttpStatus.UNPROCESSABLE_ENTITY);
        keyword.put("UNSUPPORTED_MEDIA", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        keyword.put("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        KEYWORD_STATUS = Collections.unmodifiableMap(keyword);
    }

    private ApiStatusMapper() {
    }

    /**
     * Resolve an appropriate {@link HttpStatus} for the provided {@link BaseResponse}.
     *
     * @param response the API response payload
     * @return the HTTP status that best represents the response
     */
    public static HttpStatus toHttpStatus(BaseResponse<?> response) {
        if (response == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        HttpStatus resolved = resolveFromCode(response.getCode());
        if (resolved != null) {
            return resolved;
        }

        ApiStatus apiStatus = response.getStatus();
        if (apiStatus != null) {
            return DEFAULT_STATUS.getOrDefault(apiStatus, HttpStatus.OK);
        }
        return HttpStatus.OK;
    }

    /**
     * Resolve an HTTP status based solely on an error code.
     *
     * @param code          the business error code
     * @param defaultStatus status to use when the code cannot be mapped
     * @return resolved HTTP status
     */
    public static HttpStatus fromErrorCode(String code, HttpStatus defaultStatus) {
        HttpStatus resolved = resolveFromCode(code);
        if (resolved != null) {
            return resolved;
        }
        return defaultStatus != null ? defaultStatus : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static HttpStatus resolveFromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        HttpStatus fromExact = EXACT_CODE_STATUS.get(code);
        if (fromExact != null) {
            return fromExact;
        }

        String upper = code.toUpperCase(Locale.ROOT);
        fromExact = EXACT_CODE_STATUS.get(upper);
        if (fromExact != null) {
            return fromExact;
        }

        Matcher matcher = TRAILING_HTTP_STATUS.matcher(code);
        if (matcher.find()) {
            int statusCode = Integer.parseInt(matcher.group(1));
            HttpStatus httpStatus = HttpStatus.resolve(statusCode);
            if (httpStatus != null) {
                return httpStatus;
            }
        }

        for (Map.Entry<String, HttpStatus> entry : KEYWORD_STATUS.entrySet()) {
            if (upper.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
