package com.ejada.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ejada.common.dto.ServiceResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ServiceResultResponsesTest {

    @Test
    void resolveHttpStatusReturnsUnauthorizedForAuthError() {
        ServiceResult<Void> result = ServiceResult.error(null, "ERR-AUTH-INVALID", "Invalid", List.of());

        assertEquals(HttpStatus.UNAUTHORIZED, ServiceResultResponses.resolveHttpStatus(result));
    }

    @Test
    void resolveHttpStatusReturnsInternalServerErrorForInternalFailures() {
        ServiceResult<Void> result = ServiceResult.error(null, "EINT000", "boom", List.of());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ServiceResultResponses.resolveHttpStatus(result));
    }

    @Test
    void resolveHttpStatusReturnsBadRequestWhenCodeMissing() {
        ServiceResult<Void> result = new ServiceResult<>(null, "", "desc", List.of(), null, null);

        assertEquals(HttpStatus.BAD_REQUEST, ServiceResultResponses.resolveHttpStatus(result));
    }

    @Test
    void resolveHttpStatusReturnsBadRequestForValidationCodesByDefault() {
        ServiceResult<Void> result = ServiceResult.error(null, "EVAL100", "fail", List.of("missing"));

        assertEquals(HttpStatus.BAD_REQUEST, ServiceResultResponses.resolveHttpStatus(result));
    }
}
