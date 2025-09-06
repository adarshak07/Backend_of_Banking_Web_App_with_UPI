package com.alien.bank.management.system.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
        
        log.error("OAuth2 authentication failed: {}", exception.getMessage(), exception);
        log.error("Request URL: {}", request.getRequestURL());
        log.error("Query String: {}", request.getQueryString());
        log.error("Exception type: {}", exception.getClass().getSimpleName());
        
        String errorType = "oauth_failed";
        
        // Handle specific OAuth2 errors
        if (exception.getMessage() != null) {
            if (exception.getMessage().contains("authorization_request_not_found")) {
                errorType = "oauth_session_expired";
                log.warn("OAuth authorization request not found - likely session expired or interrupted");
            } else if (exception.getMessage().contains("access_denied")) {
                errorType = "oauth_access_denied";
            } else if (exception.getMessage().contains("invalid_grant")) {
                errorType = "oauth_invalid_grant";
            }
        }
        
        // Redirect to frontend with specific error
        response.sendRedirect("http://localhost:3030/login?error=" + errorType);
    }
}
