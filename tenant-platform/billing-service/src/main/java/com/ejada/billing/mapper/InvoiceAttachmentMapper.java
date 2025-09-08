package com.ejada.billing.mapper;

import com.ejada.billing.dto.InvoiceAttachmentContentDto;
import com.ejada.billing.dto.InvoiceAttachmentMetaDto;
import com.ejada.billing.model.InvoiceAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Base64;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InvoiceAttachmentMapper {

    /* -------------------- Entity -> DTO (metadata only) -------------------- */

    @Mapping(target = "invoiceAttachmentId", source = "invoiceAttachmentId")
    @Mapping(target = "invoiceId",           source = "invoice.invoiceId")
    @Mapping(target = "fileName",            source = "fileNm")
    @Mapping(target = "mimeType",            source = "mimeTyp")
    @Mapping(target = "createdAt",           source = "createdAt")
    @Mapping(target = "sizeBytes",           expression = "java(size(entity.getContent()))")
    InvoiceAttachmentMetaDto toMetaDto(InvoiceAttachment entity);

    /* -------------------- Entity -> DTO (with Base64 content) -------------- */

    @Mapping(target = "invoiceAttachmentId", source = "invoiceAttachmentId")
    @Mapping(target = "invoiceId",           source = "invoice.invoiceId")
    @Mapping(target = "fileName",            source = "fileNm")
    @Mapping(target = "mimeType",            source = "mimeTyp")
    @Mapping(target = "createdAt",           source = "createdAt")
    @Mapping(target = "base64Content",       expression = "java(toBase64(entity.getContent()))")
    InvoiceAttachmentContentDto toContentDto(InvoiceAttachment entity);

    /* -------------------- Factory: build entity from primitives ------------- */
    /**
     * Convenience factory when you have invoiceId + raw bytes (e.g., after generating a PDF).
     * createdAt is filled by @PrePersist; override if you want to set explicitly.
     */
    @Mapping(target = "invoiceAttachmentId", ignore = true)
    @Mapping(target = "invoice",             expression = "java(com.ejada.billing.model.Invoice.ref(invoiceId))")
    @Mapping(target = "fileNm",              source = "fileName")
    @Mapping(target = "mimeTyp",             source = "mimeType")
    @Mapping(target = "content",             source = "content")
    @Mapping(target = "createdAt",           ignore = true)
    InvoiceAttachment toEntity(Long invoiceId, String fileName, String mimeType, byte[] content);

    /* -------------------- Helpers ------------------------------------------ */
    default Long size(byte[] content) {
        return content == null ? 0L : (long) content.length;
    }

    default String toBase64(byte[] content) {
        return content == null ? null : Base64.getEncoder().encodeToString(content);
    }
}
