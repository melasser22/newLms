package com.lms.tenant.events.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

  @Query(value = """
    select * from outbox_event
     where status in ('NEW','FAILED')
     order by occurred_at
     for update skip locked
     limit :n
  """ , nativeQuery = true)
  List<OutboxEvent> lockNextBatch(@Param("n") int n);
}
