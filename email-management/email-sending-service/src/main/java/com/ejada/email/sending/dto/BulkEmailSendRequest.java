package com.ejada.email.sending.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkEmailSendRequest(@NotEmpty List<@Valid EmailSendRequest> entries) {

  public BulkEmailSendRequest {
    entries = List.copyOf(entries);
  }
}
