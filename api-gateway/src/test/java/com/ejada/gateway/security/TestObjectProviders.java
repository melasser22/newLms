package com.ejada.gateway.security;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;

public final class TestObjectProviders {
  private TestObjectProviders() {
  }

  static <T> ObjectProvider<T> of(T instance) {
    return new ObjectProvider<T>() {
      @Override
      public T getObject(Object... args) {
        return instance;
      }

      @Override
      public T getObject() {
        return instance;
      }

      @Override
      public T getIfAvailable() {
        return instance;
      }

      @Override
      public T getIfAvailable(Supplier<T> defaultSupplier) {
        return instance != null ? instance : defaultSupplier.get();
      }

      @Override
      public void ifAvailable(Consumer<T> dependencyConsumer) {
        if (instance != null) {
          dependencyConsumer.accept(instance);
        }
      }

      @Override
      public T getIfUnique() {
        return instance;
      }

      @Override
      public T getIfUnique(Supplier<T> defaultSupplier) {
        return instance != null ? instance : defaultSupplier.get();
      }

      @Override
      public void ifUnique(Consumer<T> dependencyConsumer) {
        if (instance != null) {
          dependencyConsumer.accept(instance);
        }
      }
    };
  }
}
