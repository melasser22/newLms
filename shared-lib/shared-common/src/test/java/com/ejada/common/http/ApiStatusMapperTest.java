package com.ejada.common.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.enums.StatusEnums.ApiStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiStatusMapperTest {

    @Test
    void successCodeWithHttpStatusSuffix_mapsToStatus() {
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .status(ApiStatus.SUCCESS)
                .code("SUCCESS-200")
                .build();

        assertEquals(HttpStatus.OK, ApiStatusMapper.toHttpStatus(response));
    }

    @Test
    void errorCodeWithKeyword_mapsToNotFound() {
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .status(ApiStatus.ERROR)
                .code("ERR_PARAM_NOT_FOUND")
                .build();

        assertEquals(HttpStatus.NOT_FOUND, ApiStatusMapper.toHttpStatus(response));
    }

    @Test
    void exactCodeMappingTakesPrecedence() {
        BaseResponse<Void> response = BaseResponse.error("ERR-AUTH-INVALID", "Invalid credentials");

        assertEquals(HttpStatus.UNAUTHORIZED, ApiStatusMapper.toHttpStatus(response));
    }

    @Test
    void fallbackToApiStatusWhenUnknownCode() {
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .status(ApiStatus.ERROR)
                .code("ERR-UNKNOWN")
                .build();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ApiStatusMapper.toHttpStatus(response));
    }

    @Test
    void warningDefaultsToBadRequest() {
        BaseResponse<Void> response = BaseResponse.<Void>builder()
                .status(ApiStatus.WARNING)
                .code("WARN-LIMIT")
                .build();

        assertEquals(HttpStatus.BAD_REQUEST, ApiStatusMapper.toHttpStatus(response));
    }
}
