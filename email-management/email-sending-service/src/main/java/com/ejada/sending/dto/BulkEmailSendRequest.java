package com.ejada.sending.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkEmailSendRequest(@NotEmpty List<@Valid EmailSendRequest> entries) {}
