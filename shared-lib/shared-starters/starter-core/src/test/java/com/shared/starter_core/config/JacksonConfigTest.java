package com.shared.starter_core.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;


import com.common.dto.BaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

/**
 * Tests for {@link JacksonConfig}.
 */
class JacksonConfigTest {

    @Test
    void serializingBaseResponseWithUnpagedPageDoesNotFail() {
        ObjectMapper mapper = new JacksonConfig().objectMapper();
        Page<String> emptyPage = Page.<String>empty();
        BaseResponse<Page<String>> resp = BaseResponse.success("ok", emptyPage);

        assertThatNoException().isThrownBy(() -> {
            String json = mapper.writeValueAsString(resp);
            assertThat(json).doesNotContain("pageable");
        });
    }
}
