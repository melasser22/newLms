package com.ejada.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "shared.security.resource-server.enabled=false")
@ActiveProfiles("test")
class CatalogServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
