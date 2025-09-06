package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.UserOAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOAuthProviderRepository extends JpaRepository<UserOAuthProvider, Long> {
    Optional<UserOAuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<UserOAuthProvider> findByProviderAndProviderEmail(String provider, String providerEmail);
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}
