package com.ejada.subscription.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.Test;

class YesNoBooleanDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesYToTrue() throws Exception {
        Wrapper wrapper = mapper.readValue("{\"value\":\"Y\"}", Wrapper.class);
        assertThat(wrapper.value()).isTrue();
    }

    @Test
    void deserializesNToFalse() throws Exception {
        Wrapper wrapper = mapper.readValue("{\"value\":\"N\"}", Wrapper.class);
        assertThat(wrapper.value()).isFalse();
    }

    @Test
    void acceptsBooleanLiterals() throws Exception {
        Wrapper wrapperTrue = mapper.readValue("{\"value\":true}", Wrapper.class);
        Wrapper wrapperFalse = mapper.readValue("{\"value\":false}", Wrapper.class);
        assertThat(wrapperTrue.value()).isTrue();
        assertThat(wrapperFalse.value()).isFalse();
    }

    @Test
    void rejectsInvalidString() {
        assertThatThrownBy(() -> mapper.readValue("{\"value\":\"maybe\"}", Wrapper.class))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining("Expected 'Y' or 'N'");
    }

    private record Wrapper(@JsonDeserialize(using = YesNoBooleanDeserializer.class) Boolean value) {}
}
