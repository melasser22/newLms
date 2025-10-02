package com.ejada.subscription.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LenientUuidConverterTest {

    private final LenientUuidConverter converter = new LenientUuidConverter();

    @Test
    void convertReturnsNullWhenSourceIsBlank() {
        assertThat(converter.convert(null)).isNull();
        assertThat(converter.convert(" ")).isNull();
    }

    @Test
    void convertStripsCurlyBracesBeforeParsing() {
        UUID expected = UUID.randomUUID();
        String wrapped = "{" + expected + "}";

        assertThat(converter.convert(wrapped)).isEqualTo(expected);
    }

    @Test
    void convertDelegatesToUuidParsingForInvalidValues() {
        assertThatThrownBy(() -> converter.convert("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
