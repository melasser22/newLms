package com.ejada.billing.repository;

import com.ejada.billing.model.InvoiceAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceAttachmentRepository extends JpaRepository<InvoiceAttachment, Long> {

    Optional<InvoiceAttachment> findFirstByInvoice_InvoiceIdOrderByCreatedAtDesc(Long invoiceId);
}
