package com.ejada.starter_core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.ClassUtils;

@Configuration
public class JacksonConfig {

    /**
     * Global ObjectMapper configuration.
     * This mapper is automatically picked up by Spring Boot.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule for LocalDate/LocalDateTime serialization
        mapper.registerModule(new JavaTimeModule());

        // Prevent failures on unknown JSON fields
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Write dates as ISO-8601 instead of timestamps
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Exclude null fields from JSON output
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Avoid UnsupportedOperationException when serializing PageImpl#pageable
        registerPageImplMixin(mapper);

        return mapper;
    }

    /** Mixin to ignore the "pageable" property of {@link PageImpl}. */
    @JsonIgnoreProperties("pageable")
    private interface PageImplMixIn {}

    /**
     * Registers the mixin for {@code PageImpl} only if Spring Data is on the classpath.
     * This avoids a hard dependency on {@code spring-data-commons} for services that
     * don't need it while preserving the behavior for those that do.
     */
    private void registerPageImplMixin(ObjectMapper mapper) {
        if (!ClassUtils.isPresent("org.springframework.data.domain.PageImpl", getClass().getClassLoader())) {
            return;
        }

        try {
            Class<?> pageImplClass = ClassUtils.forName("org.springframework.data.domain.PageImpl", getClass().getClassLoader());
            mapper.addMixIn(pageImplClass, PageImplMixIn.class);
        } catch (ClassNotFoundException ex) {
            // Should not happen because isPresent() returned true, but ignore defensively
        }
    }
}
