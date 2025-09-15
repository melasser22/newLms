package com.ejada.billing.repository;

import com.ejada.billing.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findByExtSubscriptionIdOrderByInvoiceDtDesc(Long extSubscriptionId, Pageable pageable);

    Page<Invoice> findByExtCustomerIdOrderByInvoiceDtDesc(Long extCustomerId, Pageable pageable);

    Optional<Invoice> findTopByExtSubscriptionIdOrderByInvoiceDtDesc(Long extSubscriptionId);

    Page<Invoice> findByStatusCdAndInvoiceDtBetweenOrderByInvoiceDtDesc(String statusCd,
                                                                        LocalDate from, LocalDate to,
                                                                        Pageable pageable);
}
