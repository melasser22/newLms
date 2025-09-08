package com.ejada.billing.controller;

import com.ejada.billing.service.InvoiceAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/billing")
public final class InvoiceAttachmentController {

    private final InvoiceAttachmentService svc;

    /** Streams the latest attachment for an invoice (inline). */
    @GetMapping("/invoices/{invoiceId}/attachment")
    public ResponseEntity<byte[]> download(@PathVariable final Long invoiceId) throws IOException {
        var view = svc.getLatestAttachment(invoiceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(view.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + view.fileName() + "\"")
                .body(view.resource().getContentAsByteArray());
    }
}
