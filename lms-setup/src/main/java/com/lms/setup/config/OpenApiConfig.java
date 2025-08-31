package com.lms.setup.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LMS Setup API")
                        .description("""
                            Learning Management System Setup API for managing countries, cities, lookups, resources, and system parameters.

                            ## Features
                            - **Country Management**: CRUD operations for countries with multilingual support
                            - **City Management**: CRUD operations for cities with country relationships
                            - **Lookup Management**: Dynamic lookup values for system configuration
                            - **Resource Management**: System resources and permissions
                            - **System Parameters**: Configuration parameters for system behavior

                            ## Authentication
                            This API uses JWT-based authentication. Include the JWT token in the Authorization header:
                            `Authorization: Bearer <your-jwt-token>`

                            ## Rate Limiting
                            - General endpoints: 100 requests per minute
                            - Sensitive operations: 10 requests per minute

                            ## Error Handling
                            All errors follow a consistent format with error codes and messages.
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LMS Development Team")
                                .email("dev@lms.com")
                                .url("https://lms.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080/core").description("Local Development"),
                        new Server().url("https://api.lms.com/core").description("Production API"),
                        new Server().url("https://staging-api.lms.com/core").description("Staging Environment")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\""))
                        .addSecuritySchemes("apiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API Key for service-to-service authentication")))
                .tags(List.of(
                        new Tag().name("Country Management").description("APIs for managing countries"),
                        new Tag().name("City Management").description("APIs for managing cities"),
                        new Tag().name("Lookup Management").description("APIs for managing lookup values"),
                        new Tag().name("Resource Management").description("APIs for managing system resources"),
                        new Tag().name("System Parameter Management").description("APIs for managing system parameters"),
                        new Tag().name("Health Check").description("Health and monitoring endpoints")
                ));
    }
}
