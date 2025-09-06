package com.alien.bank.management.system.service;

import com.alien.bank.management.system.entity.RefreshToken;
import com.alien.bank.management.system.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken findByToken(String token);
    RefreshToken verifyExpiration(RefreshToken token);
    void revokeToken(String token);
    void revokeAllUserTokens(Long userId);
    void deleteExpiredTokens();
}
