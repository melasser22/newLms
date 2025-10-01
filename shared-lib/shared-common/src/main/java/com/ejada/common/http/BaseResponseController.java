package com.ejada.common.http;

import com.ejada.common.dto.BaseResponse;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Convenience support for REST controllers that expose endpoints returning {@link BaseResponse} payloads.
 * Provides helper methods that delegate to {@link BaseResponseEntityFactory} to apply the appropriate
 * HTTP status mapping while keeping controller code concise.
 */
public abstract class BaseResponseController {

    protected <T> ResponseEntity<BaseResponse<T>> respond(final BaseResponse<T> response) {
        return BaseResponseEntityFactory.build(response);
    }

    protected <T> ResponseEntity<BaseResponse<T>> respond(
            final BaseResponse<T> response,
            final HttpStatus successStatusOverride) {
        return BaseResponseEntityFactory.build(response, successStatusOverride);
    }

    protected <T> ResponseEntity<BaseResponse<T>> respond(final Supplier<BaseResponse<T>> responseSupplier) {
        return respond(responseSupplier, null);
    }

    protected <T> ResponseEntity<BaseResponse<T>> respond(
            final Supplier<BaseResponse<T>> responseSupplier,
            final HttpStatus successStatusOverride) {
        BaseResponse<T> response = responseSupplier.get();
        return BaseResponseEntityFactory.build(response, successStatusOverride);
    }
}
