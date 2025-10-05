package com.ejada.testsupport.extensions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SecuritySchemaExtension.class)
class SecuritySchemaExtensionTest {

    @Test
    void configuresSecuritySchemaAndFlyway() {
        assertThat(System.getProperty("spring.jpa.properties.hibernate.default_schema")).isEqualTo("security");
        assertThat(System.getProperty("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces")).isEqualTo("true");
        assertThat(System.getProperty("spring.jpa.properties.hibernate.format_sql")).isEqualTo("true");
        assertThat(System.getProperty("spring.flyway.enabled")).isEqualTo("true");
        assertThat(System.getProperty("spring.flyway.schemas")).isEqualTo("public,security");
        assertThat(System.getProperty("spring.flyway.default-schema")).isEqualTo("security");
        assertThat(System.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration/common,classpath:db/migration/{vendor}");
    }
}
