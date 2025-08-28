package com.common.dto;

import com.common.enums.StatusEnums.ApiStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BaseResponseTest {

    @Test
    void builderCreatesExpectedResponse() {
        Instant now = Instant.now();
        BaseResponse<String> response = BaseResponse.<String>builder()
                .status(ApiStatus.SUCCESS)
                .code("SUCCESS-200")
                .message("ok")
                .data("payload")
                .timestamp(now)
                .build();

        assertEquals(ApiStatus.SUCCESS, response.getStatus());
        assertEquals("SUCCESS-200", response.getCode());
        assertEquals("ok", response.getMessage());
        assertEquals("payload", response.getData());
        assertEquals(now, response.getTimestamp());
        assertTrue(response.isSuccess());
    }

    @Test
    void equalityBasedOnFields() {
        Instant now = Instant.now();
        BaseResponse<String> r1 = BaseResponse.<String>builder()
                .status(ApiStatus.ERROR)
                .code("ERR")
                .message("bad")
                .timestamp(now)
                .build();
        BaseResponse<String> r2 = BaseResponse.<String>builder()
                .status(ApiStatus.ERROR)
                .code("ERR")
                .message("bad")
                .timestamp(now)
                .build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
