package com.ejada.testsupport.assertions;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.dto.ServiceResult;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.Objects;

/**
 * Fluent AssertJ helpers for verifying shared response wrappers in tests.
 */
public final class ResponseAssertions {

    private ResponseAssertions() {
        // utility
    }

    public static <T> BaseResponseAssert<T> assertThatBaseResponse(BaseResponse<T> actual) {
        return new BaseResponseAssert<>(actual);
    }

    public static <T> ServiceResultAssert<T> assertThatServiceResult(ServiceResult<T> actual) {
        return new ServiceResultAssert<>(actual);
    }

    public static final class BaseResponseAssert<T> extends AbstractAssert<BaseResponseAssert<T>, BaseResponse<T>> {

        BaseResponseAssert(BaseResponse<T> actual) {
            super(actual, BaseResponseAssert.class);
        }

        public BaseResponseAssert<T> isSuccess() {
            isNotNull();
            if (!actual.isSuccess()) {
                failWithMessage("Expected BaseResponse to be successful but was %s", actual);
            }
            return this;
        }

        public BaseResponseAssert<T> isError() {
            isNotNull();
            if (!actual.isError()) {
                failWithMessage("Expected BaseResponse to be error but was %s", actual);
            }
            return this;
        }

        public BaseResponseAssert<T> hasCode(String code) {
            isNotNull();
            if (!Objects.equals(actual.getCode(), code)) {
                failWithMessage("Expected code to be <%s> but was <%s>", code, actual.getCode());
            }
            return this;
        }

        public BaseResponseAssert<T> hasMessage(String message) {
            isNotNull();
            if (!Objects.equals(actual.getMessage(), message)) {
                failWithMessage("Expected message to be <%s> but was <%s>", message, actual.getMessage());
            }
            return this;
        }

        public BaseResponseAssert<T> hasDataSatisfying(java.util.function.Consumer<T> consumer) {
            isNotNull();
            Assertions.assertThat(actual.getData()).satisfies(consumer);
            return this;
        }
    }

    public static final class ServiceResultAssert<T> extends AbstractAssert<ServiceResultAssert<T>, ServiceResult<T>> {

        ServiceResultAssert(ServiceResult<T> actual) {
            super(actual, ServiceResultAssert.class);
        }

        public ServiceResultAssert<T> isSuccess() {
            isNotNull();
            if (!actual.success()) {
                failWithMessage("Expected ServiceResult to be success but statusCode was <%s>", actual.statusCode());
            }
            return this;
        }

        public ServiceResultAssert<T> isFailure() {
            isNotNull();
            if (!actual.failure()) {
                failWithMessage("Expected ServiceResult to be failure but statusCode was <%s>", actual.statusCode());
            }
            return this;
        }

        public ServiceResultAssert<T> hasStatusCode(String code) {
            isNotNull();
            if (!Objects.equals(actual.statusCode(), code)) {
                failWithMessage("Expected status code to be <%s> but was <%s>", code, actual.statusCode());
            }
            return this;
        }

        public ServiceResultAssert<T> hasPayloadSatisfying(java.util.function.Consumer<T> consumer) {
            isNotNull();
            Assertions.assertThat(actual.payload()).satisfies(consumer);
            return this;
        }
    }
}
