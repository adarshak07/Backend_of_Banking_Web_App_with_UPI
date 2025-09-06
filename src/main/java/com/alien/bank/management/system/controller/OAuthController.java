package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.repository.*;
import com.alien.bank.management.system.security.JwtService;
import com.alien.bank.management.system.service.RefreshTokenService;
import com.alien.bank.management.system.service.AuditService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final UserRepository userRepository;
    private final UserOAuthProviderRepository userOAuthProviderRepository;
    private final OAuthLinkTokenRepository oauthLinkTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${mybank.jwt.expires-ms:900000}")
    private long jwtExpirationMs;

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = getCookieValue(request, "MYBANK_REFRESH");
            if (refreshToken == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Refresh token not found"));
            }

            RefreshToken token = refreshTokenService.findByToken(refreshToken);
            if (token == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid refresh token"));
            }

            token = refreshTokenService.verifyExpiration(token);
            User user = token.getUser();

            // Generate new access token
            String newJwt = jwtService.generateToken(user);

            // Set new access token cookie
            Cookie accessCookie = new Cookie("MYBANK_ACCESS", newJwt);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge((int) (jwtExpirationMs / 1000));
            response.addCookie(accessCookie);

            return ResponseEntity.ok(Map.of("access_token", newJwt));
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Token refresh failed"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = getCookieValue(request, "MYBANK_REFRESH");
            if (refreshToken != null) {
                refreshTokenService.revokeToken(refreshToken);
            }

            // Clear cookies
            clearAuthCookies(response);

            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            log.error("Error during logout", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Logout failed"));
        }
    }

    @PostMapping("/link/confirm")
    public ResponseEntity<Map<String, String>> confirmAccountLinking(
            @RequestParam String token,
            @RequestParam String password,
            HttpServletResponse response) {
        try {
            Optional<OAuthLinkToken> linkTokenOpt = oauthLinkTokenRepository.findByToken(token);
            if (linkTokenOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid link token"));
            }

            OAuthLinkToken linkToken = linkTokenOpt.get();
            if (Boolean.TRUE.equals(linkToken.getUsed()) || linkToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Link token expired or already used"));
            }

            User user = linkToken.getUser();
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user"));
            }

            // Verify password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid password"));
            }

            // Create OAuth provider link
            UserOAuthProvider oauthProvider = UserOAuthProvider.builder()
                    .user(user)
                    .provider(linkToken.getProvider())
                    .providerUserId(linkToken.getProviderUserId())
                    .providerEmail(linkToken.getProviderEmail())
                    .build();

            userOAuthProviderRepository.save(oauthProvider);

            // Mark token as used
            linkToken.setUsed(true);
            oauthLinkTokenRepository.save(linkToken);

            // Update user's OAuth login time
            user.setLastOauthLogin(LocalDateTime.now());
            userRepository.save(user);

            // Log successful account linking
            auditService.logAccountLinking(user, "google", true);

            // Generate tokens and set cookies
            setAuthCookies(user, response);

            return ResponseEntity.ok(Map.of("message", "Account linked successfully"));
        } catch (Exception e) {
            log.error("Error confirming account linking", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Account linking failed"));
        }
    }

    @PostMapping("/unlink")
    public ResponseEntity<Map<String, String>> unlinkAccount(
            @RequestParam String provider,
            HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
            }

            String userEmail = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            Optional<UserOAuthProvider> oauthProviderOpt = userOAuthProviderRepository
                    .findByProviderAndProviderUserId(provider, user.getId().toString());

            if (oauthProviderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "OAuth provider not linked"));
            }

            userOAuthProviderRepository.delete(oauthProviderOpt.get());
            
            // Log successful account unlinking
            auditService.logAccountUnlinking(user, provider, true);
            
            return ResponseEntity.ok(Map.of("message", "Account unlinked successfully"));
        } catch (Exception e) {
            log.error("Error unlinking account", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Account unlinking failed"));
        }
    }

    @GetMapping("/sso-status")
    public ResponseEntity<Map<String, Object>> getSsoStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", true); // This should come from configuration
        return ResponseEntity.ok(status);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
            }

            String userEmail = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("name", user.getName());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole());
            userInfo.put("emailVerified", user.getEmailVerified());
            userInfo.put("avatarUrl", user.getAvatarUrl());
            userInfo.put("lastOauthLogin", user.getLastOauthLogin());

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Error getting current user", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get user info"));
        }
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setAuthCookies(User user, HttpServletResponse response) {
        // Generate JWT
        String jwt = jwtService.generateToken(user);
        
        // Generate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        
        // Set access token cookie
        Cookie accessCookie = new Cookie("MYBANK_ACCESS", jwt);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (jwtExpirationMs / 1000));
        response.addCookie(accessCookie);
        
        // Set refresh token cookie
        Cookie refreshCookie = new Cookie("MYBANK_REFRESH", refreshToken.getTokenHash());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("MYBANK_ACCESS", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("MYBANK_REFRESH", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }
}
