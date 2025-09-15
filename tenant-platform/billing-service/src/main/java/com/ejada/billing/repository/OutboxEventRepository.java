package com.ejada.billing.repository;

import com.ejada.billing.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update OutboxEvent o
              set o.published = true,
                  o.publishedAt = :publishedAt
            where o.outboxEventId in :ids
           """)
    int markPublished(List<Long> ids, OffsetDateTime publishedAt);
}
