package com.ejada.billing.repository;

import com.ejada.billing.model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceInvoiceIdOrderByLineNoAsc(Long invoiceId);
}
