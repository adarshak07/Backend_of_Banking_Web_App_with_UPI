package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.Account;
import com.alien.bank.management.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByLast4Digits(String last4Digits);

    List<Account> findAllByUser(User user);
    
    long countByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = ?1")
    Optional<Account> findByIdForUpdate(Long id);
}
