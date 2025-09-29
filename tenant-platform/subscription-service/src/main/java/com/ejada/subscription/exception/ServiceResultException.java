package com.ejada.subscription.exception;

import com.ejada.subscription.dto.ServiceResult;

/**
 * Runtime exception that carries a {@link ServiceResult} payload. Throwing this from a
 * transactional service method ensures the transaction is rolled back while still allowing the
 * controller layer to return the pre-built response body.
 */
public class ServiceResultException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient ServiceResult<?> result;

    public ServiceResultException(final ServiceResult<?> result) {
        super(result != null ? result.statusDesc() : null);
        this.result = result;
    }

    public ServiceResultException(final ServiceResult<?> result, final Throwable cause) {
        super(result != null ? result.statusDesc() : null, cause);
        this.result = result;
    }

    @SuppressWarnings("unchecked")
    public <T> ServiceResult<T> getResult() {
        return (ServiceResult<T>) result;
    }
}
