package com.shared.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentPropertiesTest {
    @Test
    void hasDefaultValues() {
        EnvironmentProperties props = new EnvironmentProperties();
        assertThat(props.getEnvironment()).isEqualTo("local");
        assertThat(props.getVersion()).isEqualTo("0.0.1");
    }
}
