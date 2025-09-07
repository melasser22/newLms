package com.ejada.subscription.repository;

import com.ejada.subscription.model.OutboxEvent;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long>, JpaSpecificationExecutor<OutboxEvent> {

    List<OutboxEvent> findFirst100ByProcessedAtIsNullOrderByCreatedAtAsc();
}
