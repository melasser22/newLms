package com.ejada.template.mapper;

import com.ejada.template.domain.entity.TemplateEntity;
import com.ejada.template.domain.entity.TemplateVersionEntity;
import com.ejada.template.domain.value.AttachmentMetadata;
import com.ejada.template.dto.AttachmentMetadataDto;
import com.ejada.template.dto.TemplateDto;
import com.ejada.template.dto.TemplateVersionDto;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TemplateMapper {

  TemplateDto toDto(TemplateEntity entity);

  List<TemplateVersionDto> toVersionDtos(Set<TemplateVersionEntity> versions);

  TemplateVersionDto toDto(TemplateVersionEntity entity);

  List<AttachmentMetadataDto> toAttachmentDtos(Set<AttachmentMetadata> attachments);

  Set<AttachmentMetadata> toAttachments(List<AttachmentMetadataDto> attachments);

}
