// ErrorResponse.java
package com.common.dto;

import com.common.context.CorrelationContextUtil;
import com.common.enums.StatusEnums.ApiStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @Builder.Default private ApiStatus status = ApiStatus.ERROR;
    private String code;
    private String message;
    private List<String> details;

    // Correlation ID for log/trace correlation
    private String correlationId;

    // Optional tenant awareness
    private String tenantId;

    @Builder.Default private Instant timestamp = Instant.now();

    @JsonProperty("correlationId")
    public String getCorrelationId() {
        return (correlationId == null || correlationId.isBlank())
                ? CorrelationContextUtil.getCorrelationId()
                : correlationId;
    }
}
