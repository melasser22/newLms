package com.ejada.starter_core.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;


import com.ejada.common.dto.BaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

/**
 * Tests for {@link JacksonConfig}.
 */
class JacksonConfigTest {

    @Test
    void serializingBaseResponseWithUnpagedPageDoesNotFail() throws JsonProcessingException {
        ObjectMapper mapper = new JacksonConfig().objectMapper();
        Page<String> emptyPage = Page.<String>empty();
        BaseResponse<Page<String>> resp = BaseResponse.success("ok", emptyPage);

        assertThatNoException().isThrownBy(() -> {
            String json = mapper.writeValueAsString(resp);
            assertThat(json).doesNotContain("pageable");
        });
    }
}
