package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.RewardWallet;
import com.alien.bank.management.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface RewardWalletRepository extends JpaRepository<RewardWallet, Long> {
    Optional<RewardWallet> findByUser(User user);
    Optional<RewardWallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from RewardWallet w where w.user = ?1")
    Optional<RewardWallet> findByUserForUpdate(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from RewardWallet w where w.userId = ?1")
    Optional<RewardWallet> findByUserIdForUpdate(Long userId);
}


