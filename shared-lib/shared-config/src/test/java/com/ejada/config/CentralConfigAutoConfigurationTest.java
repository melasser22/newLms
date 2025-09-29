package com.ejada.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
	    classes = CentralConfigAutoConfiguration.class,
	    properties = "spring.cloud.config.enabled=false"
        )class CentralConfigAutoConfigurationTest {

    @Autowired
    private AppProperties props;

    @Test
    void loadsDefaultEnvironment() {
        assertThat(props.getEnv()).isEqualTo("dev");
    }
}
