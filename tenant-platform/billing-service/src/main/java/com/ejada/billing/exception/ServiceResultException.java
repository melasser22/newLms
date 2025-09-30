package com.ejada.billing.exception;

import com.ejada.common.dto.ServiceResult;

/**
 * Runtime exception carrying a {@link ServiceResult}. Throwing this from transactional service
 * methods guarantees rollback while still letting controllers return the prepared response body.
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
