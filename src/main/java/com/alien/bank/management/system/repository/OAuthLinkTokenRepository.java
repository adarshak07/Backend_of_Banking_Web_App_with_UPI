package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.OAuthLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OAuthLinkTokenRepository extends JpaRepository<OAuthLinkToken, Long> {
    Optional<OAuthLinkToken> findByToken(String token);
    
    @Modifying
    @Query("DELETE FROM OAuthLinkToken olt WHERE olt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
