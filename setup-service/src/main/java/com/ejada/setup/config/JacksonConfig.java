package com.ejada.setup.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

    // ObjectMapper default for web (HTTP)
    @Bean
    @Primary
    public ObjectMapper httpObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }
}
