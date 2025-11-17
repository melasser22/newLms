package com.ejada.email.sending.service.impl;

import com.ejada.email.sending.dto.AttachmentMetadataDto;
import com.ejada.email.sending.service.AttachmentMergeService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class AttachmentMergeServiceImpl implements AttachmentMergeService {

  @Override
  public List<AttachmentMetadataDto> merge(
      List<AttachmentMetadataDto> templateDefaults, List<AttachmentMetadataDto> adHoc) {
    Map<String, AttachmentMetadataDto> merged = new LinkedHashMap<>();
    Stream.concat(
            templateDefaults == null ? Stream.empty() : templateDefaults.stream(),
            adHoc == null ? Stream.empty() : adHoc.stream())
        .forEach(attachment -> merged.putIfAbsent(attachment.fileName(), attachment));
    return List.copyOf(merged.values());
  }
}
