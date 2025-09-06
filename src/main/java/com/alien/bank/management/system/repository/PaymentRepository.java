package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.Payment;
import com.alien.bank.management.system.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRefId(String refId);

    List<Payment> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUserAndCreatedAtBetween(User user, Date start, Date end);
}


