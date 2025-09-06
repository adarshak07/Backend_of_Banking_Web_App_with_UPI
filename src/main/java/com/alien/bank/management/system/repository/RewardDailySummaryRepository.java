package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.RewardDailySummary;
import com.alien.bank.management.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Date;
import java.util.Optional;

public interface RewardDailySummaryRepository extends JpaRepository<RewardDailySummary, Long> {
    Optional<RewardDailySummary> findByUserAndDay(User user, Date day);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from RewardDailySummary d where d.user = ?1 and d.day = ?2")
    Optional<RewardDailySummary> findByUserAndDayForUpdate(User user, Date day);
}


