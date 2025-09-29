package com.ejada.testsupport.extensions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class SetupSchemaExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String DEFAULT_SCHEMA = "setup";

    @Override
    public void beforeAll(ExtensionContext context) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("spring.jpa.properties.hibernate.default_schema", DEFAULT_SCHEMA);
        properties.put("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", "true");
        properties.put("spring.jpa.properties.hibernate.format_sql", "true");
        properties.put("spring.flyway.enabled", "true");
        properties.put("spring.flyway.schemas", "public," + DEFAULT_SCHEMA);
        properties.put("spring.flyway.default-schema", DEFAULT_SCHEMA);
        properties.put("spring.flyway.locations", "classpath:db/migration/common,classpath:db/migration/{vendor}");

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

    private ExtensionContext.Store getClassStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(SetupSchemaExtension.class, context.getRequiredTestClass()));
    }

    private record PropertiesHolder(Map<String, String> previousValues) {
        private static final String KEY = "setup-schema-properties";
    }
}
