package com.ejada.template.repository;

import com.ejada.template.domain.entity.EmailEventEntity;
import com.ejada.template.domain.enums.EmailEventType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailEventRepository extends JpaRepository<EmailEventEntity, Long> {

  @Query(
      "select e.eventType as type, count(e) as total "
          + "from EmailEventEntity e "
          + "where e.eventTimestamp between :from and :to "
          + "group by e.eventType")
  List<EventAggregation> aggregateBetween(
      @Param("from") Instant from, @Param("to") Instant to);

  interface EventAggregation {
    EmailEventType getType();

    long getTotal();
  }
}
