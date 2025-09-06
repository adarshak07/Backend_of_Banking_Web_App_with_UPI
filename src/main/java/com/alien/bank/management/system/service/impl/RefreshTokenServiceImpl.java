package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.RefreshToken;
import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.repository.RefreshTokenRepository;
import com.alien.bank.management.system.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${mybank.refresh.expiry-days:30}")
    private int refreshTokenExpiryDays;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke all existing tokens for this user
        revokeAllUserTokens(user.getId());

        String token = UUID.randomUUID().toString();
        String tokenHash = hashToken(token);
        
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(refreshTokenExpiryDays);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiryDate)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken findByToken(String token) {
        String tokenHash = hashToken(token);
        return refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = findByToken(token);
        if (refreshToken != null) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        }
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    private String hashToken(String token) {
        // Simple hash for demo - in production, use proper hashing like BCrypt
        return String.valueOf(token.hashCode());
    }
}
