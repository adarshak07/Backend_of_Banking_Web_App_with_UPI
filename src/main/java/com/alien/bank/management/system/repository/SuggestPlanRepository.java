
package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.SuggestPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SuggestPlanRepository extends JpaRepository<SuggestPlan, Long> {
    List<SuggestPlan> findByLabelContainingIgnoreCase(String q);
    List<SuggestPlan> findByOperatorId(Long operatorId);
}
