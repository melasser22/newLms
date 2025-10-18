package com.ejada.gateway.loadbalancer;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

/**
 * Lazily initializes the underlying {@link ServiceInstanceListSupplier} chain to avoid eagerly
 * resolving beans while the application context is still wiring dependencies.
 */
public class LazyServiceInstanceListSupplier implements ServiceInstanceListSupplier {

  private final Supplier<ServiceInstanceListSupplier> delegateSupplier;
  private volatile ServiceInstanceListSupplier delegate;

  public LazyServiceInstanceListSupplier(Supplier<ServiceInstanceListSupplier> delegateSupplier) {
    this.delegateSupplier = Objects.requireNonNull(delegateSupplier, "delegateSupplier");
  }

  private ServiceInstanceListSupplier getDelegate() {
    ServiceInstanceListSupplier result = delegate;
    if (result == null) {
      synchronized (this) {
        result = delegate;
        if (result == null) {
          result = Objects.requireNonNull(delegateSupplier.get(),
              "delegateSupplier returned null ServiceInstanceListSupplier");
          delegate = result;
        }
      }
    }
    return result;
  }

  @Override
  public Flux<List<ServiceInstance>> get() {
    return getDelegate().get();
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Flux<List<ServiceInstance>> get(Request request) {
    return getDelegate().get(request);
  }

  @Override
  public String getServiceId() {
    return getDelegate().getServiceId();
  }
}
