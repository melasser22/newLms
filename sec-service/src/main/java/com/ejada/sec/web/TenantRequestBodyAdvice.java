package com.ejada.sec.web;

import com.ejada.common.dto.BaseRequest;
import com.ejada.sec.context.TenantContextProvider;
import java.lang.reflect.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * Automatically enriches every {@link BaseRequest} with tenant identifiers resolved from the
 * current request context or JWT token.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class TenantRequestBodyAdvice extends RequestBodyAdviceAdapter {

  private final TenantContextProvider tenantContextProvider;

  @Override
  public boolean supports(MethodParameter methodParameter, Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    Class<?> targetClass = ResolvableType.forType(targetType).resolve();
    return targetClass != null && BaseRequest.class.isAssignableFrom(targetClass);
  }

  @Override
  public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
      Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
    if (body instanceof BaseRequest request) {
      request.setTenantId(tenantContextProvider.requireTenantId());
      request.setInternalTenantId(
          tenantContextProvider.resolveInternalTenantId().orElse(null));
    }
    return body;
  }
}
