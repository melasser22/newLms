// BaseResponse.java
package com.common.dto;

import com.common.context.CorrelationContextUtil;
import com.common.enums.StatusEnums.ApiStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import lombok.*;

import java.time.Instant;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {

    private ApiStatus status;
    private String code;

    @Nullable private String message;
    @Nullable private T data;

    @Builder.Default private Instant timestamp = Instant.now();

    // Correlation identifier for tracing across services
    private String correlationId;

    @JsonProperty("correlationId")
    public String getCorrelationId() {
        return (correlationId == null || correlationId.isBlank())
                ? CorrelationContextUtil.getCorrelationId()
                : correlationId;
    }
}
