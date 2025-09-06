package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.RewardLedger;
import com.alien.bank.management.system.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardLedgerRepository extends JpaRepository<RewardLedger, Long> {
    List<RewardLedger> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}


