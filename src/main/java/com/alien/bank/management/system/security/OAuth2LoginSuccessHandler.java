package com.alien.bank.management.system.security;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.repository.*;
import com.alien.bank.management.system.service.RefreshTokenService;
import com.alien.bank.management.system.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserOAuthProviderRepository userOAuthProviderRepository;
    private final OAuthLinkTokenRepository oauthLinkTokenRepository;
    private final AccountRepository accountRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Value("${feature.sso.enabled:false}")
    private boolean ssoEnabled;

    @Value("${mybank.jwt.expires-ms:900000}")
    private long jwtExpirationMs;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        log.info("OAuth2LoginSuccessHandler called - SSO enabled: {}", ssoEnabled);
        
        if (!ssoEnabled) {
            log.warn("SSO is disabled, redirecting to frontend with error");
            response.sendRedirect("http://localhost:3030/login?error=sso_disabled");
            return;
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String providerUserId = (String) attributes.get("sub");

        log.info("OAuth2 login attempt for email: {}", email);
        log.info("OAuth2 user attributes: {}", attributes);
        log.info("Provider user ID: {}", providerUserId);

        try {
            // Check if user already has OAuth provider linked
            Optional<UserOAuthProvider> existingProvider = userOAuthProviderRepository
                    .findByProviderAndProviderUserId("google", providerUserId);

            if (existingProvider.isPresent()) {
                // User already linked, log them in
                User user = existingProvider.get().getUser();
                handleExistingUserLogin(user, response);
                return;
            }

            // Check if email exists in system
            Optional<User> existingUser = userRepository.findByEmail(email);
            
            if (existingUser.isPresent()) {
                // Email exists, need to link accounts
                handleAccountLinking(existingUser.get(), email, name, picture, providerUserId, response);
            } else {
                // New user, create account
                handleNewUserSignup(email, name, picture, providerUserId, response);
            }

        } catch (Exception e) {
            log.error("Error during OAuth2 login", e);
            try {
                response.sendRedirect("http://localhost:3030/login?error=oauth_error");
            } catch (IOException ioException) {
                log.error("Failed to redirect after OAuth2 error", ioException);
            }
        }
    }

    private void handleExistingUserLogin(User user, HttpServletResponse response) throws IOException {
        log.info("🔄 OAuth2LoginSuccessHandler: Existing OAuth user login: {}", user.getEmail());
        log.info("🔄 OAuth2LoginSuccessHandler: User details - ID: {}, Name: {}, Email: {}, Role: {}", 
                user.getId(), user.getName(), user.getEmail(), user.getRole());
        
        // Update last OAuth login
        user.setLastOauthLogin(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        log.info("🔄 OAuth2LoginSuccessHandler: User saved with ID: {}", savedUser.getId());

        // Log successful OAuth login
        auditService.logOAuthLogin(savedUser, "google", true);

        // Generate JWT token
        String jwt = jwtService.generateToken(savedUser);
        log.info("🔄 OAuth2LoginSuccessHandler: JWT token generated: {}", jwt.substring(0, 50) + "...");
        
        // Redirect with JWT token as URL parameter
        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:3030/oauth-success")
                .queryParam("token", jwt)
                .queryParam("redirect", "home")
                .build()
                .toUriString();
        
        log.info("🔄 OAuth2LoginSuccessHandler: Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void handleAccountLinking(User existingUser, String email, String name, String picture, 
                                    String providerUserId, HttpServletResponse response) throws IOException {
        log.info("Account linking required for email: {}", email);
        
        try {
            // Create link token for password verification
            String linkToken = UUID.randomUUID().toString();
            OAuthLinkToken oauthLinkToken = OAuthLinkToken.builder()
                    .token(linkToken)
                    .user(existingUser)
                    .provider("google")
                    .providerUserId(providerUserId)
                    .providerEmail(email)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .used(false) // Explicitly set to false
                    .build();
            
            oauthLinkTokenRepository.save(oauthLinkToken);
            
            // Redirect to account linking page
            response.sendRedirect("http://localhost:3030/auth/link?token=" + linkToken + "&redirect=home");
        } catch (Exception e) {
            log.error("Error creating account linking token for email: {}", email, e);
            response.sendRedirect("http://localhost:3030/login?error=oauth_error");
        }
    }

    private void handleNewUserSignup(String email, String name, String picture, 
                                   String providerUserId, HttpServletResponse response) throws IOException {
        log.info("Creating new user via OAuth: {}", email);
        
        // Create new user
        User newUser = User.builder()
                .name(name)
                .email(email)
                .emailVerified(true)
                .avatarUrl(picture)
                .lastOauthLogin(LocalDateTime.now())
                .role(Role.USER)
                .build();
        
        newUser = userRepository.save(newUser);

        // Create OAuth provider link
        UserOAuthProvider oauthProvider = UserOAuthProvider.builder()
                .user(newUser)
                .provider("google")
                .providerUserId(providerUserId)
                .providerEmail(email)
                .providerAvatar(picture)
                .build();
        
        userOAuthProviderRepository.save(oauthProvider);

        // Create default account with zero balance
        String accountNumber = generateAccountNumber();
        Account defaultAccount = Account.builder()
                .user(newUser)
                .encryptedPan(accountNumber) // Using encryptedPan field for account number
                .last4Digits(accountNumber.substring(accountNumber.length() - 4))
                .balance(0.0)
                .build();
        
        accountRepository.save(defaultAccount);

        // Log successful OAuth login for new user
        auditService.logOAuthLogin(newUser, "google", true);

        // Generate JWT token
        String jwt = jwtService.generateToken(newUser);
        
        // Redirect with JWT token as URL parameter
        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:3030/oauth-success")
                .queryParam("token", jwt)
                .queryParam("onboarding", "true")
                .queryParam("redirect", "home")
                .build()
                .toUriString();
        
        response.sendRedirect(redirectUrl);
    }


    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis();
    }
}
