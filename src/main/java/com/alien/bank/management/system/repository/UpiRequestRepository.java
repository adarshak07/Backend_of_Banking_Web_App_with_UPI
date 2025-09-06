package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.UpiRequest;
import com.alien.bank.management.system.entity.UpiRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UpiRequestRepository extends JpaRepository<UpiRequest, Long> {
    List<UpiRequest> findByPayerVpaAndStatus(String payerVpa, UpiRequestStatus status);
}


