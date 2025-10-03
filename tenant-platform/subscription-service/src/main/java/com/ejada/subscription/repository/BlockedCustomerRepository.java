package com.ejada.subscription.repository;

import com.ejada.subscription.model.BlockedCustomer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedCustomerRepository extends JpaRepository<BlockedCustomer, Long> {

    Optional<BlockedCustomer> findFirstByExtCustomerIdAndIsActiveTrue(Long extCustomerId);

    Optional<BlockedCustomer> findFirstByEmailAndIsActiveTrue(String email);
}
