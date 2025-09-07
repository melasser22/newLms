package com.ejada.billing.dto;

import java.time.OffsetDateTime;

/** Lightweight metadata for listing or headers (no content). */
public record InvoiceAttachmentMetaDto(
        Long invoiceAttachmentId,
        Long invoiceId,
        String fileName,
        String mimeType,
        OffsetDateTime createdAt,
        Long sizeBytes
) {}
