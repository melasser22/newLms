package com.ejada.testsupport.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.LinkedHashMap;
import java.util.Map;

public class PostgresTestExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(PostgresTestExtension.class);

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Override
    public void beforeAll(ExtensionContext context) {
        ensureContainerStarted(context);

        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", POSTGRES.getJdbcUrl());
        properties.put("spring.datasource.username", POSTGRES.getUsername());
        properties.put("spring.datasource.password", POSTGRES.getPassword());

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
        rootStore.getOrComputeIfAbsent("postgres", key -> {
            if (!POSTGRES.isRunning()) {
                POSTGRES.start();
            }
            return (ExtensionContext.Store.CloseableResource) POSTGRES::stop;
        });
    }

    private ExtensionContext.Store getClassStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(PostgresTestExtension.class, context.getRequiredTestClass()));
    }

    static PostgreSQLContainer<?> getContainer() {
        return POSTGRES;
    }

    private record PropertiesHolder(Map<String, String> previousValues) {
        private static final String KEY = "postgres-properties";
    }
}
