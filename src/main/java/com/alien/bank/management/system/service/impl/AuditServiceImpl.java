package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    @Override
    public void logLoginAttempt(String email, String ipAddress, boolean success, String method) {
        String status = success ? "SUCCESS" : "FAILED";
        log.info("LOGIN_ATTEMPT: email={}, ip={}, method={}, status={}, timestamp={}", 
                email, ipAddress, method, status, LocalDateTime.now());
    }

    @Override
    public void logAccountLinking(User user, String provider, boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        log.info("ACCOUNT_LINKING: user_id={}, email={}, provider={}, status={}, timestamp={}", 
                user.getId(), user.getEmail(), provider, status, LocalDateTime.now());
    }

    @Override
    public void logAccountUnlinking(User user, String provider, boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        log.info("ACCOUNT_UNLINKING: user_id={}, email={}, provider={}, status={}, timestamp={}", 
                user.getId(), user.getEmail(), provider, status, LocalDateTime.now());
    }

    @Override
    public void logOAuthLogin(User user, String provider, boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        log.info("OAUTH_LOGIN: user_id={}, email={}, provider={}, status={}, timestamp={}", 
                user.getId(), user.getEmail(), provider, status, LocalDateTime.now());
    }
}
