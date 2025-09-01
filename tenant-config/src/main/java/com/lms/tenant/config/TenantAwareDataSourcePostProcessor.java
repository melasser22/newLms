package com.lms.tenant.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/** Wraps any primary DataSource with TenantAwareDataSource when enabled. */
@Component
@ConditionalOnProperty(prefix = "lms.tenant.resolution", name = "wrap-data-source", havingValue = "true", matchIfMissing = true)
public class TenantAwareDataSourcePostProcessor implements BeanPostProcessor {
  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof DataSource ds && !(bean instanceof TenantAwareDataSource)) {
      return new TenantAwareDataSource(ds);
    }
    return bean;
  }
}
