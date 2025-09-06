package com.alien.bank.management.system.service;

import com.alien.bank.management.system.entity.User;

public interface AuditService {
    void logLoginAttempt(String email, String ipAddress, boolean success, String method);
    void logAccountLinking(User user, String provider, boolean success);
    void logAccountUnlinking(User user, String provider, boolean success);
    void logOAuthLogin(User user, String provider, boolean success);
}
