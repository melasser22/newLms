package com.ejada.billing.dto;

import java.time.OffsetDateTime;

/** Full payload with Base64 content when you need to serialize the file in JSON. */
public record InvoiceAttachmentContentDto(
        Long invoiceAttachmentId,
        Long invoiceId,
        String fileName,
        String mimeType,
        OffsetDateTime createdAt,
        String base64Content
) { }
