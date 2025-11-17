package com.ejada.sending.service;

import com.ejada.sending.dto.AttachmentMetadataDto;
import java.util.List;

public interface AttachmentMergeService {
  List<AttachmentMetadataDto> merge(List<AttachmentMetadataDto> templateDefaults, List<AttachmentMetadataDto> adHoc);
}
