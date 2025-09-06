
package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.SuggestOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SuggestOperatorRepository extends JpaRepository<SuggestOperator, Long> {
    List<SuggestOperator> findByNameContainingIgnoreCase(String q);
}
