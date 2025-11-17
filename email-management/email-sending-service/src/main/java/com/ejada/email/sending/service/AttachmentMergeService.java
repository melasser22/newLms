package com.ejada.email.sending.service;

import com.ejada.email.sending.dto.AttachmentMetadataDto;
import java.util.List;

public interface AttachmentMergeService {
  List<AttachmentMetadataDto> merge(List<AttachmentMetadataDto> templateDefaults, List<AttachmentMetadataDto> adHoc);
}
