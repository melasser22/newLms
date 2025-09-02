package com.ejada.starter_core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;

@Configuration
public class JacksonConfig {

    /**
     * Global ObjectMapper configuration.
     * This mapper is automatically picked up by Spring Boot.
     */
    @Bean
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
        mapper.addMixIn(PageImpl.class, PageImplMixIn.class);

        return mapper;
    }

    /** Mixin to ignore the "pageable" property of {@link PageImpl}. */
    @JsonIgnoreProperties("pageable")
    private interface PageImplMixIn {}
}
