package com.ejada.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class BulkEmailSendRequest {
  @NotEmpty private List<@Valid EmailSendRequest> sends;
}
