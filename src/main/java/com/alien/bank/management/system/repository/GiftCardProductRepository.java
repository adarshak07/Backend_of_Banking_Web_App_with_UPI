package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.GiftCardProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftCardProductRepository extends JpaRepository<GiftCardProduct, Long> {
    List<GiftCardProduct> findByActiveTrueOrderByCostCoinsAsc();
}


