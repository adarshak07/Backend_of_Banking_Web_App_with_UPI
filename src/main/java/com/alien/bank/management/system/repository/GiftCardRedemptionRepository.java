package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.GiftCardRedemption;
import com.alien.bank.management.system.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftCardRedemptionRepository extends JpaRepository<GiftCardRedemption, Long> {
    List<GiftCardRedemption> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}


