package com.ejada.testsupport.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;
import java.util.Map;

public class RedisTestExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(RedisTestExtension.class);

    private static final int REDIS_PORT = 6379;

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(REDIS_PORT);

    @Override
    public void beforeAll(ExtensionContext context) {
        ensureContainerStarted(context);

        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("spring.data.redis.host", REDIS.getHost());
        properties.put("spring.data.redis.port", String.valueOf(REDIS.getMappedPort(REDIS_PORT)));

        Map<String, String> previousValues = SystemPropertyExtensionSupport.apply(properties);
        getClassStore(context).put(PropertiesHolder.KEY, new PropertiesHolder(previousValues));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        PropertiesHolder holder = getClassStore(context).remove(PropertiesHolder.KEY, PropertiesHolder.class);
        if (holder != null) {
            SystemPropertyExtensionSupport.restore(holder.previousValues());
        }
    }

    private void ensureContainerStarted(ExtensionContext context) {
        ExtensionContext.Store rootStore = context.getRoot().getStore(NAMESPACE);
        rootStore.getOrComputeIfAbsent("redis", key -> {
            if (!REDIS.isRunning()) {
                REDIS.start();
            }
            return (ExtensionContext.Store.CloseableResource) REDIS::stop;
        });
    }

    private ExtensionContext.Store getClassStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(RedisTestExtension.class, context.getRequiredTestClass()));
    }

    static GenericContainer<?> getContainer() {
        return REDIS;
    }

    private record PropertiesHolder(Map<String, String> previousValues) {
        private static final String KEY = "redis-properties";
    }
}
