package com.ejada.billing.repository;

import com.ejada.billing.model.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageCounterRepository extends JpaRepository<UsageCounter, Long> {

    Optional<UsageCounter> findByExtSubscriptionIdAndConsumptionTypCd(Long extSubscriptionId, String consumptionTypCd);

    List<UsageCounter> findByExtSubscriptionId(Long extSubscriptionId);

    List<UsageCounter> findByExtSubscriptionIdInAndConsumptionTypCd(Collection<Long> subIds, String consumptionTypCd);

    @Query("""
           select uc from UsageCounter uc
           where uc.extSubscriptionId = :extSubscriptionId
             and uc.consumptionTypCd in :types
           """)
    List<UsageCounter> findAllBySubAndTypes(Long extSubscriptionId, Collection<String> types);
}
