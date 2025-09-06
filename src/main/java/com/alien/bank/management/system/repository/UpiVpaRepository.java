package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.UpiVpa;
import com.alien.bank.management.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UpiVpaRepository extends JpaRepository<UpiVpa, Long> {
    Optional<UpiVpa> findByVpa(String vpa);
    List<UpiVpa> findByUser(User user);
    boolean existsByVpa(String vpa);
    List<UpiVpa> findByVpaContainingIgnoreCase(String vpa);
}


