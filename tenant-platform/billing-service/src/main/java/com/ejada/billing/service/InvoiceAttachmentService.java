package com.ejada.billing.service;

import org.springframework.core.io.Resource;

public interface InvoiceAttachmentService {

    record AttachmentView(String fileName, String mimeType, Resource resource) {}

    /** Returns the latest attachment for the given invoiceId. */
    AttachmentView getLatestAttachment(Long invoiceId);
}
