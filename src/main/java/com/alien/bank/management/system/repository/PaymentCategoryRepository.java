package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.PaymentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentCategoryRepository extends JpaRepository<PaymentCategory, Long> {
    Optional<PaymentCategory> findByCode(String code);
}


