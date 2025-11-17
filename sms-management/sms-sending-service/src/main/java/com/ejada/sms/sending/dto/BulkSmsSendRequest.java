package com.ejada.sms.sending.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkSmsSendRequest(
    @NotEmpty List<@Valid SmsSendRequest> entries
) {}
