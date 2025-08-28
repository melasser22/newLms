package com.shared.crypto.starter.metrics;

import com.shared.crypto.starter.InMemoryKeyProviderAutoConfiguration;
import com.shared.crypto.CryptoService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Binds minimal crypto metrics to Micrometer.
 * We avoid calling non-existent methods on CryptoService; we read key state from the starter KeyProvider.
 */
@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class, CryptoService.class})
public class CryptoMetricsConfiguration {

  @Bean
  @ConditionalOnBean({CryptoService.class, InMemoryKeyProviderAutoConfiguration.KeyProvider.class})
  @ConditionalOnMissingBean(name = "cryptoMetricsBinder")
  public MeterBinder cryptoMetricsBinder(InMemoryKeyProviderAutoConfiguration.KeyProvider provider) {
    return (MeterRegistry registry) -> {
      // Is there an active key?
      registry.gauge("crypto.keys.active", provider,
          p -> p.currentKeyId() != null ? 1.0 : 0.0);

      // Numeric representation of active KID (hash) to observe rotation changes
      registry.gauge("crypto.active.kid.hash", provider,
          p -> p.currentKeyId() != null ? p.currentKeyId().hashCode() : -1);
    };
  }
}
