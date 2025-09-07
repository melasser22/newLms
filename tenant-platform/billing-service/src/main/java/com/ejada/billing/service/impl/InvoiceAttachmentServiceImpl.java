package com.ejada.billing.service.impl;

import com.ejada.billing.model.InvoiceAttachment;
import com.ejada.billing.repository.InvoiceAttachmentRepository;
import com.ejada.billing.service.InvoiceAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceAttachmentServiceImpl implements InvoiceAttachmentService {

    private final InvoiceAttachmentRepository repo;

    @Override
    @Transactional(readOnly = true)
    public AttachmentView getLatestAttachment(Long invoiceId) {
        InvoiceAttachment ia = repo.findFirstByInvoice_InvoiceIdOrderByCreatedAtDesc(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("No attachment for invoice " + invoiceId));
        return new AttachmentView(ia.getFileNm(), ia.getMimeTyp(), new ByteArrayResource(ia.getContent()));
    }
}
