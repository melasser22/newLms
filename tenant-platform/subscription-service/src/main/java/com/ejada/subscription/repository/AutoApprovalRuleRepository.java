package com.ejada.subscription.repository;

import com.ejada.subscription.model.AutoApprovalRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoApprovalRuleRepository extends JpaRepository<AutoApprovalRule, Long> {

    List<AutoApprovalRule> findByIsActiveTrueOrderByPriorityDesc();
}
